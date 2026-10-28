package com.company.recruitment.service;

import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.entity.Job;
import com.company.recruitment.exception.JobNotFoundException;
import com.company.recruitment.mapper.JobMapper;
import com.company.recruitment.repository.JobRepository;
import com.company.recruitment.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public JobResponseDto createJob(JobRequestDto request) {
        log.info("Creating new job posting: {}", request.getJobTitle());
        Job job = jobMapper.toEntity(request);
        Job saved = jobRepository.save(job);
        auditLogService.logAction("Job", saved.getId().toString(), "CREATE_JOB", "RECRUITER", null, saved.getStatus(), "Created job: " + saved.getJobTitle());
        return jobMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponseDto getJobById(UUID id) {
        Job job = getJobEntityById(id);
        return jobMapper.toResponseDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobEntityById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponseDto> getAllJobs(String status) {
        List<Job> jobs;
        if (status != null && !status.trim().isEmpty()) {
            jobs = jobRepository.findByStatus(status.toUpperCase());
        } else {
            jobs = jobRepository.findAll();
        }
        return jobs.stream().map(jobMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobResponseDto updateJob(UUID id, JobRequestDto request) {
        log.info("Updating job posting: id={}", id);
        Job job = getJobEntityById(id);
        String oldState = job.getStatus() + " - " + job.getJobTitle();
        jobMapper.updateEntityFromDto(request, job);
        Job updated = jobRepository.save(job);
        auditLogService.logAction("Job", updated.getId().toString(), "UPDATE_JOB", "RECRUITER", oldState, updated.getStatus(), "Updated job details");
        return jobMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteJob(UUID id) {
        log.info("Closing/Deleting job: id={}", id);
        Job job = getJobEntityById(id);
        job.setStatus("CLOSED");
        jobRepository.save(job);
        auditLogService.logAction("Job", job.getId().toString(), "CLOSE_JOB", "RECRUITER", "ACTIVE", "CLOSED", "Closed job posting");
    }

    @Override
    @Transactional(readOnly = true)
    public Job findActiveJobByTitleOrKeyword(String text) {
        if (text == null || text.trim().isEmpty()) {
            return getFirstActiveJob();
        }
        List<Job> activeJobs = jobRepository.findByStatus("ACTIVE");
        if (activeJobs.isEmpty()) {
            return null;
        }
        String lowerText = text.toLowerCase();
        for (Job job : activeJobs) {
            if (lowerText.contains(job.getJobTitle().toLowerCase())) {
                return job;
            }
        }
        // Fallback: check required skills
        for (Job job : activeJobs) {
            for (String skill : job.getRequiredSkills()) {
                if (lowerText.contains(skill.toLowerCase())) {
                    return job;
                }
            }
        }
        return activeJobs.getFirst();
    }

    private Job getFirstActiveJob() {
        List<Job> activeJobs = jobRepository.findByStatus("ACTIVE");
        return activeJobs.isEmpty() ? null : activeJobs.getFirst();
    }
}
