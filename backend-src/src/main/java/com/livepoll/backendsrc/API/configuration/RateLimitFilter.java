package com.livepoll.backendsrc.API.configuration;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";
    private static final String REMAINING_HEADER = "X-Rate-Limit-Remaining";
    private static final String RETRY_AFTER_SECONDS_HEADER = "X-Rate-Limit-Retry-After-Seconds";
    private static final int TOO_MANY_REQUESTS_STATUS = 429;

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = getRequestPath(request);
        return !properties.isEnabled()
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || path.startsWith("/ws")
                || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitPolicy policy = resolvePolicy(request);
        String clientKey = resolveClientKey(request);
        String bucketKey = policy.name() + ":" + clientKey;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, ignored -> createBucket(policy.config()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = nanosToRetryAfterSeconds(probe.getNanosToWaitForRefill());
        response.setStatus(TOO_MANY_REQUESTS_STATUS);
        response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
        response.setHeader(RETRY_AFTER_SECONDS_HEADER, String.valueOf(retryAfterSeconds));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(
                "{\"successful\":false,\"message\":\"Too many requests. Try again later.\",\"data\":{\"errorCode\":\"%s\"}}"
                        .formatted(ERROR_CODE)
        );
    }

    private Bucket createBucket(RateLimitProperties.Policy policy) {
        validatePolicy(policy);
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(policy.getCapacity())
                        .refillGreedy(policy.getRefillTokens(), policy.getRefillDuration()))
                .build();
    }

    private void validatePolicy(RateLimitProperties.Policy policy) {
        if (policy.getCapacity() <= 0 || policy.getRefillTokens() <= 0) {
            throw new IllegalArgumentException("Rate-limit capacity and refill tokens must be greater than zero.");
        }
        Duration refillDuration = policy.getRefillDuration();
        if (refillDuration == null || refillDuration.isZero() || refillDuration.isNegative()) {
            throw new IllegalArgumentException("Rate-limit refill duration must be greater than zero.");
        }
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String path = getRequestPath(request);
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        if (HttpMethod.POST.matches(method)
                && ("/api/user/auth/login".equals(path) || "/api/user/auth/register".equals(path))) {
            return new RateLimitPolicy("auth", properties.getAuth());
        }

        if (HttpMethod.POST.matches(method) && path.matches("^/api/polls/[^/]+/vote$")) {
            return new RateLimitPolicy("vote", properties.getVote());
        }

        return new RateLimitPolicy("general", properties.getGeneral());
    }

    private String resolveClientKey(HttpServletRequest request) {
        return "ip:" + request.getRemoteAddr();
    }

    private String getRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private long nanosToRetryAfterSeconds(long nanos) {
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
        if (nanos > 0 && TimeUnit.SECONDS.toNanos(seconds) < nanos) {
            seconds++;
        }
        return Math.max(1, seconds);
    }

    private record RateLimitPolicy(String name, RateLimitProperties.Policy config) {
    }
}
