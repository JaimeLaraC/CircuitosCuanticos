package com.example.beusuarios.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    private JwtProvider jwtProvider;
    private final String testSecret = "testJwtSecretKeyForTestingPurposesMustBeLongEnough"; // Use a valid length secret
    private final String testExpiresIn = "1h"; // 1 hour

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(testSecret, testExpiresIn);
    }

    @Test
    void testGenerateTokenAndValidate() {
        Long userId = 1L;
        String email = "test@example.com";
        String subject = email;

        String token = jwtProvider.generateToken(subject, userId, email);
        assertNotNull(token);

        assertTrue(jwtProvider.validateToken(token));
        assertEquals(subject, jwtProvider.extractUsername(token));
        assertEquals(userId, jwtProvider.extractUserId(token));
    }

    @Test
    void testTokenExpiration() throws InterruptedException {
        // Use a very short expiration for testing
        JwtProvider shortExpiryJwtProvider = new JwtProvider(testSecret, "1s"); // 1 second
        Long userId = 2L;
        String email = "expire@example.com";

        String token = shortExpiryJwtProvider.generateToken(email, userId, email);
        assertNotNull(token);
        assertTrue(shortExpiryJwtProvider.validateToken(token)); // Valid immediately

        Thread.sleep(1100); // Wait for 1.1 seconds for token to expire

        assertFalse(shortExpiryJwtProvider.validateToken(token)); // Should be expired
    }

    @Test
    void testInvalidTokenSignature() {
        String token = jwtProvider.generateToken("subject", 1L, "test@example.com");
        // Tamper the token (e.g., modify a character in the signature part)
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtProvider.validateToken(tamperedToken));
    }
}
