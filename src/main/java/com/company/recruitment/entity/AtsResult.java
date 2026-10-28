package com.company.recruitment.entity;

import com.company.recruitment.entity.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ats_results", indexes = {
        @Index(name = "idx_ats_candidate_id", columnList = "candidate_id"),
        @Index(name = "idx_ats_final_score", columnList = "final_score"),
        @Index(name = "idx_ats_decision", columnList = "decision")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Column(name = "rule_score", nullable = false)
    private Double ruleScore;

    @Column(name = "ai_score")
    private Double aiScore;

    @Column(name = "final_score", nullable = false)
    private Double finalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CandidateStatus decision;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ats_matched_skills", joinColumns = @JoinColumn(name = "ats_result_id"))
    @Column(name = "skill", nullable = false)
    @Builder.Default
    private List<String> matchedSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ats_missing_skills", joinColumns = @JoinColumn(name = "ats_result_id"))
    @Column(name = "skill", nullable = false)
    @Builder.Default
    private List<String> missingSkills = new ArrayList<>();

    @Column(name = "scoring_breakdown_json", columnDefinition = "TEXT")
    private String scoringBreakdownJson;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "is_ai_assisted")
    @Builder.Default
    private Boolean isAiAssisted = false;

    @CreationTimestamp
    @Column(name = "evaluated_at", updatable = false)
    private LocalDateTime evaluatedAt;
}
