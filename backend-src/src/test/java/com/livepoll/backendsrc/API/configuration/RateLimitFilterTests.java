package com.livepoll.backendsrc.API.configuration;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTests {

    @Test
    void allowsRequestsWithinGeneralLimit() throws ServletException, IOException {
        RateLimitFilter filter = createFilter(2, 1, 1);

        MockHttpServletResponse firstResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.1");
        MockHttpServletResponse secondResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.1");

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getHeader("X-Rate-Limit-Remaining")).isEqualTo("0");
    }

    @Test
    void rejectsRequestsAboveGeneralLimit() throws ServletException, IOException {
        RateLimitFilter filter = createFilter(1, 1, 1);

        MockHttpServletResponse firstResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.1");
        MockHttpServletResponse secondResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.1");

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("3600");
        assertThat(secondResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void usesStricterAuthPolicyForLoginAndRegister() throws ServletException, IOException {
        RateLimitFilter filter = createFilter(10, 1, 10);

        MockHttpServletResponse firstResponse = perform(filter, "POST", "/api/user/auth/login", "127.0.0.1");
        MockHttpServletResponse secondResponse = perform(filter, "POST", "/api/user/auth/login", "127.0.0.1");

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void bypassesOptionsRequests() throws ServletException, IOException {
        RateLimitFilter filter = createFilter(1, 1, 1);

        MockHttpServletResponse firstResponse = perform(filter, "OPTIONS", "/api/user/auth/login", "127.0.0.1");
        MockHttpServletResponse secondResponse = perform(filter, "OPTIONS", "/api/user/auth/login", "127.0.0.1");

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void keepsSeparateBucketsPerClientIp() throws ServletException, IOException {
        RateLimitFilter filter = createFilter(1, 1, 1);

        MockHttpServletResponse firstClientResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.1");
        MockHttpServletResponse secondClientResponse = perform(filter, "GET", "/api/polls/get-polls", "127.0.0.2");

        assertThat(firstClientResponse.getStatus()).isEqualTo(200);
        assertThat(secondClientResponse.getStatus()).isEqualTo(200);
    }

    private RateLimitFilter createFilter(long generalCapacity, long authCapacity, long voteCapacity) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setGeneral(new RateLimitProperties.Policy(generalCapacity, generalCapacity, Duration.ofHours(1)));
        properties.setAuth(new RateLimitProperties.Policy(authCapacity, authCapacity, Duration.ofHours(1)));
        properties.setVote(new RateLimitProperties.Policy(voteCapacity, voteCapacity, Duration.ofHours(1)));
        return new RateLimitFilter(properties);
    }

    private MockHttpServletResponse perform(
            RateLimitFilter filter,
            String method,
            String path,
            String remoteAddress
    ) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
