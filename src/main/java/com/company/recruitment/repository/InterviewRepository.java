package com.company.recruitment.repository;

import com.company.recruitment.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    List<Interview> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    Optional<Interview> findByAccessToken(String accessToken);
    List<Interview> findByInterviewerEmailIgnoreCaseOrderByInterviewDateDesc(String interviewerEmail);
    List<Interview> findTop15ByEvaluatedAtIsNotNullOrderByEvaluatedAtDesc();
}
