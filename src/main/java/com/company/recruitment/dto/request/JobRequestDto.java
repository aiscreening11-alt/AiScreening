package com.company.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestDto {

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private String department;

    private String description;

    @NotEmpty(message = "At least one required skill must be specified")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @PositiveOrZero(message = "Minimum experience must be non-negative")
    @Builder.Default
    private Double minimumExperience = 0.0;

    private String educationRequirement;

    private String location;

    @Builder.Default
    private String employmentType = "FULL_TIME";

    @Builder.Default
    private String status = "ACTIVE";
}
