package com.example.becircuitos.client.dto;

// Corresponds to UserCreditDto in BE Usuarios
public class UserCreditResponseDto {
    private Long userId; // May not be needed if already known by client
    private Integer credit;

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getCredit() { return credit; }
    public void setCredit(Integer credit) { this.credit = credit; }
}
