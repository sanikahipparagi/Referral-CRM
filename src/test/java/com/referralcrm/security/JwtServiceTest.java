package com.referralcrm.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("test-secret-that-is-at-least-32-bytes-long", 60);

    @Test void issuedTokenRoundTripsUserId() {
        UUID userId = UUID.randomUUID();
        assertEquals(userId, jwt.userId(jwt.issue(userId, "person@example.com")));
    }

    @Test void rejectsWeakSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60));
    }
}
