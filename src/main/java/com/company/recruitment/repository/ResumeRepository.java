package com.company.recruitment.repository;

import com.company.recruitment.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    Optional<Resume> findByCandidateId(UUID candidateId);
    List<Resume> findByResumeHash(String resumeHash);
}
