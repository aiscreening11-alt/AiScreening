package com.company.recruitment.dto.request;

import com.company.recruitment.entity.enums.InterviewRound;
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
public class ScheduleInterviewRequestDto {

    @NotNull(message = "Interview round is required (L1_TECHNICAL, L2_TECHNICAL, MANAGERIAL, HR_ROUND)")
    private InterviewRound round;

    private String interviewerName;

    private String interviewerEmail;

    @NotNull(message = "Interview date and time is required")
    private LocalDateTime interviewDate;

    @Builder.Default
    private Integer durationMinutes = 45;

    private String meetingLink;

    private String notes;
}
