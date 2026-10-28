package com.company.recruitment.mapper;

import com.company.recruitment.dto.response.InterviewDto;
import com.company.recruitment.entity.Interview;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewDto toDto(Interview entity) {
        if (entity == null) return null;
        return InterviewDto.builder()
                .id(entity.getId())
                .round(entity.getRound())
                .roundDisplayName(entity.getRound() != null ? entity.getRound().getDisplayName() : null)
                .interviewDate(entity.getInterviewDate())
                .interviewerName(entity.getInterviewerName())
                .interviewerEmail(entity.getInterviewerEmail())
                .meetingLink(entity.getMeetingLink())
                .durationMinutes(entity.getDurationMinutes())
                .notes(entity.getNotes())
                .feedback(entity.getFeedback())
                .technicalRating(entity.getTechnicalRating())
                .communicationRating(entity.getCommunicationRating())
                .confidenceRating(entity.getConfidenceRating())
                .bodyLanguageRating(entity.getBodyLanguageRating())
                .overallRating(entity.getOverallRating())
                .recommendation(entity.getRecommendation())
                .comments(entity.getComments() != null ? entity.getComments() : entity.getFeedback())
                .evaluatedAt(entity.getEvaluatedAt())
                .accessToken(entity.getAccessToken())
                .candidateId(entity.getCandidate() != null ? entity.getCandidate().getId() : null)
                .candidateName(entity.getCandidate() != null ? entity.getCandidate().getFullName() : null)
                .jobTitle(entity.getCandidate() != null && entity.getCandidate().getJob() != null ? entity.getCandidate().getJob().getJobTitle() : null)
                .result(entity.getResult())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
