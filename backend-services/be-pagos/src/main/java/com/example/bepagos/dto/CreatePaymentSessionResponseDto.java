package com.example.bepagos.dto;

public class CreatePaymentSessionResponseDto {
    private String message;
    private String redirectUrl;

    public CreatePaymentSessionResponseDto(String message, String redirectUrl) {
        this.message = message;
        this.redirectUrl = redirectUrl;
    }

    // Getters
    public String getMessage() { return message; }
    public String getRedirectUrl() { return redirectUrl; }
}
