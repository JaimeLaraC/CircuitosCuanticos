package com.example.beusuarios.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(); // Uses BCrypt with 12 rounds
    }

    @Test
    void testHashPasswordIsNotNull() {
        String hashedPassword = passwordService.hashPassword("testPassword123");
        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$12$")); // BCrypt prefix for 12 rounds
    }

    @Test
    void testCheckPasswordCorrect() {
        String plainPassword = "testPassword123";
        String hashedPassword = passwordService.hashPassword(plainPassword);
        assertTrue(passwordService.checkPassword(plainPassword, hashedPassword));
    }

    @Test
    void testCheckPasswordIncorrect() {
        String plainPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String hashedPassword = passwordService.hashPassword(plainPassword);
        assertFalse(passwordService.checkPassword(wrongPassword, hashedPassword));
    }

    @Test
    void testCheckPasswordWithNulls() {
        assertFalse(passwordService.checkPassword(null, "someHash"));
        assertFalse(passwordService.checkPassword("somePassword", null));
        assertFalse(passwordService.checkPassword(null, null));
    }
}
