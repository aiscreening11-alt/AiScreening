package com.company.recruitment.service.email;

import com.company.recruitment.entity.Candidate;

public interface EmailService {
    void sendShortlistEmail(Candidate candidate);
    void sendRejectionEmail(Candidate candidate);
    void sendReviewRequiredEmail(Candidate candidate);
    void sendSelectionEmail(Candidate candidate);
    void sendNotSelectedEmail(Candidate candidate);
    void sendInterviewScheduleEmails(com.company.recruitment.entity.Interview interview);
    void sendIncompleteApplicationRejectionEmail(Candidate candidate, java.util.List<String> missingItems);
}
