package com.example.beusuarios.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {
    private EncryptionService encryptionService;
    private final String testKey = "testDefaultKeyForEncryption1234"; // Must be 32 bytes

    @BeforeEach
    void setUp() {
        // Ensure the key is 32 bytes for AES-256
        encryptionService = new EncryptionService(testKey);
    }

    @Test
    void testEncryptDecryptSuccess() {
        String originalText = "test@example.com";
        String encryptedText = encryptionService.encrypt(originalText);
        assertNotNull(encryptedText);
        assertNotEquals(originalText, encryptedText);

        String decryptedText = encryptionService.decrypt(encryptedText);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testEncryptNull() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void testDecryptNull() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void testDecryptTamperedData() {
        String originalText = "test@example.com";
        String encryptedText = encryptionService.encrypt(originalText);
        String tamperedEncryptedText = encryptedText.substring(0, encryptedText.length() - 1) + "X"; // Tamper last char

        // Decryption of tampered GCM data should ideally fail or return null/garbage
        // The current EncryptionService returns null on error.
        assertNull(encryptionService.decrypt(tamperedEncryptedText));
    }

    @Test
    void testConstructorWithInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> new EncryptionService("shortkey"));
    }
}
