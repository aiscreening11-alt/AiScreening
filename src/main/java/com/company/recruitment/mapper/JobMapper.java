package com.company.recruitment.mapper;

import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.entity.Job;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class JobMapper {

    public Job toEntity(JobRequestDto dto) {
        if (dto == null) return null;
        return Job.builder()
                .jobTitle(dto.getJobTitle())
                .department(dto.getDepartment())
                .description(dto.getDescription())
                .requiredSkills(dto.getRequiredSkills() != null ? new ArrayList<>(dto.getRequiredSkills()) : new ArrayList<>())
                .preferredSkills(dto.getPreferredSkills() != null ? new ArrayList<>(dto.getPreferredSkills()) : new ArrayList<>())
                .minimumExperience(dto.getMinimumExperience() != null ? dto.getMinimumExperience() : 0.0)
                .educationRequirement(dto.getEducationRequirement())
                .location(dto.getLocation())
                .employmentType(dto.getEmploymentType() != null ? dto.getEmploymentType() : "FULL_TIME")
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();
    }

    public void updateEntityFromDto(JobRequestDto dto, Job entity) {
        if (dto == null || entity == null) return;
        entity.setJobTitle(dto.getJobTitle());
        entity.setDepartment(dto.getDepartment());
        entity.setDescription(dto.getDescription());
        if (dto.getRequiredSkills() != null) {
            entity.setRequiredSkills(new ArrayList<>(dto.getRequiredSkills()));
        }
        if (dto.getPreferredSkills() != null) {
            entity.setPreferredSkills(new ArrayList<>(dto.getPreferredSkills()));
        }
        if (dto.getMinimumExperience() != null) {
            entity.setMinimumExperience(dto.getMinimumExperience());
        }
        entity.setEducationRequirement(dto.getEducationRequirement());
        entity.setLocation(dto.getLocation());
        if (dto.getEmploymentType() != null) {
            entity.setEmploymentType(dto.getEmploymentType());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    public JobResponseDto toResponseDto(Job entity) {
        if (entity == null) return null;
        return JobResponseDto.builder()
                .id(entity.getId())
                .jobTitle(entity.getJobTitle())
                .department(entity.getDepartment())
                .description(entity.getDescription())
                .requiredSkills(entity.getRequiredSkills() != null ? new ArrayList<>(entity.getRequiredSkills()) : new ArrayList<>())
                .preferredSkills(entity.getPreferredSkills() != null ? new ArrayList<>(entity.getPreferredSkills()) : new ArrayList<>())
                .minimumExperience(entity.getMinimumExperience())
                .educationRequirement(entity.getEducationRequirement())
                .location(entity.getLocation())
                .employmentType(entity.getEmploymentType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
