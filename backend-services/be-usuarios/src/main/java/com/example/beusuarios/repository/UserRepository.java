package com.example.beusuarios.repository;

import com.example.beusuarios.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Querying by encrypted email will be tricky.
    // This will likely require fetching all users and decrypting in memory,
    // or using a DB function if possible, or a separate non-encrypted lookup column.
    // For now, a simple findByEmail is a placeholder.
    // A more robust solution will be addressed with the AttributeConverter.
    // Optional<User> findByEmail(String email); // Cannot be reliably used with randomized encryption
    Optional<User> findByConfirmationToken(String confirmationToken);
}
