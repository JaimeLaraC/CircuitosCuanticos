package com.example.becircuitos.client;

import com.example.becircuitos.client.dto.DecrementCreditRequestDto;
import com.example.becircuitos.client.dto.DecrementCreditResponseDto;
import com.example.becircuitos.client.dto.UserCreditResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class UserManagementClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserManagementClient userManagementClient;

    private final String usersApiBaseUrl = "http://localhost:3001"; // Test URL
    private final String testJwt = "test.jwt.token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Re-initialize userManagementClient if @InjectMocks doesn't pick up constructor arg from field
        userManagementClient = new UserManagementClient(restTemplate, usersApiBaseUrl);
    }

    @Test
    void testGetUserCreditSuccess() {
        UserCreditResponseDto mockResponseDto = new UserCreditResponseDto();
        mockResponseDto.setCredit(10);
        ResponseEntity<UserCreditResponseDto> mockResponseEntity = new ResponseEntity<>(mockResponseDto, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(usersApiBaseUrl + "/users/1/credit"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UserCreditResponseDto.class))
        ).thenReturn(mockResponseEntity);

        Optional<UserCreditResponseDto> result = userManagementClient.getUserCredit(1L, testJwt);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().getCredit());
    }

    @Test
    void testGetUserCreditNotFound() {
         when(restTemplate.exchange(
            anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(UserCreditResponseDto.class))
        ).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Optional<UserCreditResponseDto> result = userManagementClient.getUserCredit(1L, testJwt);
        assertFalse(result.isPresent());
    }


    @Test
    void testDecrementUserCreditSuccess() {
        DecrementCreditResponseDto mockResponseDto = new DecrementCreditResponseDto();
        mockResponseDto.setNewCredit(9);
        ResponseEntity<DecrementCreditResponseDto> mockResponseEntity = new ResponseEntity<>(mockResponseDto, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(usersApiBaseUrl + "/users/1/credit/decrement"),
            eq(HttpMethod.POST),
            any(HttpEntity.class), // Could be more specific with ArgumentCaptor
            eq(DecrementCreditResponseDto.class))
        ).thenReturn(mockResponseEntity);

        Optional<DecrementCreditResponseDto> result = userManagementClient.decrementUserCredit(1L, 1, testJwt);

        assertTrue(result.isPresent());
        assertEquals(9, result.get().getNewCredit());
    }

    @Test
    void testDecrementUserCreditInsufficientCredit() {
        when(restTemplate.exchange(
            anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(DecrementCreditResponseDto.class))
        ).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Insufficient credit")); // Simulate 400 from BE Usuarios

        Optional<DecrementCreditResponseDto> result = userManagementClient.decrementUserCredit(1L, 1, testJwt);
        assertFalse(result.isPresent());
    }
}
