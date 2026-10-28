package com.company.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resume_candidate_id", columnList = "candidate_id"),
        @Index(name = "idx_resume_hash", columnList = "resume_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "resume_hash", length = 64)
    private String resumeHash;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
