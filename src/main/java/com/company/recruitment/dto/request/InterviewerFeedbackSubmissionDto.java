package com.company.recruitment.dto.request;

import com.company.recruitment.entity.enums.InterviewResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerFeedbackSubmissionDto {

    @NotNull(message = "Technical competency rating is required (1-5)")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer technicalRating;

    @NotNull(message = "Communication skills rating is required (1-5)")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer communicationRating;

    @NotNull(message = "Confidence level rating is required (1-5)")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer confidenceRating;

    @NotNull(message = "Body language & professional demeanor rating is required (1-5)")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer bodyLanguageRating;

    @NotNull(message = "Recommendation verdict (SELECTED or NOT_SELECTED) is required")
    private InterviewResult result;

    private String comments;

    private String interviewerName;
}
