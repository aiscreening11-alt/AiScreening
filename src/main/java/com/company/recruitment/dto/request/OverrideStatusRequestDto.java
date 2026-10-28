package com.company.recruitment.dto.request;

import com.company.recruitment.entity.enums.CandidateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideStatusRequestDto {

    @NotNull(message = "New status is required")
    private CandidateStatus newStatus;

    @NotBlank(message = "Reason for override is required")
    private String reason;

    private String performedBy;
}
