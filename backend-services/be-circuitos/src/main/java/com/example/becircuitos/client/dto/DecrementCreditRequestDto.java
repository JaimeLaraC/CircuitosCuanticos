package com.example.becircuitos.client.dto;

// Corresponds to CreditOperationRequestDto in BE Usuarios
public class DecrementCreditRequestDto {
    private Integer amount;

    // Constructors, Getters & Setters
    public DecrementCreditRequestDto() {}
    public DecrementCreditRequestDto(Integer amount) { this.amount = amount; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}
