package com.company.recruitment.mapper;

import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.CandidateSkillDto;
import com.company.recruitment.dto.response.CandidateSummaryDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.CandidateSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CandidateMapper {

    private final JobMapper jobMapper;
    private final AtsResultMapper atsResultMapper;
    private final InterviewMapper interviewMapper;
    private final EmailCommunicationMapper emailCommunicationMapper;

    public CandidateSummaryDto toSummaryDto(Candidate entity) {
        if (entity == null) return null;

        List<String> topSkills = entity.getSkills() != null
                ? entity.getSkills().stream().map(CandidateSkill::getNormalizedSkill).distinct().limit(6).collect(Collectors.toList())
                : Collections.emptyList();

        Double atsScore = entity.getAtsResult() != null ? entity.getAtsResult().getFinalScore() : null;
        String email = entity.getResumeEmail() != null ? entity.getResumeEmail() : entity.getSenderEmail();

        return CandidateSummaryDto.builder()
                .id(entity.getId())
                .jobId(entity.getJob() != null ? entity.getJob().getId() : null)
                .jobTitle(entity.getJob() != null ? entity.getJob().getJobTitle() : null)
                .fullName(entity.getFullName())
                .email(email)
                .phoneNumber(entity.getPhoneNumber())
                .totalExperienceYears(entity.getTotalExperienceYears())
                .currentCompany(entity.getCurrentCompany())
                .gender(entity.getGender())
                .status(entity.getStatus())
                .finalAtsScore(atsScore)
                .topSkills(topSkills)
                .emailDiscrepancyFlag(entity.getEmailDiscrepancyFlag())
                .appliedAt(entity.getAppliedAt())
                .build();
    }

    public CandidateResponseDto toResponseDto(Candidate entity) {
        if (entity == null) return null;

        List<CandidateSkillDto> skillDtos = entity.getSkills() != null
                ? entity.getSkills().stream().map(s -> CandidateSkillDto.builder()
                        .id(s.getId())
                        .skillName(s.getSkillName())
                        .normalizedSkill(s.getNormalizedSkill())
                        .category(s.getCategory())
                        .isMatchedRequirement(s.getIsMatchedRequirement())
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return CandidateResponseDto.builder()
                .id(entity.getId())
                .job(entity.getJob() != null ? jobMapper.toResponseDto(entity.getJob()) : null)
                .fullName(entity.getFullName())
                .senderEmail(entity.getSenderEmail())
                .resumeEmail(entity.getResumeEmail())
                .emailDiscrepancyFlag(entity.getEmailDiscrepancyFlag())
                .phoneNumber(entity.getPhoneNumber())
                .totalExperienceYears(entity.getTotalExperienceYears())
                .currentCompany(entity.getCurrentCompany())
                .previousCompanies(entity.getPreviousCompanies())
                .education(entity.getEducation())
                .degree(entity.getDegree())
                .location(entity.getLocation())
                .noticePeriod(entity.getNoticePeriod())
                .gender(entity.getGender())
                .status(entity.getStatus())
                .resumeFileName(entity.getResume() != null ? entity.getResume().getFileName() : null)
                .resumeFileType(entity.getResume() != null ? entity.getResume().getFileType() : null)
                .extractedResumeText(entity.getResume() != null ? entity.getResume().getExtractedText() : null)
                .skills(skillDtos)
                .atsResult(entity.getAtsResult() != null ? atsResultMapper.toDto(entity.getAtsResult()) : null)
                .interviews(entity.getInterviews() != null ? entity.getInterviews().stream().map(interviewMapper::toDto).collect(Collectors.toList()) : Collections.emptyList())
                .emailCommunications(entity.getEmailCommunications() != null ? entity.getEmailCommunications().stream().map(emailCommunicationMapper::toDto).collect(Collectors.toList()) : Collections.emptyList())
                .appliedAt(entity.getAppliedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
