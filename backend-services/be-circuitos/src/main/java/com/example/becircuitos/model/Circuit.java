package com.example.becircuitos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "Circuits", indexes = {
    @Index(name = "idx_circuit_userid", columnList = "userId")
})
public class Circuit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId; // References User ID from BE Usuarios

    @NotNull
    @Lob // For potentially large Qiskit code string, maps to TEXT or similar
    @Column(nullable = false, columnDefinition = "TEXT")
    private String qiskitCode;

    @NotNull
    @Column(nullable = false)
    private Integer qubitCount;

    @NotNull
    @Lob // For potentially large JSON string for truth table data
    @Column(nullable = false, columnDefinition = "TEXT") // MySQL typically uses TEXT for JSON like data if not using native JSON type
    private String truthTableData; // Store as JSON string

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQiskitCode() { return qiskitCode; }
    public void setQiskitCode(String qiskitCode) { this.qiskitCode = qiskitCode; }
    public Integer getQubitCount() { return qubitCount; }
    public void setQubitCount(Integer qubitCount) { this.qubitCount = qubitCount; }
    public String getTruthTableData() { return truthTableData; }
    public void setTruthTableData(String truthTableData) { this.truthTableData = truthTableData; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
