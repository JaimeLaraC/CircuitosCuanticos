package com.example.beusuarios.controller;

import com.example.beusuarios.dto.LoginRequest;
import com.example.beusuarios.dto.LoginResponse;
import com.example.beusuarios.dto.PasswordResetRequestDetailsDto;
import com.example.beusuarios.dto.RegisterRequest;
import com.example.beusuarios.dto.ResetPasswordRequestDto;
import com.example.beusuarios.model.PasswordResetToken;
import com.example.beusuarios.model.User;
import com.example.beusuarios.repository.PasswordResetTokenRepository;
import com.example.beusuarios.repository.UserRepository;
import com.example.beusuarios.security.jwt.JwtProvider;
import com.example.beusuarios.service.EmailService;
import com.example.beusuarios.service.PasswordService;
// import com.example.beusuarios.service.EncryptionService; // Not directly used in controller for login logic

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import java.util.List; // Required for iterating users
import java.net.URI;
// import java.security.SecureRandom; // For token generation, UUID is also good
// import java.util.Base64; // If using raw random bytes for token

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtProvider jwtProvider;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    // ... registerUser and confirmEmail methods ...
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPasswordHash(passwordService.hashPassword(registerRequest.getPassword()));
        newUser.setConfirmationToken(UUID.randomUUID().toString());
        newUser.setIsConfirmed(false);

        try {
            userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
             return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Email is already in use or another data integrity issue.");
        }
        emailService.sendConfirmationEmail(newUser.getEmail(), newUser.getConfirmationToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body("Registro exitoso. Por favor, revisa tu email para confirmar tu cuenta.");
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Void> confirmEmail(@RequestParam("token") String token) {
        Optional<User> optionalUser = userRepository.findByConfirmationToken(token);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!user.getIsConfirmed()) {
                user.setIsConfirmed(true);
                user.setConfirmationToken(null);
                userRepository.save(user);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendBaseUrl + "/auth/account-confirmed"))
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendBaseUrl + "/auth/login?message=already_confirmed"))
                        .build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBaseUrl + "/auth/invalid-token"))
                    .build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        // WARNING: This is inefficient for large number of users.
        // Iterates through all users and decrypts email for comparison.
        List<User> allUsers = userRepository.findAll();
        Optional<User> foundUser = Optional.empty();

        for (User user : allUsers) {
            // user.getEmail() will be decrypted by the AttributeConverter
            if (user.getEmail().equals(loginRequest.getEmail())) {
                foundUser = Optional.of(user);
                break;
            }
        }

        if (foundUser.isEmpty()) {
            logger.warn("Login attempt failed for email {}: User not found", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials or user not found.");
        }

        User user = foundUser.get();

        if (!passwordService.checkPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            logger.warn("Login attempt failed for user {}: Incorrect password", user.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials.");
        }

        if (!user.getIsConfirmed()) {
            logger.warn("Login attempt failed for user {}: Account not confirmed", user.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Account not confirmed. Please check your email.");
        }

        String token = jwtProvider.generateToken(user.getEmail(), user.getId(), user.getEmail());
        // Note: user.getEmail() here is already decrypted due to AttributeConverter.

        LoginResponse loginResponse = new LoginResponse(
            token,
            user.getId(),
            user.getEmail(), // This is the decrypted email
            user.getCredit()
        );

        logger.info("User {} logged in successfully", user.getId());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDetailsDto requestDetailsDto) {
        // WARNING: Inefficient email lookup, same as login.
        List<User> allUsers = userRepository.findAll();
        Optional<User> foundUser = Optional.empty();
        for (User user : allUsers) {
            if (user.getEmail().equals(requestDetailsDto.getEmail())) {
                foundUser = Optional.of(user);
                break;
            }
        }

        if (foundUser.isPresent()) {
            User user = foundUser.get();

            String plainToken = UUID.randomUUID().toString(); // Generate a secure random token
            // String hashedToken = passwordService.hashPassword(plainToken); // bcrypt for token (overkill)
            String hashedToken = passwordService.hashGenericToken(plainToken); // SHA-256 for token

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hashedToken);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15)); // 15 minutes expiration

            passwordResetTokenRepository.save(resetToken);

            // Send email with the PLAIN token
            emailService.sendPasswordResetEmail(user.getEmail(), plainToken);
            logger.info("Password reset requested for user {}", user.getId());
        } else {
            // User not found, but don't reveal this for security.
            logger.info("Password reset requested for non-existent email: {}", requestDetailsDto.getEmail());
        }

        // Always return a generic success message
        return ResponseEntity.ok().body("Si el email existe, se ha enviado un enlace para resetear la contraseña.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDto resetRequestDto) {
        String plainToken = resetRequestDto.getToken();
        String newPassword = resetRequestDto.getNewPassword();

        String hashedToken = passwordService.hashGenericToken(plainToken); // Hash the received plain token

        Optional<PasswordResetToken> optionalResetToken =
            passwordResetTokenRepository.findByTokenHash(hashedToken);

        if (optionalResetToken.isEmpty()) {
            logger.warn("Password reset attempt with invalid token hash for plain token starting with: {}", plainToken.substring(0, Math.min(plainToken.length(), 8)));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired password reset token.");
        }

        PasswordResetToken resetToken = optionalResetToken.get();

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken); // Clean up expired token
            logger.warn("Password reset attempt with expired token for user {}", resetToken.getUser().getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password reset token has expired.");
        }

        User user = resetToken.getUser();
        if (user == null) {
             // Should not happen if DB integrity is maintained, but good to check.
             passwordResetTokenRepository.delete(resetToken); // Clean up invalid token
             logger.error("PasswordResetToken {} found but has no associated user.", resetToken.getId());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing password reset: User not found for token.");
        }

        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken); // Invalidate the token by deleting it

        logger.info("Password successfully reset for user {}", user.getId());
        return ResponseEntity.ok().body("Contraseña actualizada correctamente.");
    }
}
