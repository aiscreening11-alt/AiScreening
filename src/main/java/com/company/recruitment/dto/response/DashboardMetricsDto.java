package com.company.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private long totalApplications;
    private long shortlisted;
    private long reviewRequired;
    private long rejected;
    private long interviewScheduled;
    private long selected;
    private long notSelected;
    private long processingFailed;
    private Map<String, Long> statusBreakdown;

    // Diversity & Gender Demographics
    private long maleCount;
    private long femaleCount;
    private long otherCount;
    private long unspecifiedCount;
    private Map<String, Long> genderBreakdown;
}
