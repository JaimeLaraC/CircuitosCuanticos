package com.example.beusuarios.dto;

public class LoginResponse {
    private String accessToken;
    private Long userId;
    private String email; // Decrypted email
    private Integer credit;

    public LoginResponse(String accessToken, Long userId, String email, Integer credit) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
        this.credit = credit;
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public Integer getCredit() { return credit; }
    // No Setters needed for response DTO usually
}
