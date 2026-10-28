package com.company.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_messages", indexes = {
        @Index(name = "idx_gmail_msg_id", columnList = "gmail_message_id", unique = true),
        @Index(name = "idx_email_msg_processed", columnList = "processed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gmail_message_id", nullable = false, unique = true, length = 100)
    private String gmailMessageId;

    @Column(name = "sender_email", length = 255)
    private String senderEmail;

    @Column(length = 500)
    private String subject;

    @Column(name = "body_snippet", columnDefinition = "TEXT")
    private String bodySnippet;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processing_status", length = 50)
    @Builder.Default
    private String processingStatus = "RECEIVED"; // RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
