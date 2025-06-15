package com.example.becircuitos.client;

import com.example.becircuitos.client.dto.DecrementCreditRequestDto;
import com.example.becircuitos.client.dto.DecrementCreditResponseDto;
import com.example.becircuitos.client.dto.UserCreditResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;

@Component
public class UserManagementClient {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementClient.class);
    private final RestTemplate restTemplate;
    private final String usersApiBaseUrl;

    public UserManagementClient(RestTemplate restTemplate,
                                @Value("${services.users.url}") String usersApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.usersApiBaseUrl = usersApiBaseUrl;
    }

    public Optional<UserCreditResponseDto> getUserCredit(Long userId, String jwtToken) {
        String url = usersApiBaseUrl + "/users/" + userId + "/credit";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Calling BE Usuarios to get credit for user {}: URL: {}", userId, url);
            ResponseEntity<UserCreditResponseDto> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, UserCreditResponseDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully retrieved credit for user {}: {}", userId, response.getBody().getCredit());
                return Optional.of(response.getBody());
            }
            logger.warn("Received non-OK response or empty body from BE Usuarios for getUserCredit. Status: {}", response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            logger.error("Client error while getting credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty(); // e.g., 404 User not found, 401 Unauthorized from BE Usuarios
        } catch (HttpServerErrorException e) {
            logger.error("Server error while getting credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty(); // e.g., BE Usuarios is down
        } catch (Exception e) {
            logger.error("Unexpected error while getting credit for user {}: {}", userId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<DecrementCreditResponseDto> decrementUserCredit(Long userId, int amount, String jwtToken) {
        String url = usersApiBaseUrl + "/users/" + userId + "/credit/decrement";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("Content-Type", "application/json");

        DecrementCreditRequestDto requestBody = new DecrementCreditRequestDto(amount);
        HttpEntity<DecrementCreditRequestDto> entity = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("Calling BE Usuarios to decrement credit for user {}: URL: {}, Amount: {}", userId, url, amount);
            ResponseEntity<DecrementCreditResponseDto> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, DecrementCreditResponseDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully decremented credit for user {}. New credit: {}", userId, response.getBody().getNewCredit());
                return Optional.of(response.getBody());
            }
            logger.warn("Received non-OK response or empty body from BE Usuarios for decrementUserCredit. Status: {}", response.getStatusCode());
            return Optional.empty(); // Should ideally not happen if OK status
        } catch (HttpClientErrorException e) {
            // Specific handling for 400 Bad Request (e.g. insufficient credit from BE Usuarios)
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                 logger.warn("BE Usuarios indicated bad request (e.g., insufficient credit) for user {}: {}", userId, e.getResponseBodyAsString());
            } else {
                logger.error("Client error while decrementing credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            }
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            logger.error("Server error while decrementing credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error while decrementing credit for user {}: {}", userId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
