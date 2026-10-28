package com.company.recruitment.service.gmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailPollingService {

    private final GmailClient gmailClient;
    private final EmailIngestionPipeline emailIngestionPipeline;

    @Value("${gmail.enabled:false}")
    private boolean gmailEnabled;

    @Scheduled(fixedDelayString = "${gmail.polling.interval:60000}")
    public void pollMailbox() {
        if (!gmailEnabled) {
            log.trace("Gmail polling is disabled in application configuration (gmail.enabled=false).");
            return;
        }

        if (!gmailClient.isConnected()) {
            log.trace("Gmail client is not authenticated yet. Polling deferred until OAuth setup.");
            return;
        }

        log.info("Starting scheduled polling sweep of recruitment Gmail account...");
        try {
            List<String> unreadMsgIds = gmailClient.listUnreadMessageIds(20);
            if (unreadMsgIds.isEmpty()) {
                log.debug("No new unread candidate application emails found.");
                return;
            }

            log.info("Found {} unread emails to inspect.", unreadMsgIds.size());
            for (String msgId : unreadMsgIds) {
                try {
                    GmailClient.InboundEmailDetails details = gmailClient.getMessageDetails(msgId);
                    log.info("Inspecting email ID: {} Subject: '{}' From: '{}'", msgId, details.getSubject(), details.getSenderEmail());
                    emailIngestionPipeline.processGmailMessage(details);
                } catch (Exception e) {
                    log.error("Error fetching message details for {}: {}", msgId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled Gmail polling sweep: {}", e.getMessage(), e);
        }
    }
}
