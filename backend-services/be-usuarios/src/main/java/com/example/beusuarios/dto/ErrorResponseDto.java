package com.example.beusuarios.dto; // Or com.example.beusuarios.dto.error

import java.time.LocalDateTime;
import java.util.List; // For field errors

public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error; // e.g., "Bad Request", "Not Found"
    private String message;
    private String path;
    private List<String> details; // For multiple validation errors

    // Constructor for general errors
    public ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Constructor for validation errors
    public ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message, String path, List<String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    // Getters (and potentially setters if needed by some frameworks, though usually not for DTOs)
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public List<String> getDetails() { return details; }
}
