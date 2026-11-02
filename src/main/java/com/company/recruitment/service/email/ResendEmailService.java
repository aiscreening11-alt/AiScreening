package com.company.recruitment.service.email;

import com.company.recruitment.exception.GmailIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resend REST API Email Service.
 * Transmits emails over HTTPS (port 443), bypassing cloud hosting outbound SMTP port blocks (e.g., Render, AWS, Heroku).
 */
@Slf4j
@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:Sanvii ATS <onboarding@resend.dev>}")
    private String fromEmail;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendEmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.trim().equalsIgnoreCase("dummy");
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getMaskedApiKey() {
        if (!isConfigured()) return "NOT_CONFIGURED";
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) return "re_****";
        return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * Dispatches an HTML email via Resend HTTPS REST API.
     *
     * @param recipientEmail Recipient email address
     * @param subject        Email subject
     * @param htmlBody       HTML content of the email
     * @param replyTo        Optional reply-to address (e.g. recruitment Gmail)
     * @return Resend message ID
     */
    public String sendEmail(String recipientEmail, String subject, String htmlBody, String replyTo) {
        if (!isConfigured()) {
            throw new GmailIntegrationException("Resend API key is not configured. Please set RESEND_API_KEY environment variable.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromEmail != null && !fromEmail.isBlank() ? fromEmail : "Sanvii ATS <onboarding@resend.dev>");
            payload.put("to", List.of(recipientEmail.trim()));
            payload.put("subject", subject);
            payload.put("html", htmlBody);

            if (replyTo != null && !replyTo.isBlank()) {
                payload.put("reply_to", replyTo.trim());
            }

            String requestJson = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            log.info(">>> [RESEND API] Dispatching HTTPS email (port 443) to '{}' via {} (From='{}', Reply-To='{}')...",
                    recipientEmail, RESEND_API_URL, fromEmail, replyTo);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                String emailId = "";
                try {
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("id")) {
                        emailId = node.get("id").asText();
                    }
                } catch (Exception parseEx) {
                    log.debug("Could not parse Resend JSON response id: {}", parseEx.getMessage());
                }
                log.info(">>> [RESEND API - SUCCESS] Email successfully dispatched to '{}' (Resend ID: {})", recipientEmail, emailId);
                return emailId;
            } else {
                log.error(">>> [RESEND API - ERROR] Resend returned HTTP {}: {}", statusCode, responseBody);
                throw new GmailIntegrationException("Resend API returned error (HTTP " + statusCode + "): " + responseBody);
            }
        } catch (IOException | InterruptedException e) {
            log.error(">>> [RESEND API - EXCEPTION] Failed to dispatch email via Resend API: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GmailIntegrationException("Network error while connecting to Resend API: " + e.getMessage(), e);
        }
    }
}
