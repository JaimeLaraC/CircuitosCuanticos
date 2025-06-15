package com.example.beusuarios.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits

    private final SecretKey secretKey;

    public EncryptionService(@Value("${encryption.key}") String keyString) {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes long for AES-256.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext for use in decryption
            byte[] encryptedPayload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedPayload, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedPayload, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedPayload);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedPayloadBase64) {
        if (encryptedPayloadBase64 == null) {
            return null;
        }
        try {
            byte[] encryptedPayload = Base64.getDecoder().decode(encryptedPayloadBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedPayload, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[encryptedPayload.length - iv.length];
            System.arraycopy(encryptedPayload, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // It's often better not to throw a generic RuntimeException in decrypt
            // as it can be an indicator of tampered data or wrong key.
            // Consider a custom, more specific exception.
            System.err.println("Error decrypting data: " + e.getMessage());
            // Depending on policy, you might return null, or throw a specific crypto exception
            return null; // Or throw new DecryptionErrorException("Failed to decrypt data", e);
        }
    }
}
