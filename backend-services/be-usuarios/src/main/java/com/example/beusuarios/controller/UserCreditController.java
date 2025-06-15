package com.example.beusuarios.controller;

import com.example.beusuarios.dto.CreditOperationRequestDto;
import com.example.beusuarios.dto.CreditOperationRequestDto;
import com.example.beusuarios.dto.CreditOperationResponseDto;
import com.example.beusuarios.dto.UserCreditDto;
import com.example.beusuarios.exception.InsufficientCreditException;
import com.example.beusuarios.model.User;
import com.example.beusuarios.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // For atomicity
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users") // Base path for these user-specific internal endpoints
public class UserCreditController {

    private static final Logger logger = LoggerFactory.getLogger(UserCreditController.class);

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userId}/credit")
    @Transactional(readOnly = true) // Good practice for read operations
    public ResponseEntity<?> getUserCredit(@PathVariable Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            logger.warn("Attempt to get credit for non-existent user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        User user = optionalUser.get();
        // user.getEmail() will be decrypted here if accessed, but not needed for credit response
        logger.info("Retrieved credit for user ID: {}", userId);
        return ResponseEntity.ok(new UserCreditDto(userId, user.getCredit()));
    }

    @PostMapping("/{userId}/credit/decrement")
    @Transactional // Ensures atomicity for read-update
    public ResponseEntity<?> decrementUserCredit(@PathVariable Long userId,
                                                  @Valid @RequestBody CreditOperationRequestDto requestDto) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            logger.warn("Attempt to decrement credit for non-existent user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        User user = optionalUser.get();
        int amountToDecrement = requestDto.getAmount();

        if (user.getCredit() < amountToDecrement) {
            // throw new InsufficientCreditException("User " + userId + " has insufficient credit (" + user.getCredit() + ") for decrement amount " + amountToDecrement);
            // For a more user-friendly message for the API consumer:
            throw new InsufficientCreditException("Insufficient credit to perform this operation.");
        }

        user.setCredit(user.getCredit() - amountToDecrement);
        userRepository.save(user);

        logger.info("Decremented credit by {} for user ID: {}. New credit: {}", amountToDecrement, userId, user.getCredit());
        return ResponseEntity.ok(new CreditOperationResponseDto(userId, user.getCredit()));
    }

    @PostMapping("/{userId}/credit/increment")
    @Transactional // Ensures atomicity for read-update
    public ResponseEntity<?> incrementUserCredit(@PathVariable Long userId,
                                                  @Valid @RequestBody CreditOperationRequestDto requestDto) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            logger.warn("Attempt to increment credit for non-existent user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        User user = optionalUser.get();
        int amountToIncrement = requestDto.getAmount();

        // Consider if there's a max credit limit, though not specified
        user.setCredit(user.getCredit() + amountToIncrement);
        userRepository.save(user);

        logger.info("Incremented credit by {} for user ID: {}. New credit: {}", amountToIncrement, userId, user.getCredit());
        return ResponseEntity.ok(new CreditOperationResponseDto(userId, user.getCredit()));
    }
}
