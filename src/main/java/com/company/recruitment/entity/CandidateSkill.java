package com.company.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_skills", indexes = {
        @Index(name = "idx_candidate_skills_candidate", columnList = "candidate_id"),
        @Index(name = "idx_candidate_skills_normalized", columnList = "normalized_skill")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "normalized_skill", nullable = false, length = 100)
    private String normalizedSkill;

    @Column(length = 50)
    private String category; // PROGRAMMING_LANGUAGE, FRAMEWORK, DATABASE, CLOUD, TOOL, OTHER

    @Column(name = "is_matched_requirement")
    @Builder.Default
    private Boolean isMatchedRequirement = false;
}
