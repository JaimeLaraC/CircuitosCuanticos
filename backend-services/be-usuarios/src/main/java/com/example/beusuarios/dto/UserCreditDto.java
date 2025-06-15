package com.example.beusuarios.dto;

public class UserCreditDto {
    private Long userId;
    private Integer credit;

    public UserCreditDto(Long userId, Integer credit) {
        this.userId = userId;
        this.credit = credit;
    }

    // Getters
    public Long getUserId() { return userId; }
    public Integer getCredit() { return credit; }
}
