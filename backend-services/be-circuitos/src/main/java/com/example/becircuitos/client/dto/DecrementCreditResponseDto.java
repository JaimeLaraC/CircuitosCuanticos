package com.example.becircuitos.client.dto;

// Corresponds to CreditOperationResponseDto in BE Usuarios
public class DecrementCreditResponseDto {
    private Long userId; // May not be needed
    private Integer newCredit;

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getNewCredit() { return newCredit; }
    public void setNewCredit(Integer newCredit) { this.newCredit = newCredit; }
}
