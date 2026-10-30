package com.livepoll.backendsrc.API.service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class RoomCodeAccessGuard {

    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");
    private static final int MAX_LOOKUPS_PER_WINDOW = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final Map<String, Deque<Long>> attemptsByClient = new ConcurrentHashMap<>();

    public RoomCodeAccessGuard() {
        this(Clock.systemUTC());
    }

    RoomCodeAccessGuard(Clock clock) {
        this.clock = clock;
    }

    public boolean isValidRoomCode(String roomCode) {
        return roomCode != null && ROOM_CODE_PATTERN.matcher(roomCode.trim().toUpperCase()).matches();
    }

    public boolean allowLookup(HttpServletRequest request) {
        long now = clock.millis();
        String clientKey = clientKey(request);
        Deque<Long> attempts = attemptsByClient.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());

        synchronized (attempts) {
            pruneOldAttempts(attempts, now);

            if (attempts.size() >= MAX_LOOKUPS_PER_WINDOW) {
                return false;
            }

            attempts.addLast(now);
            return true;
        }
    }

    private void pruneOldAttempts(Deque<Long> attempts, long now) {
        long oldestAllowed = now - WINDOW.toMillis();

        while (!attempts.isEmpty() && attempts.peekFirst() < oldestAllowed) {
            attempts.removeFirst();
        }
    }

    private String clientKey(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
