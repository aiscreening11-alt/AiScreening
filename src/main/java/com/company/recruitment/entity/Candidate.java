package com.company.recruitment.entity;

import com.company.recruitment.entity.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidates", indexes = {
        @Index(name = "idx_candidate_email", columnList = "resume_email"),
        @Index(name = "idx_candidate_sender_email", columnList = "sender_email"),
        @Index(name = "idx_candidate_status", columnList = "status"),
        @Index(name = "idx_candidate_job_id", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "sender_email", length = 255)
    private String senderEmail;

    @Column(name = "resume_email", length = 255)
    private String resumeEmail;

    @Column(name = "email_discrepancy_flag")
    @Builder.Default
    private Boolean emailDiscrepancyFlag = false;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "total_experience_years")
    @Builder.Default
    private Double totalExperienceYears = 0.0;

    @Column(name = "current_company", length = 200)
    private String currentCompany;

    @Column(columnDefinition = "TEXT")
    private String previousCompanies;

    @Column(length = 200)
    private String education;

    @Column(length = 150)
    private String degree;

    @Column(length = 150)
    private String location;

    @Column(name = "notice_period", length = 100)
    private String noticePeriod;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private com.company.recruitment.entity.enums.Gender gender = com.company.recruitment.entity.enums.Gender.UNSPECIFIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CandidateStatus status = CandidateStatus.RECEIVED;

    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Resume resume;

    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AtsResult atsResult;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CandidateSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmailCommunication> emailCommunications = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Interview> interviews = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addSkill(CandidateSkill skill) {
        skills.add(skill);
        skill.setCandidate(this);
    }

    public void addEmailCommunication(EmailCommunication emailCommunication) {
        if (emailCommunications == null) {
            emailCommunications = new ArrayList<>();
        }
        emailCommunications.add(emailCommunication);
        emailCommunication.setCandidate(this);
    }

    public void addInterview(Interview interview) {
        if (interviews == null) {
            interviews = new ArrayList<>();
        }
        interviews.add(interview);
        interview.setCandidate(this);
    }
}
