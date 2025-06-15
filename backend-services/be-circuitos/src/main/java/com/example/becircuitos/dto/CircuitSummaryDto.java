package com.example.becircuitos.dto;

import java.time.LocalDateTime;

public class CircuitSummaryDto {
    private Long circuitId;
    private int qubitCount;
    private LocalDateTime createdAt; // Or String if specific formatting is needed client-side

    public CircuitSummaryDto(Long circuitId, int qubitCount, LocalDateTime createdAt) {
        this.circuitId = circuitId;
        this.qubitCount = qubitCount;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getCircuitId() { return circuitId; }
    public int getQubitCount() { return qubitCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
