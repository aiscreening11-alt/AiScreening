package com.company.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponseDto {
    private UUID id;
    private String jobTitle;
    private String department;
    private String description;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private Double minimumExperience;
    private String educationRequirement;
    private String location;
    private String employmentType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
