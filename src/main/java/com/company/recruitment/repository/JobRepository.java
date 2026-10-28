package com.company.recruitment.repository;

import com.company.recruitment.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatus(String status);
    Optional<Job> findByJobTitleIgnoreCaseAndStatus(String jobTitle, String status);
}
