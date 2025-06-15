package com.example.becircuitos.dto;

import java.time.LocalDateTime;
// Assuming truthTableData will be returned as a String (JSON)
// If it needs to be a Map<String, Object>, add ObjectMapper for deserialization

public class CircuitDetailDto {
    private Long circuitId;
    private String qiskitCode;
    private String truthTableData; // JSON string
    private int qubitCount;
    private LocalDateTime createdAt;

    public CircuitDetailDto(Long circuitId, String qiskitCode, String truthTableData, int qubitCount, LocalDateTime createdAt) {
        this.circuitId = circuitId;
        this.qiskitCode = qiskitCode;
        this.truthTableData = truthTableData;
        this.qubitCount = qubitCount;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getCircuitId() { return circuitId; }
    public String getQiskitCode() { return qiskitCode; }
    public String getTruthTableData() { return truthTableData; }
    public int getQubitCount() { return qubitCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
