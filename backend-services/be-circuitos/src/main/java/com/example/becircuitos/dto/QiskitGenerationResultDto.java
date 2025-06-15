package com.example.becircuitos.dto;

public class QiskitGenerationResultDto {
    private String qiskitCode;
    private int qubitCount;
    private String truthTableJson; // To store the input truth table that generated this

    public QiskitGenerationResultDto(String qiskitCode, int qubitCount, String truthTableJson) {
        this.qiskitCode = qiskitCode;
        this.qubitCount = qubitCount;
        this.truthTableJson = truthTableJson;
    }

    // Getters
    public String getQiskitCode() { return qiskitCode; }
    public int getQubitCount() { return qubitCount; }
    public String getTruthTableJson() { return truthTableJson; }
    // No setters needed for a result DTO
}
