package com.example.beusuarios.controller;

import com.example.beusuarios.dto.LoginRequest;
import com.example.beusuarios.dto.RegisterRequest;
import com.example.beusuarios.model.User;
import com.example.beusuarios.repository.UserRepository;
import com.example.beusuarios.service.EmailService;
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


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Optional: use a test application.properties if needed
@Transactional // Rollback transactions after each test
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // Mock EmailService to prevent actual email sending during tests
    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean slate for users
        // Mock behavior for emailService
        doNothing().when(emailService).sendConfirmationEmail(anyString(), anyString());
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setPassword("Password123!");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registro exitoso. Por favor, revisa tu email para confirmar tu cuenta."));
    }

    @Test
    void testRegisterUserEmailAlreadyExists() throws Exception {
        // First registration
        RegisterRequest registerRequest1 = new RegisterRequest();
        registerRequest1.setEmail("testuser@example.com");
        registerRequest1.setPassword("Password123!");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest1)));
        // Attempt to register again with the same email
        RegisterRequest registerRequest2 = new RegisterRequest();
        registerRequest2.setEmail("testuser@example.com"); // Same email
        registerRequest2.setPassword("OtherPassword456!");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isConflict()) // Expect 409 Conflict
                .andExpect(jsonPath("$.message", is("Database error: A unique constraint was violated (e.g., email already exists).")));
    }

    @Test
    void testLoginUserSuccessAfterConfirmation() throws Exception {
        // 1. Register User
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("loginuser@example.com");
        registerRequest.setPassword("Password123!");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        User user = userRepository.findAll().stream()
            .filter(u -> u.getEmail().equals("loginuser@example.com")) // Email will be decrypted by converter
            .findFirst().orElseThrow();

        // 2. Confirm User (simulating clicking the link)
        assertNotNull(user.getConfirmationToken());
        mockMvc.perform(get("/auth/confirm-email").param("token", user.getConfirmationToken()))
               .andExpect(status().isFound()); // 302 Redirect

        // 3. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("loginuser@example.com");
        loginRequest.setPassword("Password123!");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.email", is("loginuser@example.com")))
                .andExpect(jsonPath("$.userId", is(user.getId().intValue())));
    }

    @Test
    void testLoginUserNotConfirmed() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("unconfirmed@example.com");
        registerRequest.setPassword("Password123!");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unconfirmed@example.com");
        loginRequest.setPassword("Password123!");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Error: Account not confirmed. Please check your email.")));
    }

    @Test
    void testLoginUserInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nosuchuser@example.com");
        loginRequest.setPassword("Password123!");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
