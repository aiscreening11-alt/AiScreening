package com.company.recruitment.service.interview;

import com.company.recruitment.dto.request.InterviewResultRequestDto;
import com.company.recruitment.dto.request.InterviewerFeedbackSubmissionDto;
import com.company.recruitment.dto.request.ScheduleInterviewRequestDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.InterviewDto;
import com.company.recruitment.dto.response.InterviewerSessionDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.Interview;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.entity.enums.InterviewResult;
import com.company.recruitment.entity.enums.InterviewRound;
import com.company.recruitment.exception.ResourceNotFoundException;
import com.company.recruitment.mapper.CandidateMapper;
import com.company.recruitment.mapper.InterviewMapper;
import com.company.recruitment.repository.CandidateRepository;
import com.company.recruitment.repository.InterviewRepository;
import com.company.recruitment.service.CandidateService;
import com.company.recruitment.service.audit.AuditLogService;
import com.company.recruitment.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final CandidateService candidateService;
    private final CandidateRepository candidateRepository;
    private final InterviewRepository interviewRepository;
    private final EmailService emailService;
    private final CandidateMapper candidateMapper;
    private final InterviewMapper interviewMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CandidateResponseDto scheduleInterview(UUID candidateId, ScheduleInterviewRequestDto request) {
        log.info("Scheduling {} interview for candidate id={}", request.getRound(), candidateId);

        Candidate candidate = candidateService.getCandidateEntityById(candidateId);
        CandidateStatus oldStatus = candidate.getStatus();

        // 1. Generate or validate Meeting Link
        String meetingLink = request.getMeetingLink();
        if (meetingLink == null || meetingLink.trim().isEmpty()) {
            meetingLink = generateFunctioningMeetingLink(request.getRound(), candidate);
        } else {
            meetingLink = sanitizeMeetingLink(meetingLink.trim());
        }

        // 2. Build Interview Entity
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        Interview interview = Interview.builder()
                .candidate(candidate)
                .round(request.getRound() != null ? request.getRound() : InterviewRound.L1_TECHNICAL)
                .interviewDate(request.getInterviewDate() != null ? request.getInterviewDate() : LocalDateTime.now().plusDays(2))
                .interviewerName(request.getInterviewerName() != null ? request.getInterviewerName().trim() : "Technical Interviewer")
                .interviewerEmail(request.getInterviewerEmail() != null ? request.getInterviewerEmail().trim() : null)
                .meetingLink(meetingLink.trim())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 45)
                .notes(request.getNotes())
                .accessToken(accessToken)
                .result(InterviewResult.PENDING)
                .build();

        interviewRepository.save(interview);
        candidate.addInterview(interview);

        // 3. Advance Candidate Status based on round scheduled
        CandidateStatus nextStatus = determineScheduledStatus(interview.getRound());
        candidate.setStatus(nextStatus);
        candidateRepository.save(candidate);

        // 4. Send dual automated emails (Candidate + Interviewer)
        emailService.sendInterviewScheduleEmails(interview);

        // 5. Audit Logging
        auditLogService.logAction("Candidate", candidateId.toString(), "INTERVIEW_SCHEDULED_" + interview.getRound().name(),
                request.getInterviewerName() != null ? request.getInterviewerName() : "HR_ADMIN",
                oldStatus.name(), candidate.getStatus().name(),
                "Scheduled " + interview.getRound().getDisplayName() + " on " + interview.getInterviewDate() +
                        " with Meet Link: " + interview.getMeetingLink());

        return candidateMapper.toResponseDto(candidate);
    }

    @Override
    @Transactional
    public CandidateResponseDto recordRoundDecision(UUID candidateId, UUID interviewId, InterviewResultRequestDto request) {
        log.info("Recording round decision for candidate id={}, interview id={}: verdict={}", candidateId, interviewId, request.getResult());

        Candidate candidate = candidateService.getCandidateEntityById(candidateId);
        CandidateStatus oldStatus = candidate.getStatus();

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        if (!interview.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("Interview does not belong to candidate id: " + candidateId);
        }

        interview.setResult(request.getResult());
        if (request.getFeedback() != null) {
            interview.setFeedback(request.getFeedback());
        }
        if (request.getComments() != null) {
            interview.setComments(request.getComments());
            if (interview.getFeedback() == null || interview.getFeedback().isBlank()) {
                interview.setFeedback(request.getComments());
            }
        }
        if (request.getTechnicalRating() != null) {
            interview.setTechnicalRating(request.getTechnicalRating());
        }
        if (request.getCommunicationRating() != null) {
            interview.setCommunicationRating(request.getCommunicationRating());
        }
        if (request.getConfidenceRating() != null) {
            interview.setConfidenceRating(request.getConfidenceRating());
        }
        if (request.getBodyLanguageRating() != null) {
            interview.setBodyLanguageRating(request.getBodyLanguageRating());
        }
        if (interview.getTechnicalRating() != null && interview.getCommunicationRating() != null
                && interview.getConfidenceRating() != null && interview.getBodyLanguageRating() != null) {
            double avg = (interview.getTechnicalRating() + interview.getCommunicationRating()
                    + interview.getConfidenceRating() + interview.getBodyLanguageRating()) / 4.0;
            interview.setOverallRating(Math.round(avg * 100.0) / 100.0);
            interview.setEvaluatedAt(LocalDateTime.now());
        }
        if (request.getRecommendation() != null && !request.getRecommendation().isBlank()) {
            interview.setRecommendation(request.getRecommendation());
        } else {
            interview.setRecommendation(request.getResult() == InterviewResult.SELECTED ? "RECOMMENDED" : "NOT_RECOMMENDED");
        }
        if (request.getInterviewerName() != null && !request.getInterviewerName().isBlank()) {
            interview.setInterviewerName(request.getInterviewerName());
        }
        interviewRepository.save(interview);

        // Advance or Reject candidate based on decision
        if (request.getResult() == InterviewResult.SELECTED) {
            CandidateStatus passedStatus = determinePassedStatus(interview.getRound());
            candidate.setStatus(passedStatus);
            candidateRepository.save(candidate);

            // If passed the final HR round, trigger the official candidate selection/offer email
            if (interview.getRound() == InterviewRound.HR_ROUND) {
                emailService.sendSelectionEmail(candidate);
            }
        } else if (request.getResult() == InterviewResult.NOT_SELECTED) {
            candidate.setStatus(CandidateStatus.NOT_SELECTED);
            candidateRepository.save(candidate);
            emailService.sendNotSelectedEmail(candidate);
        }

        auditLogService.logAction("Candidate", candidateId.toString(), "INTERVIEW_DECISION_" + interview.getRound().name(),
                request.getInterviewerName() != null ? request.getInterviewerName() : "HR_ADMIN",
                oldStatus.name(), candidate.getStatus().name(),
                "Recorded decision for " + interview.getRound().getDisplayName() + ": " + request.getResult() +
                        ". Feedback: " + request.getFeedback());

        return candidateMapper.toResponseDto(candidate);
    }

    @Override
    @Transactional
    public CandidateResponseDto recordInterviewResult(UUID candidateId, InterviewResultRequestDto request) {
        Candidate candidate = candidateService.getCandidateEntityById(candidateId);

        // Find the latest pending interview if exists, or schedule a default L1
        Interview targetInterview = candidate.getInterviews().stream()
                .filter(i -> i.getResult() == InterviewResult.PENDING)
                .reduce((first, second) -> second)
                .orElse(null);

        if (targetInterview != null) {
            return recordRoundDecision(candidateId, targetInterview.getId(), request);
        }

        // Backward compatibility: create a record and decide
        Interview interview = Interview.builder()
                .candidate(candidate)
                .round(InterviewRound.L1_TECHNICAL)
                .interviewDate(request.getInterviewDate() != null ? request.getInterviewDate() : LocalDateTime.now())
                .interviewerName(request.getInterviewerName() != null ? request.getInterviewerName() : "HR Admin")
                .feedback(request.getFeedback())
                .result(request.getResult())
                .meetingLink(generateFunctioningMeetingLink(InterviewRound.L1_TECHNICAL, candidate))
                .build();

        interviewRepository.save(interview);
        candidate.addInterview(interview);

        if (request.getResult() == InterviewResult.SELECTED) {
            candidate.setStatus(CandidateStatus.L1_PASSED);
            candidateRepository.save(candidate);
        } else if (request.getResult() == InterviewResult.NOT_SELECTED) {
            candidate.setStatus(CandidateStatus.NOT_SELECTED);
            candidateRepository.save(candidate);
            emailService.sendNotSelectedEmail(candidate);
        }

        return candidateMapper.toResponseDto(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getCandidateInterviews(UUID candidateId) {
        return interviewRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId)
                .stream()
                .map(interviewMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewerSessionDto getInterviewerSessionByToken(String accessToken) {
        Interview interview = interviewRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with token: " + accessToken));
        return buildInterviewerSessionDto(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewerSessionDto getInterviewerSessionById(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with id: " + interviewId));
        return buildInterviewerSessionDto(interview);
    }

    @Override
    @Transactional
    public CandidateResponseDto submitInterviewerFeedback(UUID interviewId, InterviewerFeedbackSubmissionDto feedbackDto) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        interview.setTechnicalRating(feedbackDto.getTechnicalRating());
        interview.setCommunicationRating(feedbackDto.getCommunicationRating());
        interview.setConfidenceRating(feedbackDto.getConfidenceRating());
        interview.setBodyLanguageRating(feedbackDto.getBodyLanguageRating());

        double avg = (feedbackDto.getTechnicalRating() + feedbackDto.getCommunicationRating()
                + feedbackDto.getConfidenceRating() + feedbackDto.getBodyLanguageRating()) / 4.0;
        interview.setOverallRating(Math.round(avg * 100.0) / 100.0);
        interview.setEvaluatedAt(LocalDateTime.now());
        interview.setRecommendation(feedbackDto.getResult() == InterviewResult.SELECTED ? "RECOMMENDED" : "NOT_RECOMMENDED");
        interview.setComments(feedbackDto.getComments());
        interview.setFeedback(feedbackDto.getComments());
        interview.setResult(feedbackDto.getResult());

        if (feedbackDto.getInterviewerName() != null && !feedbackDto.getInterviewerName().isBlank()) {
            interview.setInterviewerName(feedbackDto.getInterviewerName().trim());
        }

        interviewRepository.save(interview);

        Candidate candidate = interview.getCandidate();
        CandidateStatus oldStatus = candidate.getStatus();

        if (feedbackDto.getResult() == InterviewResult.SELECTED) {
            CandidateStatus passedStatus = determinePassedStatus(interview.getRound());
            candidate.setStatus(passedStatus);
            candidateRepository.save(candidate);

            if (interview.getRound() == InterviewRound.HR_ROUND) {
                emailService.sendSelectionEmail(candidate);
            }
        } else {
            candidate.setStatus(CandidateStatus.NOT_SELECTED);
            candidateRepository.save(candidate);
            emailService.sendNotSelectedEmail(candidate);
        }

        auditLogService.logAction("Candidate", candidate.getId().toString(), "INTERVIEWER_FEEDBACK_" + interview.getRound().name(),
                interview.getInterviewerName() != null ? interview.getInterviewerName() : "INTERVIEWER",
                oldStatus.name(), candidate.getStatus().name(),
                String.format("Interviewer submitted evaluation for %s: Result=%s, Overall Rating=%.2f/5 (Tech:%d, Comm:%d, Conf:%d, BodyLang:%d). Comments: %s",
                        interview.getRound().getDisplayName(), feedbackDto.getResult(), interview.getOverallRating(),
                        feedbackDto.getTechnicalRating(), feedbackDto.getCommunicationRating(),
                        feedbackDto.getConfidenceRating(), feedbackDto.getBodyLanguageRating(),
                        feedbackDto.getComments()));

        return candidateMapper.toResponseDto(candidate);
    }

    @Override
    @Transactional
    public CandidateResponseDto submitInterviewerFeedbackByToken(String accessToken, InterviewerFeedbackSubmissionDto feedbackDto) {
        Interview interview = interviewRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with token: " + accessToken));
        return submitInterviewerFeedback(interview.getId(), feedbackDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getInterviewsByInterviewerEmail(String email) {
        return interviewRepository.findByInterviewerEmailIgnoreCaseOrderByInterviewDateDesc(email)
                .stream()
                .map(interviewMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getRecentEvaluations() {
        return interviewRepository.findTop15ByEvaluatedAtIsNotNullOrderByEvaluatedAtDesc()
                .stream()
                .map(interviewMapper::toDto)
                .collect(Collectors.toList());
    }

    private InterviewerSessionDto buildInterviewerSessionDto(Interview interview) {
        Candidate candidate = interview.getCandidate();
        List<String> skillNames = candidate != null && candidate.getSkills() != null
                ? candidate.getSkills().stream().map(s -> s.getSkillName()).collect(Collectors.toList())
                : List.of();

        return InterviewerSessionDto.builder()
                .interviewId(interview.getId())
                .accessToken(interview.getAccessToken())
                .candidateId(candidate != null ? candidate.getId() : null)
                .candidateName(candidate != null ? candidate.getFullName() : "Candidate")
                .candidateEmail(candidate != null ? (candidate.getResumeEmail() != null ? candidate.getResumeEmail() : candidate.getSenderEmail()) : "")
                .phoneNumber(candidate != null ? candidate.getPhoneNumber() : "")
                .jobTitle(candidate != null && candidate.getJob() != null ? candidate.getJob().getJobTitle() : "Open Position")
                .department(candidate != null && candidate.getJob() != null ? candidate.getJob().getDepartment() : "General")
                .atsScore(candidate != null && candidate.getAtsResult() != null ? candidate.getAtsResult().getFinalScore() : null)
                .skills(skillNames)
                .resumeFileName(candidate != null && candidate.getResume() != null ? candidate.getResume().getFileName() : null)
                .round(interview.getRound())
                .roundDisplayName(interview.getRound() != null ? interview.getRound().getDisplayName() : "Interview Round")
                .interviewDate(interview.getInterviewDate())
                .durationMinutes(interview.getDurationMinutes())
                .meetingLink(interview.getMeetingLink())
                .interviewerName(interview.getInterviewerName())
                .interviewerEmail(interview.getInterviewerEmail())
                .notes(interview.getNotes())
                .evaluated(interview.getEvaluatedAt() != null || interview.getResult() != InterviewResult.PENDING)
                .technicalRating(interview.getTechnicalRating())
                .communicationRating(interview.getCommunicationRating())
                .confidenceRating(interview.getConfidenceRating())
                .bodyLanguageRating(interview.getBodyLanguageRating())
                .overallRating(interview.getOverallRating())
                .recommendation(interview.getRecommendation())
                .comments(interview.getComments() != null ? interview.getComments() : interview.getFeedback())
                .result(interview.getResult())
                .evaluatedAt(interview.getEvaluatedAt())
                .build();
    }

    private CandidateStatus determineScheduledStatus(InterviewRound round) {
        if (round == null) return CandidateStatus.L1_SCHEDULED;
        return switch (round) {
            case L1_TECHNICAL -> CandidateStatus.L1_SCHEDULED;
            case L2_TECHNICAL -> CandidateStatus.L2_SCHEDULED;
            case MANAGERIAL -> CandidateStatus.MANAGERIAL_SCHEDULED;
            case HR_ROUND -> CandidateStatus.HR_SCHEDULED;
        };
    }

    private CandidateStatus determinePassedStatus(InterviewRound round) {
        if (round == null) return CandidateStatus.L1_PASSED;
        return switch (round) {
            case L1_TECHNICAL -> CandidateStatus.L1_PASSED;
            case L2_TECHNICAL -> CandidateStatus.L2_PASSED;
            case MANAGERIAL -> CandidateStatus.MANAGERIAL_PASSED;
            case HR_ROUND -> CandidateStatus.SELECTED; // Final hire / offer state
        };
    }

    @Override
    @Transactional
    public void resendInterviewInvitation(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));
        log.info("Resending interview schedule invitations for interview id={}", interviewId);
        emailService.sendInterviewScheduleEmails(interview);
    }

    private String generateFunctioningMeetingLink(InterviewRound round, Candidate candidate) {
        String roundName = round != null ? round.name() : "L1_TECHNICAL";
        String candidateName = "Candidate";
        if (candidate != null && candidate.getFullName() != null && !candidate.getFullName().isBlank()) {
            candidateName = candidate.getFullName().replaceAll("[^a-zA-Z0-9]", "");
        }
        if (candidateName.isBlank()) {
            candidateName = "Candidate";
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "https://meet.jit.si/Sanvii-Interview-" + candidateName + "-" + roundName + "-" + suffix;
    }

    private String sanitizeMeetingLink(String link) {
        if (link == null) return null;
        String trimmed = link.trim();
        // If user entered a plain Google Meet code like "abc-defg-hij" without https://
        if (trimmed.matches("^[a-z]{3}-[a-z]{4}-[a-z]{3}$")) {
            return "https://meet.google.com/" + trimmed;
        }
        return trimmed;
    }
}
