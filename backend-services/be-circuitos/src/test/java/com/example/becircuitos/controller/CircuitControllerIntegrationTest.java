package com.example.becircuitos.controller;

import com.example.becircuitos.client.UserManagementClient;
import com.example.becircuitos.client.dto.DecrementCreditResponseDto;
import com.example.becircuitos.client.dto.UserCreditResponseDto;
import com.example.becircuitos.dto.GenerateCircuitRequestDto;
import com.example.becircuitos.model.Circuit;
import com.example.becircuitos.repository.CircuitRepository;
import com.example.becircuitos.security.jwt.JwtProvider; // Assuming JwtProvider is in this service for test token generation
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CircuitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitRepository circuitRepository;

    @MockBean
    private UserManagementClient userManagementClient;

    @Autowired // Use the actual JwtProvider from the application context for generating test tokens
    private JwtProvider jwtProvider;

    private String testJwtToken;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        circuitRepository.deleteAll();
        // Generate a test JWT. Subject is email, also include userId claim.
        testJwtToken = jwtProvider.generateToken("testuser@example.com", testUserId, "testuser@example.com");
    }

    @Test
    void testGenerateCircuitFreemiumSuccess() throws Exception {
        GenerateCircuitRequestDto request = new GenerateCircuitRequestDto();
        Map<String, Object> truthTable = new HashMap<>();
        truthTable.put("numInputs", 2); // Freemium (<= 6 qubits)
        request.setTruthTable(truthTable);

        mockMvc.perform(post("/circuits/generate")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qubitCount", is(2)))
                .andExpect(jsonPath("$.isSaved", is(false)))
                .andExpect(jsonPath("$.qiskitCode", notNullValue()));
    }

    @Test
    void testGenerateCircuitPremiumSuccess() throws Exception {
        GenerateCircuitRequestDto request = new GenerateCircuitRequestDto();
        Map<String, Object> truthTable = new HashMap<>();
        truthTable.put("numInputs", 7); // Premium (> 6 qubits)
        request.setTruthTable(truthTable);

        UserCreditResponseDto creditDto = new UserCreditResponseDto();
        creditDto.setCredit(5);
        when(userManagementClient.getUserCredit(eq(testUserId), eq(testJwtToken)))
            .thenReturn(Optional.of(creditDto));

        DecrementCreditResponseDto decrementDto = new DecrementCreditResponseDto();
        decrementDto.setNewCredit(4);
        when(userManagementClient.decrementUserCredit(eq(testUserId), eq(1), eq(testJwtToken)))
            .thenReturn(Optional.of(decrementDto));

        mockMvc.perform(post("/circuits/generate")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qubitCount", is(7)))
                .andExpect(jsonPath("$.isSaved", is(true)))
                .andExpect(jsonPath("$.circuitId", notNullValue()));

        assertEquals(1, circuitRepository.findByUserId(testUserId).size());
    }

    @Test
    void testGenerateCircuitPremiumInsufficientCredit() throws Exception {
        GenerateCircuitRequestDto request = new GenerateCircuitRequestDto();
        Map<String, Object> truthTable = new HashMap<>();
        truthTable.put("numInputs", 8);
        request.setTruthTable(truthTable);

        UserCreditResponseDto creditDto = new UserCreditResponseDto();
        creditDto.setCredit(0); // No credit
        when(userManagementClient.getUserCredit(eq(testUserId), eq(testJwtToken)))
            .thenReturn(Optional.of(creditDto));

        mockMvc.perform(post("/circuits/generate")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().string("Crédito insuficiente. Por favor, compra más crédito para generar circuitos de más de 6 cúbits."));
    }

    @Test
    void testGetSavedCircuitsSuccess() throws Exception {
        // Save a circuit for the user directly
        Circuit c1 = new Circuit();
        c1.setUserId(testUserId);
        c1.setQiskitCode("code1");
        c1.setQubitCount(7);
        c1.setTruthTableData("{}");
        circuitRepository.save(c1);

        mockMvc.perform(get("/circuits")
                .header("Authorization", "Bearer " + testJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].circuitId", is(c1.getId().intValue())))
                .andExpect(jsonPath("$[0].qubitCount", is(7)));
    }

    @Test
    void testGetCircuitDetailSuccess() throws Exception {
        Circuit c1 = new Circuit();
        c1.setUserId(testUserId);
        c1.setQiskitCode("detailed_code");
        c1.setQubitCount(8);
        c1.setTruthTableData("{\"key\":\"value\"}");
        Circuit savedCircuit = circuitRepository.save(c1);

        mockMvc.perform(get("/circuits/" + savedCircuit.getId())
                .header("Authorization", "Bearer " + testJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitId", is(savedCircuit.getId().intValue())))
                .andExpect(jsonPath("$.qiskitCode", is("detailed_code")))
                .andExpect(jsonPath("$.truthTableData", is("{\"key\":\"value\"}")));
    }

    @Test
    void testGetCircuitDetailNotFoundForOtherUser() throws Exception {
         Circuit c1 = new Circuit();
         c1.setUserId(testUserId + 1); // Different user ID
         c1.setQiskitCode("other_user_code");
         c1.setQubitCount(8);
         c1.setTruthTableData("{}");
         Circuit savedCircuit = circuitRepository.save(c1);

        mockMvc.perform(get("/circuits/" + savedCircuit.getId())
                .header("Authorization", "Bearer " + testJwtToken)) // testJwtToken is for testUserId
                .andExpect(status().isNotFound());
    }
}
