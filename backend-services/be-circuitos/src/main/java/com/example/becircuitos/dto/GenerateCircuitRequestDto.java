package com.example.becircuitos.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class GenerateCircuitRequestDto {
    @NotNull(message = "Truth table cannot be null")
    private Map<String, Object> truthTable;

    // Getter and Setter
    public Map<String, Object> getTruthTable() { return truthTable; }
    public void setTruthTable(Map<String, Object> truthTable) { this.truthTable = truthTable; }
}
