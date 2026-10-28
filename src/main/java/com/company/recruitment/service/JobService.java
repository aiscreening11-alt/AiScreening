package com.company.recruitment.service;

import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.entity.Job;

import java.util.List;
import java.util.UUID;

public interface JobService {
    JobResponseDto createJob(JobRequestDto request);
    JobResponseDto getJobById(UUID id);
    Job getJobEntityById(UUID id);
    List<JobResponseDto> getAllJobs(String status);
    JobResponseDto updateJob(UUID id, JobRequestDto request);
    void deleteJob(UUID id);
    Job findActiveJobByTitleOrKeyword(String text);
}
