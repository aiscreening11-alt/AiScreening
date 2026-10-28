package com.company.recruitment.dto.response;

import com.company.recruitment.entity.enums.EmailStatus;
import com.company.recruitment.entity.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerNotificationDto {
    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private String recipientEmail;
    private EmailType emailType;
    private String subject;
    private String bodyContent;
    private EmailStatus sentStatus;
    private String errorMessage;
    private LocalDateTime sentAt;
    private UUID interviewId;
    private String meetingLink;
}
