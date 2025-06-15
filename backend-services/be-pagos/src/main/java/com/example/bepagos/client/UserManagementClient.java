package com.example.bepagos.client;

import com.example.bepagos.client.dto.IncrementCreditRequestDto;
import com.example.bepagos.client.dto.IncrementCreditResponseDto;
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

    public Optional<IncrementCreditResponseDto> incrementUserCredit(Long userId, int amount, String jwtToken) {
        String url = usersApiBaseUrl + "/users/" + userId + "/credit/increment";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("Content-Type", "application/json");

        IncrementCreditRequestDto requestBody = new IncrementCreditRequestDto(amount);
        HttpEntity<IncrementCreditRequestDto> entity = new HttpEntity<>(requestBody, headers);

        try {
            logger.debug("Calling BE Usuarios to increment credit for user {}: URL: {}, Amount: {}", userId, url, amount);
            ResponseEntity<IncrementCreditResponseDto> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, IncrementCreditResponseDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully incremented credit for user {}. New credit: {}", userId, response.getBody().getNewCredit());
                return Optional.of(response.getBody());
            }
            logger.warn("Received non-OK response or empty body from BE Usuarios for incrementUserCredit. Status: {}", response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            logger.error("Client error while incrementing credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            logger.error("Server error while incrementing credit for user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error while incrementing credit for user {}: {}", userId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
