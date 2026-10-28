package com.company.recruitment.entity;

import com.company.recruitment.entity.enums.EmailStatus;
import com.company.recruitment.entity.enums.EmailType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_communications", indexes = {
        @Index(name = "idx_email_comm_candidate", columnList = "candidate_id"),
        @Index(name = "idx_email_comm_status", columnList = "sent_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    private EmailType emailType;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "body_content", columnDefinition = "TEXT", nullable = false)
    private String bodyContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_status", nullable = false, length = 50)
    @Builder.Default
    private EmailStatus sentStatus = EmailStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
