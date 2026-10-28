package com.company.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interviewers", indexes = {
        @Index(name = "idx_interviewers_email", columnList = "email"),
        @Index(name = "idx_interviewers_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "designation")
    private String designation;

    @Column(name = "department", length = 100)
    @Builder.Default
    private String department = "Engineering";

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "role", length = 50)
    @Builder.Default
    private String role = "INTERVIEWER";

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return active != null && active;
    }
}
