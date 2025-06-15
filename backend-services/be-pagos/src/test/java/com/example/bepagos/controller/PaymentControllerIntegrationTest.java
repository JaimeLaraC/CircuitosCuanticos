package com.example.bepagos.controller;

import com.example.bepagos.client.UserManagementClient;
import com.example.bepagos.client.dto.IncrementCreditResponseDto; // For mocking client response
import com.example.bepagos.dto.CreatePaymentSessionRequestDto;
import com.example.bepagos.security.jwt.JwtProvider; // Assuming JwtProvider is in this service for test token generation
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional; // Though BE Pagos has no DB, good habit if other components do

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Optional: if you have application-test.properties
// @Transactional // Not strictly needed as BE Pagos doesn't write to its own DB
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean // Mock the client that calls BE Usuarios
    private UserManagementClient userManagementClient;

    @Autowired // Use the actual JwtProvider from the application context for generating test tokens
    private JwtProvider jwtProvider;

    @Value("${app.frontend.payment-success-url:https://frontend.app/payment-success}")
    private String expectedSuccessRedirectUrl;

    private String testJwtToken;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // Generate a test JWT. Subject is email, also include userId claim.
        testJwtToken = jwtProvider.generateToken("testuser@example.com", testUserId, "testuser@example.com");
    }

    @Test
    void testCreatePaymentSessionSuccess_CreditPack10() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("credit_pack_10");
        request.setQuantity(1);

        IncrementCreditResponseDto mockClientResponse = new IncrementCreditResponseDto();
        mockClientResponse.setNewCredit(110); // Example new credit
        when(userManagementClient.incrementUserCredit(eq(testUserId), eq(10), eq(testJwtToken)))
            .thenReturn(Optional.of(mockClientResponse));

        mockMvc.perform(post("/payments/create-session")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Pago procesado exitosamente. Tu nuevo saldo ha sido actualizado.")))
                .andExpect(jsonPath("$.redirectUrl", is(expectedSuccessRedirectUrl)));
    }

    @Test
    void testCreatePaymentSessionSuccess_CreditPack50_Quantity2() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("credit_pack_50");
        request.setQuantity(2); // 50 * 2 = 100 credits

        IncrementCreditResponseDto mockClientResponse = new IncrementCreditResponseDto();
        mockClientResponse.setNewCredit(200); // Example new credit
        when(userManagementClient.incrementUserCredit(eq(testUserId), eq(100), eq(testJwtToken)))
            .thenReturn(Optional.of(mockClientResponse));

        mockMvc.perform(post("/payments/create-session")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Pago procesado exitosamente. Tu nuevo saldo ha sido actualizado.")))
                .andExpect(jsonPath("$.redirectUrl", is(expectedSuccessRedirectUrl)));
    }

    @Test
    void testCreatePaymentSessionInvalidProductId() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("invalid_pack");
        request.setQuantity(1);

        mockMvc.perform(post("/payments/create-session")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid product ID."))); // From PaymentController logic
    }

    @Test
    void testCreatePaymentSessionUserManagementClientFailure() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("credit_pack_10");
        request.setQuantity(1);

        when(userManagementClient.incrementUserCredit(eq(testUserId), eq(10), eq(testJwtToken)))
            .thenReturn(Optional.empty()); // Simulate failure from client

        mockMvc.perform(post("/payments/create-session")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable()) // As per PaymentController logic
                .andExpect(jsonPath("$.message", is("Error processing payment with user service.")));
    }

    @Test
    void testCreatePaymentSessionInvalidQuantity_DTOValidation() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("credit_pack_10");
        request.setQuantity(0); // Invalid quantity

        // This test relies on DTO validation and GlobalExceptionHandler
        mockMvc.perform(post("/payments/create-session")
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.details[0]", is("quantity: Quantity must be at least 1")));
    }

    @Test
    void testCreatePaymentSession_NoAuth() throws Exception {
        CreatePaymentSessionRequestDto request = new CreatePaymentSessionRequestDto();
        request.setProductId("credit_pack_10");
        request.setQuantity(1);

        mockMvc.perform(post("/payments/create-session") // No Authorization header
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // From Spring Security config
    }
}
