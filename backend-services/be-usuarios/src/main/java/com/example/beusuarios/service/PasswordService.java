package com.example.beusuarios.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat; // Java 17+ for HexFormat

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService() {
        // Salt rounds set to 12 as per design document
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    public String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        return passwordEncoder.encode(plainPassword);
    }

    public boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    public String hashGenericToken(String plainToken) {
        if (plainToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes); // Convert byte array to hex string
        } catch (NoSuchAlgorithmException e) {
            // This should ideally not happen with SHA-256
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
