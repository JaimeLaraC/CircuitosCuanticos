package com.example.beusuarios.model;

import jakarta.persistence.*;
import com.example.beusuarios.security.crypto.EmailEncryptor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email") // Placeholder, actual uniqueness will be on encrypted value
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true, length = 512) // Increased length for encrypted data + IV + Base64
    @Convert(converter = EmailEncryptor.class)
    private String email;

    @NotNull
    @Size(min = 60, max = 60) // bcrypt hashes are typically 60 characters
    @Column(nullable = false, length = 60)
    private String passwordHash;

    @NotNull
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer credit = 0;

    @NotNull
    @Column(nullable = false, columnDefinition = "BIT DEFAULT 0")
    private Boolean isConfirmed = false;

    @Column(length = 255, nullable = true)
    private String confirmationToken;

    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Integer getCredit() { return credit; }
    public void setCredit(Integer credit) { this.credit = credit; }
    public Boolean getIsConfirmed() { return isConfirmed; }
    public void setIsConfirmed(Boolean isConfirmed) { this.isConfirmed = isConfirmed; }
    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
