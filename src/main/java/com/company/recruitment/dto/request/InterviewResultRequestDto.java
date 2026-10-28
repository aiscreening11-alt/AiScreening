package com.company.recruitment.dto.request;

import com.company.recruitment.entity.enums.InterviewResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResultRequestDto {

    @NotNull(message = "Interview result (SELECTED or NOT_SELECTED) is required")
    private InterviewResult result;

    private String interviewerName;

    private String feedback;

    private Integer technicalRating;

    private Integer communicationRating;

    private Integer confidenceRating;

    private Integer bodyLanguageRating;

    private String recommendation;

    private String comments;

    private LocalDateTime interviewDate;
}
