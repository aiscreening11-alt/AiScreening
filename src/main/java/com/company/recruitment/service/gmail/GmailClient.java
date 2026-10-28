package com.company.recruitment.service.gmail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface GmailClient {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AttachmentMetadata {
        private String attachmentId;
        private String fileName;
        private String mimeType;
        private long size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class InboundEmailDetails {
        private String messageId;
        private String threadId;
        private String senderEmail;
        private String senderName;
        private String subject;
        private String bodyText;
        private String snippet;
        private LocalDateTime receivedAt;
        @Builder.Default
        private List<AttachmentMetadata> attachments = new ArrayList<>();
    }

    boolean isConnected();
    List<String> listUnreadMessageIds(int maxResults);
    InboundEmailDetails getMessageDetails(String messageId);
    byte[] downloadAttachment(String messageId, String attachmentId);
    void markAsReadAndProcessed(String messageId);
    void sendHtmlEmail(String recipientEmail, String subject, String htmlBody);
}
