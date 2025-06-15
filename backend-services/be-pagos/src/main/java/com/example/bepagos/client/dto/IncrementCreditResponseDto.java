package com.example.bepagos.client.dto;

// Mirrors CreditOperationResponseDto from BE Usuarios
public class IncrementCreditResponseDto {
    private Long userId;
    private Integer newCredit;
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getNewCredit() { return newCredit; }
    public void setNewCredit(Integer newCredit) { this.newCredit = newCredit; }
}
