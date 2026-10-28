package com.company.recruitment.service.gmail;

import com.company.recruitment.exception.GmailIntegrationException;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailClientImpl implements GmailClient {

    private final GmailAuthenticationService authService;

    @Value("${gmail.recruitment-email:recruitment@company.com}")
    private String recruitmentEmail;

    private static final String USER_ID = "me";

    @Override
    public boolean isConnected() {
        return authService.isAuthorized();
    }

    @Override
    public List<String> listUnreadMessageIds(int maxResults) {
        Gmail service = authService.getGmailService();
        if (service == null) {
            log.debug("Gmail client not authenticated or connected. Skipping message retrieval.");
            return Collections.emptyList();
        }

        try {
            // Search for unread emails with attachments
            ListMessagesResponse response = service.users().messages().list(USER_ID)
                    .setQ("is:unread has:attachment")
                    .setMaxResults((long) maxResults)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }

            return messages.stream().map(Message::getId).toList();
        } catch (IOException e) {
            log.error("Failed to list unread messages from Gmail: {}", e.getMessage());
            throw new GmailIntegrationException("Error querying Gmail mailbox", e);
        }
    }

    @Override
    public InboundEmailDetails getMessageDetails(String messageId) {
        Gmail service = authService.getGmailService();
        if (service == null) {
            throw new GmailIntegrationException("Gmail service not authenticated");
        }

        try {
            Message message = service.users().messages().get(USER_ID, messageId)
                    .setFormat("full")
                    .execute();

            String sender = "";
            String subject = "";
            LocalDateTime receivedAt = LocalDateTime.now();

            if (message.getInternalDate() != null) {
                receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getInternalDate()), ZoneId.systemDefault());
            }

            if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                    if ("From".equalsIgnoreCase(header.getName())) {
                        sender = header.getValue();
                    } else if ("Subject".equalsIgnoreCase(header.getName())) {
                        subject = header.getValue();
                    }
                }
            }

            String senderEmail = extractEmailAddress(sender);
            String senderName = extractSenderName(sender);

            List<AttachmentMetadata> attachments = new ArrayList<>();
            findAttachments(message.getPayload(), attachments);

            String bodyText = extractBodyText(message.getPayload());
            if (bodyText.trim().isEmpty() && message.getSnippet() != null) {
                bodyText = message.getSnippet();
            }

            return InboundEmailDetails.builder()
                    .messageId(message.getId())
                    .threadId(message.getThreadId())
                    .senderEmail(senderEmail)
                    .senderName(senderName)
                    .subject(subject)
                    .bodyText(bodyText)
                    .snippet(message.getSnippet())
                    .receivedAt(receivedAt)
                    .attachments(attachments)
                    .build();

        } catch (IOException e) {
            log.error("Failed to retrieve details for message ID {}: {}", messageId, e.getMessage());
            throw new GmailIntegrationException("Error fetching message details for " + messageId, e);
        }
    }

    @Override
    public byte[] downloadAttachment(String messageId, String attachmentId) {
        Gmail service = authService.getGmailService();
        if (service == null) {
            throw new GmailIntegrationException("Gmail service not authenticated");
        }

        try {
            MessagePartBody body = service.users().messages().attachments()
                    .get(USER_ID, messageId, attachmentId)
                    .execute();

            if (body == null || body.getData() == null) {
                throw new GmailIntegrationException("Attachment content is empty for id: " + attachmentId);
            }

            return Base64.decodeBase64(body.getData());
        } catch (IOException e) {
            log.error("Failed to download attachment {} for message {}: {}", attachmentId, messageId, e.getMessage());
            throw new GmailIntegrationException("Error downloading attachment: " + attachmentId, e);
        }
    }

    @Override
    public void markAsReadAndProcessed(String messageId) {
        Gmail service = authService.getGmailService();
        if (service == null) return;

        try {
            ModifyMessageRequest mods = new ModifyMessageRequest()
                    .setRemoveLabelIds(List.of("UNREAD"));
            service.users().messages().modify(USER_ID, messageId, mods).execute();
            log.debug("Marked Gmail message {} as read", messageId);
        } catch (IOException e) {
            log.warn("Failed to mark message {} as read: {}", messageId, e.getMessage());
        }
    }

    @Override
    public void sendHtmlEmail(String recipientEmail, String subject, String htmlBody) {
        Gmail service = authService.getGmailService();
        if (service == null) {
            log.warn("Gmail API not authorized to send email to {}. Simulating sending (Dev/Test mode).", recipientEmail);
            return;
        }

        try {
            // Determine the true sender email from the authenticated Gmail profile to satisfy Google security policy
            String senderEmail = recruitmentEmail;
            try {
                Profile profile = service.users().getProfile(USER_ID).execute();
                if (profile != null && profile.getEmailAddress() != null && !profile.getEmailAddress().isBlank()) {
                    senderEmail = profile.getEmailAddress();
                    log.info("Resolved authenticated Gmail sender address: {}", senderEmail);
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve Gmail profile email address ({}), falling back to configured: {}", ex.getMessage(), recruitmentEmail);
            }

            Session session = Session.getDefaultInstance(new Properties(), null);
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(senderEmail, "Sanvii ATS Recruitment"));
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(recipientEmail));
            email.setSubject(subject, "UTF-8");
            email.setContent(htmlBody, "text/html; charset=UTF-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] bytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes);

            Message message = new Message();
            message.setRaw(encodedEmail);
            service.users().messages().send(USER_ID, message).execute();
            log.info("Successfully sent email via Gmail API from '{}' to '{}' with subject: '{}'", senderEmail, recipientEmail, subject);

        } catch (com.google.api.client.auth.oauth2.TokenResponseException tre) {
            log.error("Google OAuth token expired or revoked when sending email to {}: {}", recipientEmail, tre.getDetails() != null ? tre.getDetails().getErrorDescription() : tre.getMessage());
            authService.purgeStoredCredential();
            throw new GmailIntegrationException("Google OAuth token expired or revoked. Please re-authorize Gmail integration at /api/v1/gmail/authorize", tre);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException gjre) {
            String details = gjre.getDetails() != null ? gjre.getDetails().getMessage() : gjre.getMessage();
            log.error("Google Gmail API rejected email to {} [HTTP {}]: {}", recipientEmail, gjre.getStatusCode(), details, gjre);
            throw new GmailIntegrationException("Gmail API error [" + gjre.getStatusCode() + "]: " + details, gjre);
        } catch (MessagingException | IOException e) {
            log.error("Failed to send email to {}: {}", recipientEmail, e.getMessage(), e);
            throw new GmailIntegrationException("Error sending email to " + recipientEmail + ": " + e.getMessage(), e);
        }
    }

    private void findAttachments(MessagePart part, List<AttachmentMetadata> result) {
        if (part == null) return;

        if (part.getFilename() != null && !part.getFilename().trim().isEmpty() && part.getBody() != null) {
            if (part.getBody().getAttachmentId() != null) {
                result.add(AttachmentMetadata.builder()
                        .attachmentId(part.getBody().getAttachmentId())
                        .fileName(part.getFilename())
                        .mimeType(part.getMimeType())
                        .size(part.getBody().getSize() != null ? part.getBody().getSize() : 0)
                        .build());
            }
        }

        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                findAttachments(child, result);
            }
        }
    }

    private String extractEmailAddress(String fromHeader) {
        if (fromHeader == null) return "";
        int start = fromHeader.indexOf('<');
        int end = fromHeader.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return fromHeader.substring(start + 1, end).trim().toLowerCase();
        }
        return fromHeader.trim().toLowerCase();
    }

    private String extractSenderName(String fromHeader) {
        if (fromHeader == null) return "";
        int start = fromHeader.indexOf('<');
        if (start > 0) {
            return fromHeader.substring(0, start).replace("\"", "").trim();
        }
        return "";
    }

    private String extractBodyText(MessagePart part) {
        if (part == null) return "";
        if ("text/plain".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
            byte[] bytes = Base64.decodeBase64(part.getBody().getData());
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                String childText = extractBodyText(child);
                if (!childText.trim().isEmpty()) {
                    return childText;
                }
            }
        }
        return "";
    }
}
