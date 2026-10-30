package com.company.recruitment.controller;

import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponseDto>> createJob(@Valid @RequestBody JobRequestDto request) {
        JobResponseDto created = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Job created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponseDto>>> getAllJobs(@RequestParam(required = false) String status) {
        List<JobResponseDto> jobs = jobService.getAllJobs(status);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Jobs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponseDto>> getJobById(@PathVariable UUID id) {
        JobResponseDto job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job, "Job retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponseDto>> updateJob(@PathVariable UUID id, @Valid @RequestBody JobRequestDto request) {
        JobResponseDto updated = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Job updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean permanent) {
        jobService.deleteJob(id, permanent);
        return ResponseEntity.ok(ApiResponse.success(null, permanent ? "Job permanently deleted successfully" : "Job closed/archived successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobResponseDto>> updateJobStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        JobResponseDto updated = jobService.updateJobStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(updated, "Job status updated to " + status));
    }
}
