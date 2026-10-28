package com.company.recruitment.repository;

import com.company.recruitment.entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {
    List<CandidateSkill> findByCandidateId(UUID candidateId);
    void deleteByCandidateId(UUID candidateId);
}
