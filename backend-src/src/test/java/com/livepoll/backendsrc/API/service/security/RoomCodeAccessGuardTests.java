package com.livepoll.backendsrc.API.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCodeAccessGuardTests {

    @Test
    void allowsOnlyValidSixCharacterRoomCodes() {
        RoomCodeAccessGuard guard = new RoomCodeAccessGuard();

        assertTrue(guard.isValidRoomCode("AB12CD"));
        assertTrue(guard.isValidRoomCode(" ab12cd "));
        assertFalse(guard.isValidRoomCode("AB12C"));
        assertFalse(guard.isValidRoomCode("AB12CD7"));
        assertFalse(guard.isValidRoomCode("AB-2CD"));
    }

    @Test
    void blocksRoomCodeLookupAfterThirtyAttemptsPerMinute() {
        RoomCodeAccessGuard guard = new RoomCodeAccessGuard(Clock.fixed(Instant.parse("2026-06-11T10:00:00Z"), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");

        for (int i = 0; i < 30; i++) {
            assertTrue(guard.allowLookup(request));
        }

        assertFalse(guard.allowLookup(request));
    }

    @Test
    void usesForwardedForAddressAsClientKey() {
        RoomCodeAccessGuard guard = new RoomCodeAccessGuard(Clock.fixed(Instant.parse("2026-06-11T10:00:00Z"), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.4, 203.0.113.7");

        for (int i = 0; i < 30; i++) {
            assertTrue(guard.allowLookup(request));
        }

        assertFalse(guard.allowLookup(request));
    }
}
