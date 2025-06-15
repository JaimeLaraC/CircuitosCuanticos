package com.example.becircuitos.service;

import com.example.becircuitos.dto.QiskitGenerationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class QiskitServiceTest {
    private QiskitService qiskitService;

    @BeforeEach
    void setUp() {
        qiskitService = new QiskitService();
    }

    @Test
    void testGenerateWithNumInputsProvided() {
        Map<String, Object> truthTable = new HashMap<>();
        truthTable.put("numInputs", 4); // Explicitly provide numInputs
        truthTable.put("data", "some_truth_table_data");

        QiskitGenerationResultDto result = qiskitService.generateQiskitFromTruthTable(truthTable);

        assertEquals(4, result.getQubitCount());
        assertTrue(result.getQiskitCode().contains("QuantumCircuit(4, 4)"));
        assertNotNull(result.getTruthTableJson());
        assertTrue(result.getTruthTableJson().contains("\"numInputs\":4"));
    }

    @Test
    void testGenerateWithFallbackQubitCount() {
        Map<String, Object> truthTable = new HashMap<>();
        truthTable.put("input1", "0"); // No numInputs, fallback based on map size
        truthTable.put("input2", "1");
        truthTable.put("output", "1");


        QiskitGenerationResultDto result = qiskitService.generateQiskitFromTruthTable(truthTable);
        // Fallback logic: Math.max(1, truthTable.size()) which is 3
        assertEquals(3, result.getQubitCount());
        assertTrue(result.getQiskitCode().contains("QuantumCircuit(3, 3)"));
    }

    @Test
    void testGenerateWithEmptyTruthTable() {
        QiskitGenerationResultDto result = qiskitService.generateQiskitFromTruthTable(Collections.emptyMap());
        assertEquals(0, result.getQubitCount()); // As per current logic, empty table -> 0 qubits
        assertTrue(result.getQiskitCode().startsWith("ERROR:"));
    }

    @Test
    void testGenerateWithNullTruthTable() {
        QiskitGenerationResultDto result = qiskitService.generateQiskitFromTruthTable(null);
        assertEquals(0, result.getQubitCount());
        assertTrue(result.getQiskitCode().startsWith("ERROR:"));
    }
}
