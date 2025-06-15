package com.example.becircuitos.controller;

import com.example.becircuitos.client.UserManagementClient;
import com.example.becircuitos.client.dto.DecrementCreditResponseDto;
import com.example.becircuitos.client.dto.UserCreditResponseDto;
import com.example.becircuitos.dto.GenerateCircuitRequestDto;
import com.example.becircuitos.dto.GenerateCircuitResponseDto;
import com.example.becircuitos.dto.QiskitGenerationResultDto;
import com.example.becircuitos.model.Circuit;
import com.example.becircuitos.repository.CircuitRepository;
import com.example.becircuitos.security.jwt.JwtProvider; // To extract from Authentication principal
import com.example.becircuitos.service.QiskitService;
import com.example.becircuitos.dto.CircuitDetailDto;
import com.example.becircuitos.dto.CircuitSummaryDto;

import io.jsonwebtoken.Claims; // If parsing JWT directly
import jakarta.servlet.http.HttpServletRequest; // To get the token from header if not using Principal
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Spring Security User
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/circuits")
public class CircuitController {

    private static final Logger logger = LoggerFactory.getLogger(CircuitController.class);

    @Autowired
    private QiskitService qiskitService;

    @Autowired
    private CircuitRepository circuitRepository;

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private JwtProvider jwtProvider; // To extract claims if needed, or get raw token

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    @PostMapping("/generate")
    public ResponseEntity<?> generateCircuit(
            @Valid @RequestBody GenerateCircuitRequestDto requestDto,
            Authentication authentication, HttpServletRequest httpRequest) { // Inject Authentication and HttpServletRequest

        if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }

        String usernameFromPrincipal = authentication.getName(); // This is email (subject of JWT)
        // To get userId, we need to parse the token or have it in a custom principal
        // For now, let's assume JwtAuthFilter sets UserDetails as principal.
        // We need the raw token to pass to UserManagementClient.
        String jwtToken = extractJwtFromRequest(httpRequest);
        if (jwtToken == null) {
            logger.error("JWT token is missing from request for user {}", usernameFromPrincipal);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing JWT token.");
        }

        Long userId = jwtProvider.extractUserId(jwtToken); // Extract userId from the token
        if (userId == null) {
             logger.error("Could not extract userId from JWT for user {}", usernameFromPrincipal);
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token: userId missing.");
        }


        logger.info("Generating circuit for user ID: {}", userId);
        QiskitGenerationResultDto qiskitResult =
            qiskitService.generateQiskitFromTruthTable(requestDto.getTruthTable());

        if (qiskitResult.getQubitCount() <= 0 && qiskitResult.getQiskitCode().startsWith("ERROR:")) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(qiskitResult.getQiskitCode());
        }

        // Freemium Flow
        if (qiskitResult.getQubitCount() <= 6) {
            logger.info("Freemium flow for user ID: {}. Qubit count: {}", userId, qiskitResult.getQubitCount());
            return ResponseEntity.ok(new GenerateCircuitResponseDto(
                qiskitResult.getQiskitCode(),
                qiskitResult.getQubitCount(),
                false // isSaved
            ));
        }

        // Premium Flow
        logger.info("Premium flow for user ID: {}. Qubit count: {}", userId, qiskitResult.getQubitCount());
        Optional<UserCreditResponseDto> creditResponseOpt = userManagementClient.getUserCredit(userId, jwtToken);

        if (creditResponseOpt.isEmpty()) {
            logger.error("Failed to retrieve credit for user ID: {} from UserManagementClient.", userId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // Or BAD_GATEWAY
                                 .body("Error communicating with user service to check credit.");
        }

        UserCreditResponseDto creditInfo = creditResponseOpt.get();
        if (creditInfo.getCredit() == null || creditInfo.getCredit() <= 0) {
            logger.warn("User ID: {} has insufficient credit ({}).", userId, creditInfo.getCredit());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                                 .body("Crédito insuficiente. Por favor, compra más crédito para generar circuitos de más de 6 cúbits.");
        }

        // Sufficient credit, try to decrement
        Optional<DecrementCreditResponseDto> decrementResponseOpt =
            userManagementClient.decrementUserCredit(userId, 1, jwtToken); // Assuming 1 credit cost

        if (decrementResponseOpt.isEmpty()) {
            // This could mean BE Usuarios had an issue, or it specifically denied (e.g. concurrent update led to insufficient credit)
            logger.error("Failed to decrement credit for user ID: {} via UserManagementClient. Check client logs.", userId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // Or specific error if client provides one
                                 .body("Error communicating with user service to decrement credit or credit was insufficient.");
        }

        // Decrement was successful according to BE Usuarios
        logger.info("Credit decremented for user ID: {}. New credit: {}", userId, decrementResponseOpt.get().getNewCredit());

        Circuit circuit = new Circuit();
        circuit.setUserId(userId);
        circuit.setQiskitCode(qiskitResult.getQiskitCode());
        circuit.setQubitCount(qiskitResult.getQubitCount());
        circuit.setTruthTableData(qiskitResult.getTruthTableJson());
        // createdAt is set by @PrePersist

        Circuit savedCircuit = circuitRepository.save(circuit);
        logger.info("Premium circuit saved for user ID: {} with circuit ID: {}", userId, savedCircuit.getId());

        return ResponseEntity.ok(new GenerateCircuitResponseDto(
            savedCircuit.getQiskitCode(),
            savedCircuit.getQubitCount(),
            true, // isSaved
            savedCircuit.getId()
        ));
    }

    @GetMapping
    public ResponseEntity<List<CircuitSummaryDto>> getSavedCircuits(
            Authentication authentication, HttpServletRequest httpRequest) {

        if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Or throw exception
        }

        String jwtToken = extractJwtFromRequest(httpRequest); // Helper from /generate
        if (jwtToken == null) {
            logger.error("JWT token is missing from request for user {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long userId = jwtProvider.extractUserId(jwtToken);
        if (userId == null) {
             logger.error("Could not extract userId from JWT for user {}", authentication.getName());
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        logger.info("Fetching saved circuits for user ID: {}", userId);
        List<Circuit> userCircuits = circuitRepository.findByUserId(userId);

        List<CircuitSummaryDto> circuitSummaries = userCircuits.stream()
            .map(circuit -> new CircuitSummaryDto(
                circuit.getId(),
                circuit.getQubitCount(),
                circuit.getCreatedAt()
            ))
            .collect(Collectors.toList());

        logger.info("Found {} saved circuits for user ID: {}", circuitSummaries.size(), userId);
        return ResponseEntity.ok(circuitSummaries);
    }

    // Helper method extractJwtFromRequest (if not already present or visible)
    // private String extractJwtFromRequest(HttpServletRequest request) {
    //     String bearerToken = request.getHeader("Authorization");
    //     if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
    //         return bearerToken.substring(7);
    //     }
    //     return null;
    // }

    @GetMapping("/{circuitId}")
    public ResponseEntity<?> getCircuitDetails(
            @PathVariable Long circuitId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

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

        logger.info("Fetching details for circuit ID: {} for user ID: {}", circuitId, userId);

        // Use findByIdAndUserId to ensure ownership
        Optional<Circuit> optionalCircuit = circuitRepository.findByIdAndUserId(circuitId, userId);

        if (optionalCircuit.isEmpty()) {
            logger.warn("Circuit ID: {} not found for user ID: {} or user does not own it.", circuitId, userId);
            // Return 404 Not Found whether it doesn't exist or doesn't belong to user,
            // to avoid leaking information about existence of circuits for other users.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Circuit not found.");
        }

        Circuit circuit = optionalCircuit.get();
        CircuitDetailDto circuitDetailDto = new CircuitDetailDto(
            circuit.getId(),
            circuit.getQiskitCode(),
            circuit.getTruthTableData(), // truthTableData is already a JSON string
            circuit.getQubitCount(),
            circuit.getCreatedAt()
        );

        logger.info("Successfully retrieved details for circuit ID: {} for user ID: {}", circuitId, userId);
        return ResponseEntity.ok(circuitDetailDto);
    }

    // Helper method extractJwtFromRequest (ensure it's accessible)
    // private String extractJwtFromRequest(HttpServletRequest request) {
    //     String bearerToken = request.getHeader("Authorization");
    //     if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
    //         return bearerToken.substring(7);
    //     }
    //     return null;
    // }
}
