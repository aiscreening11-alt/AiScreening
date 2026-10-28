package com.company.recruitment.service;

import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.entity.Job;
import com.company.recruitment.exception.JobNotFoundException;
import com.company.recruitment.mapper.JobMapper;
import com.company.recruitment.repository.JobRepository;
import com.company.recruitment.service.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private JobMapper jobMapper = new JobMapper();

    @InjectMocks
    private JobServiceImpl jobService;

    private Job testJob;
    private UUID testJobId;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testJob = Job.builder()
                .id(testJobId)
                .jobTitle("Java Spring Boot Developer")
                .department("Engineering")
                .description("Build microservices backend")
                .requiredSkills(List.of("Java", "Spring Boot", "Kafka", "SQL", "AWS"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .minimumExperience(5.0)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testCreateJob_Success() {
        JobRequestDto request = JobRequestDto.builder()
                .jobTitle("Java Spring Boot Developer")
                .department("Engineering")
                .description("Build microservices backend")
                .requiredSkills(List.of("Java", "Spring Boot", "Kafka", "SQL", "AWS"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .minimumExperience(5.0)
                .build();

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(testJobId);
            return j;
        });

        JobResponseDto response = jobService.createJob(request);

        assertNotNull(response);
        assertEquals("Java Spring Boot Developer", response.getJobTitle());
        assertEquals(5, response.getRequiredSkills().size());
        verify(jobRepository, times(1)).save(any(Job.class));
        verify(auditLogService, times(1)).logAction(eq("Job"), eq(testJobId.toString()), eq("CREATE_JOB"), any(), any(), any(), any());
    }

    @Test
    void testGetJobById_Found() {
        when(jobRepository.findById(testJobId)).thenReturn(Optional.of(testJob));

        JobResponseDto response = jobService.getJobById(testJobId);

        assertNotNull(response);
        assertEquals(testJobId, response.getId());
        assertEquals("Java Spring Boot Developer", response.getJobTitle());
    }

    @Test
    void testGetJobById_NotFound_ThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(jobRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobById(unknownId));
    }
}
