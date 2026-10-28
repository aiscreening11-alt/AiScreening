package com.company.recruitment.entity;

import com.company.recruitment.entity.enums.InterviewResult;
import com.company.recruitment.entity.enums.InterviewRound;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interviews", indexes = {
        @Index(name = "idx_interview_candidate", columnList = "candidate_id"),
        @Index(name = "idx_interview_result", columnList = "result"),
        @Index(name = "idx_interview_round", columnList = "round")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private InterviewRound round = InterviewRound.L1_TECHNICAL;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(name = "interviewer_name", length = 150)
    private String interviewerName;

    @Column(name = "interviewer_email", length = 150)
    private String interviewerEmail;

    @Column(name = "meeting_link", length = 255)
    private String meetingLink;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 45;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "technical_rating")
    private Integer technicalRating;

    @Column(name = "communication_rating")
    private Integer communicationRating;

    @Column(name = "confidence_rating")
    private Integer confidenceRating;

    @Column(name = "body_language_rating")
    private Integer bodyLanguageRating;

    @Column(name = "overall_rating")
    private Double overallRating;

    @Column(name = "recommendation", length = 50)
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "access_token", length = 100, unique = true)
    private String accessToken;

    @PrePersist
    public void prePersist() {
        if (this.accessToken == null || this.accessToken.isBlank()) {
            this.accessToken = UUID.randomUUID().toString().replace("-", "");
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private InterviewResult result = InterviewResult.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
