package com.company.recruitment.repository;

import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.enums.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByJobId(UUID jobId);

    Page<Candidate> findByJobId(UUID jobId, Pageable pageable);

    Page<Candidate> findByStatus(CandidateStatus status, Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE (:jobId IS NULL OR c.job.id = :jobId) AND (:status IS NULL OR c.status = :status)")
    Page<Candidate> findFiltered(@Param("jobId") UUID jobId, @Param("status") CandidateStatus status, Pageable pageable);

    Optional<Candidate> findByJobIdAndResumeEmailIgnoreCase(UUID jobId, String resumeEmail);

    Optional<Candidate> findByJobIdAndSenderEmailIgnoreCase(UUID jobId, String senderEmail);

    @Query("SELECT c.status, COUNT(c) FROM Candidate c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT c.gender, COUNT(c) FROM Candidate c GROUP BY c.gender")
    List<Object[]> countByGenderGrouped();
}
