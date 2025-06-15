package com.example.becircuitos.dto;

import com.fasterxml.jackson.annotation.JsonInclude; // To exclude null circuitId

@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include circuitId if it's null (freemium)
public class GenerateCircuitResponseDto {
    private String qiskitCode;
    private int qubitCount;
    private boolean isSaved;
    private Long circuitId; // Only for premium saved circuits

    // Constructor for freemium
    public GenerateCircuitResponseDto(String qiskitCode, int qubitCount, boolean isSaved) {
        this.qiskitCode = qiskitCode;
        this.qubitCount = qubitCount;
        this.isSaved = isSaved;
    }

    // Constructor for premium
    public GenerateCircuitResponseDto(String qiskitCode, int qubitCount, boolean isSaved, Long circuitId) {
        this.qiskitCode = qiskitCode;
        this.qubitCount = qubitCount;
        this.isSaved = isSaved;
        this.circuitId = circuitId;
    }

    // Getters
    public String getQiskitCode() { return qiskitCode; }
    public int getQubitCount() { return qubitCount; }
    public boolean getIsSaved() { return isSaved; } // Getter for boolean
    public Long getCircuitId() { return circuitId; }
}
