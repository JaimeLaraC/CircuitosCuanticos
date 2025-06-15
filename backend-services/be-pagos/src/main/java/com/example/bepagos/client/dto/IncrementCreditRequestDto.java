package com.example.bepagos.client.dto;

// Mirrors CreditOperationRequestDto from BE Usuarios
public class IncrementCreditRequestDto {
    private Integer amount;
    public IncrementCreditRequestDto() {}
    public IncrementCreditRequestDto(Integer amount) { this.amount = amount; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}
