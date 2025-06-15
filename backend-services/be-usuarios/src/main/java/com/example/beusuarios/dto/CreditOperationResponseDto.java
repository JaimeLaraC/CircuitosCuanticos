package com.example.beusuarios.dto;

public class CreditOperationResponseDto {
    private Long userId;
    private Integer newCredit;

    public CreditOperationResponseDto(Long userId, Integer newCredit) {
        this.userId = userId;
        this.newCredit = newCredit;
    }

    // Getters
    public Long getUserId() { return userId; }
    public Integer getNewCredit() { return newCredit; }
}
