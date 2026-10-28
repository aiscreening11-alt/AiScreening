package com.company.recruitment.service.gmail;

import com.company.recruitment.config.AtsScoringProperties;
import com.company.recruitment.entity.*;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.entity.enums.Gender;
import com.company.recruitment.exception.DuplicateApplicationException;
import com.company.recruitment.exception.ResumeProcessingException;
import com.company.recruitment.repository.*;
import com.company.recruitment.service.JobService;
import com.company.recruitment.service.ai.AIResumeAnalyzer;
import com.company.recruitment.service.ai.AIResumeAnalyzerFactory;
import com.company.recruitment.service.ats.ATSScoringService;
import com.company.recruitment.service.ats.SkillNormalizationService;
import com.company.recruitment.service.email.EmailService;
import com.company.recruitment.service.resume.CandidateInfoExtractor;
import com.company.recruitment.service.resume.ResumeParserService;
import com.company.recruitment.service.resume.storage.ResumeStorageService;
import com.company.recruitment.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailIngestionPipeline {

    private final EmailMessageRepository emailMessageRepository;
    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;
    private final AtsResultRepository atsResultRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final JobService jobService;
    private final ResumeParserService resumeParserService;
    private final ResumeStorageService resumeStorageService;
    private final CandidateInfoExtractor candidateInfoExtractor;
    private final ATSScoringService atsScoringService;
    private final SkillNormalizationService skillNormalizationService;
    private final AIResumeAnalyzerFactory aiResumeAnalyzerFactory;
    private final AtsScoringProperties atsProperties;
    private final EmailService emailService;
    private final GmailClient gmailClient;

    @Async
    @Transactional
    public void processGmailMessage(GmailClient.InboundEmailDetails emailDetails) {
        String msgId = emailDetails.getMessageId();
        log.info("Starting ingestion pipeline for Gmail Message ID: {}", msgId);

        // 1. Idempotency check
        if (emailMessageRepository.existsByGmailMessageId(msgId)) {
            log.info("Gmail message {} has already been processed. Skipping.", msgId);
            return;
        }

        String body = (emailDetails.getBodyText() != null && !emailDetails.getBodyText().trim().isEmpty())
                ? emailDetails.getBodyText()
                : emailDetails.getSnippet();

        EmailMessage emailMessage = EmailMessage.builder()
                .gmailMessageId(msgId)
                .senderEmail(emailDetails.getSenderEmail())
                .subject(emailDetails.getSubject())
                .bodySnippet(body)
                .receivedAt(emailDetails.getReceivedAt() != null ? emailDetails.getReceivedAt() : LocalDateTime.now())
                .processed(false)
                .processingStatus("PROCESSING")
                .build();

        emailMessage = emailMessageRepository.save(emailMessage);

        try {
            // Validate incoming application: Subject, Body, and Resume Attachment
            List<String> missingItems = validateInboundEmail(
                    emailDetails.getSubject(),
                    body,
                    emailDetails.getAttachments() != null && !emailDetails.getAttachments().isEmpty()
            );

            if (!missingItems.isEmpty()) {
                log.warn("Inbound application from '{}' is incomplete. Missing: {}. Sending rejection notice.",
                        emailDetails.getSenderEmail(), missingItems);

                Candidate rejectedCandidate = handleIncompleteApplication(
                        emailDetails.getSenderEmail(),
                        emailDetails.getSenderName(),
                        emailDetails.getSubject(),
                        missingItems,
                        null
                );

                emailMessage.setCandidate(rejectedCandidate);
                emailMessage.setProcessed(true);
                emailMessage.setProcessingStatus("REJECTED_INCOMPLETE");
                emailMessage.setErrorMessage(String.join("; ", missingItems));
                emailMessage.setProcessedAt(LocalDateTime.now());
                emailMessageRepository.save(emailMessage);

                gmailClient.markAsReadAndProcessed(msgId);
                return;
            }

            // Resume attachment is present and valid
            GmailClient.AttachmentMetadata resumeAttachment = findResumeAttachment(emailDetails.getAttachments());
            byte[] fileBytes = gmailClient.downloadAttachment(msgId, resumeAttachment.getAttachmentId());
            String fileName = resumeAttachment.getFileName();

            // Process resume through ATS engine
            Candidate candidate = processResumeBytes(fileBytes, fileName, emailDetails.getSenderEmail(), emailDetails.getSubject());

            emailMessage.setCandidate(candidate);
            emailMessage.setProcessed(true);
            emailMessage.setProcessingStatus("COMPLETED");
            emailMessage.setProcessedAt(LocalDateTime.now());
            emailMessageRepository.save(emailMessage);

            // Mark message as read in Gmail
            gmailClient.markAsReadAndProcessed(msgId);

            log.info("Successfully completed ingestion pipeline for email {} -> Candidate ID: {}", msgId, candidate.getId());

        } catch (Exception e) {
            log.error("Failed to process Gmail message {}: {}", msgId, e.getMessage(), e);
            emailMessage.setProcessed(true);
            emailMessage.setProcessingStatus("FAILED");
            emailMessage.setErrorMessage(e.getMessage());
            emailMessageRepository.save(emailMessage);
        }
    }

    public List<String> validateInboundEmail(String subject, String body, boolean hasAttachment) {
        List<String> missingItems = new ArrayList<>();

        // 1. Validate Subject Line
        if (subject == null || subject.trim().isEmpty()) {
            missingItems.add("Missing subject line: Your email did not contain a subject line. The subject line must state the position applied for (as specified in our LinkedIn post).");
        } else {
            String lower = subject.trim().toLowerCase();
            if (lower.equals("(no subject)") || lower.equals("no subject") || lower.equals("test") || lower.equals("hi") || lower.equals("hello") || subject.trim().length() < 5) {
                missingItems.add("Improper subject line: The subject line '" + subject + "' is too generic. Please state the specific job title (e.g., 'Application for [Job Title]').");
            }
        }

        // 2. Validate Email Body
        if (body == null || body.trim().isEmpty()) {
            missingItems.add("Missing email body: Your email body was empty. An application email must include an introductory cover note explaining your relevant background.");
        } else if (body.trim().length() < 15) {
            missingItems.add("Insufficient email body: Your message body is too short. Please provide a brief introductory note about your experience and interest.");
        }

        // 3. Validate Resume Attachment
        if (!hasAttachment) {
            missingItems.add("Missing resume attachment: No resume document was attached. Please attach your resume in PDF, DOC, or DOCX format.");
        }

        return missingItems;
    }

    @Transactional
    public Candidate handleIncompleteApplication(
            String senderEmail,
            String senderName,
            String subject,
            List<String> missingItems,
            UUID targetJobId) {

        Job job = null;
        if (targetJobId != null) {
            job = jobService.getJobEntityById(targetJobId);
        }
        if (job == null && subject != null && !subject.isBlank()) {
            job = jobService.findActiveJobByTitleOrKeyword(subject);
        }
        if (job == null) {
            List<com.company.recruitment.dto.response.JobResponseDto> activeJobs = jobService.getAllJobs("ACTIVE");
            if (!activeJobs.isEmpty()) {
                job = jobService.getJobEntityById(activeJobs.get(0).getId());
            }
        }

        String candidateName = (senderName != null && !senderName.isBlank()) ? senderName : "Applicant";
        Gender gender = candidateInfoExtractor.inferGender(candidateName, "");

        Candidate candidate = Candidate.builder()
                .job(job)
                .fullName(candidateName)
                .senderEmail(senderEmail != null ? senderEmail : "unknown@sender.local")
                .resumeEmail(senderEmail != null ? senderEmail : "unknown@sender.local")
                .location("Email Application")
                .gender(gender)
                .status(CandidateStatus.REJECTED)
                .build();
        candidate = candidateRepository.save(candidate);

        AtsResult atsResult = AtsResult.builder()
                .candidate(candidate)
                .ruleScore(0.0)
                .aiScore(0.0)
                .finalScore(0.0)
                .decision(CandidateStatus.REJECTED)
                .matchedSkills(new ArrayList<>())
                .missingSkills(job != null && job.getRequiredSkills() != null ? new ArrayList<>(job.getRequiredSkills()) : new ArrayList<>())
                .aiSummary("Application rejected: Incomplete submission. " + String.join(" | ", missingItems))
                .scoringBreakdownJson("{\"status\":\"REJECTED\",\"reason\":\"INCOMPLETE_SUBMISSION\"}")
                .evaluatedAt(LocalDateTime.now())
                .build();
        atsResultRepository.save(atsResult);
        candidate.setAtsResult(atsResult);

        // Shoot the rejection notification email detailing missing items
        emailService.sendIncompleteApplicationRejectionEmail(candidate, missingItems);

        return candidate;
    }

    @Transactional
    public Candidate processManualResumeUpload(byte[] fileBytes, String fileName, UUID targetJobId, String senderEmail) {
        log.info("Processing manual resume upload for file: '{}'", fileName);
        Job job = null;
        if (targetJobId != null) {
            job = jobService.getJobEntityById(targetJobId);
        }
        if (job == null) {
            List<com.company.recruitment.dto.response.JobResponseDto> activeJobs = jobService.getAllJobs("ACTIVE");
            if (!activeJobs.isEmpty()) {
                job = jobService.getJobEntityById(activeJobs.get(0).getId());
            }
        }
        if (job == null) {
            throw new ResumeProcessingException("No active job available for resume screening");
        }
        return executeProcessingFlow(fileBytes, fileName, senderEmail != null ? senderEmail : "manual@upload.local", "Application", job);
    }

    private Candidate processResumeBytes(byte[] fileBytes, String fileName, String senderEmail, String subject) {
        Job job = jobService.findActiveJobByTitleOrKeyword(subject != null ? subject : fileName);
        if (job == null) {
            throw new ResumeProcessingException("No active job available for resume screening");
        }
        return executeProcessingFlow(fileBytes, fileName, senderEmail, subject, job);
    }

    private Candidate executeProcessingFlow(byte[] fileBytes, String fileName, String senderEmail, String subject, Job job) {
        // Compute SHA-256 hash
        String resumeHash = HashUtils.sha256(fileBytes);

        // Check duplicate hash for same job
        if (resumeHash != null) {
            List<Resume> existingResumes = resumeRepository.findByResumeHash(resumeHash);
            boolean alreadyAppliedToThisJob = existingResumes.stream()
                    .anyMatch(r -> r.getCandidate() != null
                            && r.getCandidate().getJob() != null
                            && r.getCandidate().getJob().getId().equals(job.getId()));
            if (alreadyAppliedToThisJob) {
                log.warn("Exact duplicate resume hash detected for job {}", job.getJobTitle());
                throw new DuplicateApplicationException("Duplicate application detected with identical resume file hash for this job.");
            }
        }

        // Store resume file
        String storageKey = resumeStorageService.store(fileBytes, fileName);

        // Extract text
        String rawText = resumeParserService.extractText(fileBytes, fileName);

        // Extract candidate metadata
        CandidateInfoExtractor.ExtractedCandidateInfo extractedInfo = candidateInfoExtractor.extract(rawText, fileName, senderEmail);

        String resumeEmail = extractedInfo.getEmail();
        boolean emailDiscrepancy = false;
        if (resumeEmail != null && senderEmail != null && !senderEmail.contains("manual") && !resumeEmail.equalsIgnoreCase(senderEmail)) {
            emailDiscrepancy = true;
            log.warn("Email discrepancy flagged! Sender: '{}' vs Resume: '{}'", senderEmail, resumeEmail);
        }

        // Create Candidate Entity
        Candidate candidate = Candidate.builder()
                .job(job)
                .fullName(extractedInfo.getFullName())
                .senderEmail(senderEmail)
                .resumeEmail(resumeEmail != null ? resumeEmail : senderEmail)
                .emailDiscrepancyFlag(emailDiscrepancy)
                .phoneNumber(extractedInfo.getPhoneNumber())
                .totalExperienceYears(extractedInfo.getTotalExperienceYears())
                .currentCompany(extractedInfo.getCurrentCompany())
                .education(extractedInfo.getEducation())
                .degree(extractedInfo.getDegree())
                .location(extractedInfo.getLocation())
                .noticePeriod(extractedInfo.getNoticePeriod())
                .gender(extractedInfo.getGender() != null ? extractedInfo.getGender() : Gender.UNSPECIFIED)
                .status(CandidateStatus.PROCESSING)
                .build();

        candidate = candidateRepository.save(candidate);

        // Save Resume Entity
        Resume resume = Resume.builder()
                .candidate(candidate)
                .fileName(fileName != null ? fileName : "resume.pdf")
                .fileType(resumeParserService.detectMimeType(fileBytes, fileName))
                .storageKey(storageKey)
                .resumeHash(resumeHash)
                .fileSizeBytes((long) fileBytes.length)
                .extractedText(rawText)
                .build();

        resumeRepository.save(resume);
        candidate.setResume(resume);

        // 1. Evaluate Rule-Based ATS Score
        ATSScoringService.RuleBasedEvaluationResult ruleResult = atsScoringService.evaluate(rawText, extractedInfo, job);

        // Save candidate normalized skills
        if (ruleResult.getExtractedNormalizedSkills() != null) {
            for (String normSkill : ruleResult.getExtractedNormalizedSkills()) {
                CandidateSkill skill = CandidateSkill.builder()
                        .candidate(candidate)
                        .skillName(normSkill)
                        .normalizedSkill(normSkill)
                        .category("SKILL")
                        .isMatchedRequirement(ruleResult.getMatchedSkills().contains(normSkill))
                        .build();
                candidate.addSkill(skill);
            }
        }

        // 2. Evaluate AI Analysis (if enabled)
        Double aiScore = null;
        String aiSummary = null;
        boolean isAiAssisted = false;

        if (aiResumeAnalyzerFactory.isAiEnabled()) {
            try {
                AIResumeAnalyzer analyzer = aiResumeAnalyzerFactory.getAnalyzer();
                if (analyzer != null) {
                    AIResumeAnalyzer.ResumeAnalysis aiAnalysis = analyzer.analyzeResume(rawText, job.getDescription());
                    if (aiAnalysis != null) {
                        aiScore = aiAnalysis.getJobMatchScore();
                        aiSummary = aiAnalysis.getSummary();
                        isAiAssisted = true;
                    }
                }
            } catch (Exception e) {
                log.warn("AI resume analysis encountered an issue (non-fatal): {}", e.getMessage());
            }
        }

        // 3. Combine scores (Hybrid ATS)
        double ruleScore = ruleResult.getTotalScore();
        double finalScore;

        if (isAiAssisted && aiScore != null) {
            double ruleWeight = atsProperties.getScoringWeights().getRuleBasedWeight();
            double aiWeight = atsProperties.getScoringWeights().getAiWeight();
            finalScore = (ruleScore * ruleWeight) + (aiScore * aiWeight);
            finalScore = Math.round(finalScore * 10.0) / 10.0;
        } else {
            finalScore = ruleScore;
        }

        // Determine final screening status
        CandidateStatus decision;
        if (finalScore >= atsProperties.getShortlistThreshold()) {
            decision = CandidateStatus.SHORTLISTED;
        } else if (finalScore >= atsProperties.getReviewThreshold()) {
            decision = CandidateStatus.REVIEW_REQUIRED;
        } else {
            decision = CandidateStatus.REJECTED;
        }

        candidate.setStatus(decision);
        candidateRepository.save(candidate);

        // Save ATS Result Entity
        AtsResult atsResultEntity = AtsResult.builder()
                .candidate(candidate)
                .ruleScore(ruleScore)
                .aiScore(aiScore)
                .finalScore(finalScore)
                .decision(decision)
                .matchedSkills(ruleResult.getMatchedSkills())
                .missingSkills(ruleResult.getMissingSkills())
                .scoringBreakdownJson(ruleResult.getBreakdownJson())
                .aiSummary(aiSummary)
                .isAiAssisted(isAiAssisted)
                .build();

        atsResultRepository.save(atsResultEntity);
        candidate.setAtsResult(atsResultEntity);

        // 4. Trigger Stage 1 Screening Email
        triggerScreeningEmail(candidate, decision);

        return candidate;
    }

    private void triggerScreeningEmail(Candidate candidate, CandidateStatus decision) {
        try {
            if (decision == CandidateStatus.SHORTLISTED) {
                emailService.sendShortlistEmail(candidate);
            } else if (decision == CandidateStatus.REVIEW_REQUIRED) {
                emailService.sendReviewRequiredEmail(candidate);
            } else if (decision == CandidateStatus.REJECTED) {
                emailService.sendRejectionEmail(candidate);
            }
        } catch (Exception e) {
            log.error("Failed to trigger automated screening email for candidate {}: {}", candidate.getId(), e.getMessage());
        }
    }

    private GmailClient.AttachmentMetadata findResumeAttachment(List<GmailClient.AttachmentMetadata> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        for (GmailClient.AttachmentMetadata att : attachments) {
            String name = att.getFileName().toLowerCase();
            if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")) {
                return att;
            }
        }
        return attachments.getFirst();
    }
}
