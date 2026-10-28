package com.company.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerResponseDto {

    private UUID id;
    private String fullName;
    private String email;
    private String designation;
    private String department;
    private String phoneNumber;
    private String role;
    private Boolean active;
    private long assignedInterviewsCount;
    private long completedEvaluationsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
