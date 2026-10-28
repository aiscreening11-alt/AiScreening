package com.company.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_title", columnList = "job_title")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_title", nullable = false, length = 200)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", nullable = false)
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_preferred_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", nullable = false)
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @Column(name = "minimum_experience_years")
    @Builder.Default
    private Double minimumExperience = 0.0;

    @Column(name = "education_requirement", length = 200)
    private String educationRequirement;

    @Column(length = 150)
    private String location;

    @Column(name = "employment_type", length = 50)
    @Builder.Default
    private String employmentType = "FULL_TIME";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED, DRAFT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
