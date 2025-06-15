package com.example.bepagos.controller;

import com.example.bepagos.client.UserManagementClient;
import com.example.bepagos.client.dto.IncrementCreditResponseDto; // For the response from client
import com.example.bepagos.dto.CreatePaymentSessionRequestDto;
import com.example.bepagos.dto.CreatePaymentSessionResponseDto;
import com.example.bepagos.security.jwt.JwtProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private JwtProvider jwtProvider; // To extract userId from token

    @Value("${app.frontend.payment-success-url:https://frontend.app/payment-success}")
    private String paymentSuccessRedirectUrl;

    @Value("${app.frontend.payment-cancel-url:https://frontend.app/payment-cancel}") // Not used in this sim, but good to have
    private String paymentCancelRedirectUrl;


    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @PostMapping("/create-session")
    public ResponseEntity<?> createPaymentSession(
            @Valid @RequestBody CreatePaymentSessionRequestDto requestDto,
            Authentication authentication, HttpServletRequest httpRequest) {

        if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }

        String jwtToken = extractJwtFromRequest(httpRequest);
        if (jwtToken == null) {
            logger.error("JWT token is missing from request for user {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing JWT token.");
        }

        Long userId = jwtProvider.extractUserId(jwtToken);
        if (userId == null) {
             logger.error("Could not extract userId from JWT for user {}", authentication.getName());
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token: userId missing.");
        }

        // Simulate Product Validation & Credit Calculation
        int creditAmountToAdd;
        switch (requestDto.getProductId()) {
            case "credit_pack_10":
                creditAmountToAdd = 10 * requestDto.getQuantity();
                break;
            case "credit_pack_50":
                creditAmountToAdd = 50 * requestDto.getQuantity();
                break;
            // Add more product IDs as needed
            default:
                logger.warn("Invalid productId received: {} for user {}", requestDto.getProductId(), userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid product ID.");
        }

        if (creditAmountToAdd <= 0) {
             logger.warn("Calculated credit amount is zero or negative for productId: {}, quantity: {}, user {}",
                requestDto.getProductId(), requestDto.getQuantity(), userId);
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid quantity or product configuration resulting in no credit.");
        }


        logger.info("Attempting to add {} credits for user ID: {} via product: {}",
                    creditAmountToAdd, userId, requestDto.getProductId());

        Optional<IncrementCreditResponseDto> incrementResponseOpt =
            userManagementClient.incrementUserCredit(userId, creditAmountToAdd, jwtToken);

        if (incrementResponseOpt.isEmpty()) {
            logger.error("Failed to increment credit for user ID: {} via UserManagementClient. Amount: {}", userId, creditAmountToAdd);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // Or a more specific error if known
                                 .body("Error processing payment with user service.");
        }

        // Payment simulation is successful
        IncrementCreditResponseDto incrementResult = incrementResponseOpt.get();
        logger.info("Successfully processed simulated payment for user ID: {}. Credits added: {}. New total credit (from BE Usuarios): {}",
                    userId, creditAmountToAdd, incrementResult.getNewCredit());

        CreatePaymentSessionResponseDto response = new CreatePaymentSessionResponseDto(
            "Pago procesado exitosamente. Tu nuevo saldo ha sido actualizado.",
            paymentSuccessRedirectUrl // Use the configured success URL
        );
        return ResponseEntity.ok(response);
    }
}
