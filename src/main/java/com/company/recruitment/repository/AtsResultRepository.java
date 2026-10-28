package com.company.recruitment.repository;

import com.company.recruitment.entity.AtsResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AtsResultRepository extends JpaRepository<AtsResult, UUID> {
    Optional<AtsResult> findByCandidateId(UUID candidateId);
}
