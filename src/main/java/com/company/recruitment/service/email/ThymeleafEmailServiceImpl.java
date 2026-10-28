package com.company.recruitment.service.email;

import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.EmailCommunication;
import com.company.recruitment.entity.enums.EmailStatus;
import com.company.recruitment.entity.enums.EmailType;
import com.company.recruitment.repository.EmailCommunicationRepository;
import com.company.recruitment.service.gmail.GmailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThymeleafEmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;
    private final GmailClient gmailClient;
    private final EmailCommunicationRepository emailCommunicationRepository;

    @Override
    @Transactional
    public void sendShortlistEmail(Candidate candidate) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Application Update – You Have Been Shortlisted";
        Context context = createContext(candidate, jobTitle);
        String htmlBody = templateEngine.process("candidate-shortlisted", context);

        dispatchEmail(candidate, EmailType.SHORTLISTED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendRejectionEmail(Candidate candidate) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Application Update – " + jobTitle;
        Context context = createContext(candidate, jobTitle);
        String htmlBody = templateEngine.process("candidate-rejected", context);

        dispatchEmail(candidate, EmailType.REJECTED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendReviewRequiredEmail(Candidate candidate) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Application Received – Under Recruiter Review";
        Context context = createContext(candidate, jobTitle);
        String htmlBody = templateEngine.process("candidate-review-required", context);

        dispatchEmail(candidate, EmailType.REVIEW_REQUIRED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendSelectionEmail(Candidate candidate) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Congratulations – You Have Been Selected";
        Context context = createContext(candidate, jobTitle);
        String htmlBody = templateEngine.process("candidate-selected", context);

        dispatchEmail(candidate, EmailType.INTERVIEW_SELECTED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendNotSelectedEmail(Candidate candidate) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Interview Update – " + jobTitle;
        Context context = createContext(candidate, jobTitle);
        String htmlBody = templateEngine.process("candidate-not-selected", context);

        dispatchEmail(candidate, EmailType.INTERVIEW_NOT_SELECTED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendIncompleteApplicationRejectionEmail(Candidate candidate, java.util.List<String> missingItems) {
        String jobTitle = getJobTitle(candidate);
        String subject = "Application Update – Application Rejected (Incomplete Submission)";
        Context context = createContext(candidate, jobTitle);
        context.setVariable("missingItems", missingItems);
        String htmlBody = templateEngine.process("candidate-incomplete-rejected", context);

        dispatchEmail(candidate, EmailType.APPLICATION_INCOMPLETE_REJECTED, subject, htmlBody);
    }

    @Override
    @Transactional
    public void sendInterviewScheduleEmails(com.company.recruitment.entity.Interview interview) {
        Candidate candidate = interview.getCandidate();
        String jobTitle = getJobTitle(candidate);
        String roundName = interview.getRound() != null ? interview.getRound().getDisplayName() : "Technical Interview";

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' hh:mm a");
        String formattedDate = interview.getInterviewDate() != null ? interview.getInterviewDate().format(formatter) : "TBD";

        // 1. Dispatch Candidate Email
        String candidateSubject = "Interview Scheduled: " + roundName + " – " + jobTitle + " | Sanvii Techmet";
        Context candidateContext = createContext(candidate, jobTitle);
        candidateContext.setVariable("roundName", roundName);
        candidateContext.setVariable("interviewDate", formattedDate);
        candidateContext.setVariable("durationMinutes", interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 45);
        candidateContext.setVariable("interviewerName", interview.getInterviewerName() != null ? interview.getInterviewerName() : "Hiring Panel");
        candidateContext.setVariable("meetingLink", interview.getMeetingLink() != null ? interview.getMeetingLink() : "https://meet.google.com");

        String candidateHtml = templateEngine.process("candidate-interview-scheduled", candidateContext);
        dispatchEmail(candidate, EmailType.INTERVIEW_SCHEDULED_CANDIDATE, candidateSubject, candidateHtml);

        // 2. Dispatch Interviewer Email (if email address provided)
        if (interview.getInterviewerEmail() != null && !interview.getInterviewerEmail().isBlank()) {
            String interviewerSubject = "Interview Assignment: " + (candidate.getFullName() != null ? candidate.getFullName() : "Candidate") + " (" + roundName + ") | Sanvii Techmet";
            Context interviewerContext = new Context();
            interviewerContext.setVariable("interviewerName", interview.getInterviewerName() != null ? interview.getInterviewerName() : "Interviewer");
            interviewerContext.setVariable("candidateName", candidate.getFullName() != null ? candidate.getFullName() : "Candidate");
            interviewerContext.setVariable("candidateEmail", getRecipientEmail(candidate));
            interviewerContext.setVariable("jobTitle", jobTitle);
            interviewerContext.setVariable("roundName", roundName);
            interviewerContext.setVariable("interviewDate", formattedDate);
            interviewerContext.setVariable("durationMinutes", interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 45);
            interviewerContext.setVariable("meetingLink", interview.getMeetingLink() != null ? interview.getMeetingLink() : "https://meet.google.com");
            interviewerContext.setVariable("notes", interview.getNotes());
            interviewerContext.setVariable("atsScore", candidate.getAtsResult() != null ? candidate.getAtsResult().getFinalScore() : null);

            String portalToken = interview.getAccessToken() != null ? interview.getAccessToken() : interview.getId().toString();
            String portalLink = "http://localhost:8080/interviewer.html?token=" + portalToken;
            interviewerContext.setVariable("portalLink", portalLink);
            interviewerContext.setVariable("interviewId", interview.getId() != null ? interview.getId().toString() : "");

            String interviewerHtml = templateEngine.process("interviewer-interview-scheduled", interviewerContext);
            dispatchInterviewerEmail(candidate, interview.getInterviewerEmail(), EmailType.INTERVIEW_SCHEDULED_INTERVIEWER, interviewerSubject, interviewerHtml);
        }
    }

    private void dispatchInterviewerEmail(Candidate candidate, String recipient, EmailType type, String subject, String htmlBody) {
        log.info("Sending {} email to interviewer ({}) for candidate id={}", type, recipient, candidate.getId());

        EmailCommunication comm = EmailCommunication.builder()
                .candidate(candidate)
                .recipientEmail(recipient)
                .emailType(type)
                .subject(subject)
                .bodyContent(htmlBody)
                .sentStatus(EmailStatus.PENDING)
                .build();

        try {
            gmailClient.sendHtmlEmail(recipient, subject, htmlBody);
            comm.setSentStatus(EmailStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send {} email to interviewer {}: {}", type, recipient, e.getMessage());
            comm.setSentStatus(EmailStatus.FAILED);
            comm.setErrorMessage(e.getMessage());
        }

        candidate.addEmailCommunication(comm);
        emailCommunicationRepository.save(comm);
    }

    private void dispatchEmail(Candidate candidate, EmailType type, String subject, String htmlBody) {
        String recipient = getRecipientEmail(candidate);
        log.info("Sending {} email to candidate id={} ({})", type, candidate.getId(), recipient);

        EmailCommunication comm = EmailCommunication.builder()
                .candidate(candidate)
                .recipientEmail(recipient)
                .emailType(type)
                .subject(subject)
                .bodyContent(htmlBody)
                .sentStatus(EmailStatus.PENDING)
                .build();

        try {
            gmailClient.sendHtmlEmail(recipient, subject, htmlBody);
            comm.setSentStatus(EmailStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send {} email to {}: {}", type, recipient, e.getMessage());
            comm.setSentStatus(EmailStatus.FAILED);
            comm.setErrorMessage(e.getMessage());
        }

        candidate.addEmailCommunication(comm);
        emailCommunicationRepository.save(comm);
    }

    private Context createContext(Candidate candidate, String jobTitle) {
        Context context = new Context();
        context.setVariable("candidateName", candidate.getFullName() != null ? candidate.getFullName() : "Candidate");
        context.setVariable("jobTitle", jobTitle);
        return context;
    }

    private String getRecipientEmail(Candidate candidate) {
        if (candidate.getResumeEmail() != null && !candidate.getResumeEmail().isBlank()) {
            return candidate.getResumeEmail();
        }
        return candidate.getSenderEmail();
    }

    private String getJobTitle(Candidate candidate) {
        return (candidate.getJob() != null && candidate.getJob().getJobTitle() != null)
                ? candidate.getJob().getJobTitle()
                : "Open Position";
    }
}
