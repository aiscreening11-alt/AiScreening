package com.company.recruitment.mapper;

import com.company.recruitment.dto.response.EmailCommunicationDto;
import com.company.recruitment.entity.EmailCommunication;
import org.springframework.stereotype.Component;

@Component
public class EmailCommunicationMapper {

    public EmailCommunicationDto toDto(EmailCommunication entity) {
        if (entity == null) return null;
        return EmailCommunicationDto.builder()
                .id(entity.getId())
                .recipientEmail(entity.getRecipientEmail())
                .emailType(entity.getEmailType())
                .subject(entity.getSubject())
                .bodyContent(entity.getBodyContent())
                .sentStatus(entity.getSentStatus())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .build();
    }
}
