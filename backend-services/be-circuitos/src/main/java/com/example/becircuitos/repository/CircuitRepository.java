package com.example.becircuitos.repository;

import com.example.becircuitos.model.Circuit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CircuitRepository extends JpaRepository<Circuit, Long> {
    List<Circuit> findByUserId(Long userId);
    Optional<Circuit> findByIdAndUserId(Long id, Long userId); // For security check
}
