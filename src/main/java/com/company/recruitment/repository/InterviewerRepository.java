package com.company.recruitment.repository;

import com.company.recruitment.entity.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewerRepository extends JpaRepository<Interviewer, UUID> {

    Optional<Interviewer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Interviewer> findByActiveTrueOrderByFullNameAsc();

    List<Interviewer> findAllByOrderByCreatedAtDesc();
}
