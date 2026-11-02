package com.company.recruitment.controller;

import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.enums.Gender;
import com.company.recruitment.mapper.CandidateMapper;
import com.company.recruitment.repository.CandidateRepository;
import com.company.recruitment.service.gmail.EmailIngestionPipeline;
import com.company.recruitment.service.gmail.GmailAuthenticationService;
import com.company.recruitment.service.gmail.GmailPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import com.company.recruitment.service.email.ResendEmailService;
import com.company.recruitment.service.gmail.GmailClient;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class GmailAdminController {

    private final GmailAuthenticationService authService;
    private final GmailPollingService pollingService;
    private final EmailIngestionPipeline emailIngestionPipeline;
    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final GmailClient gmailClient;
    private final ResendEmailService resendEmailService;

    @GetMapping("/gmail/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGmailStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("configured", authService.isConfigured());
        status.put("authorized", authService.isAuthorized());
        status.put("hasRefreshToken", authService.hasRefreshToken());
        status.put("refreshToken", authService.getRefreshToken());
        status.put("recruitmentEmail", authService.getRecruitmentEmail());
        status.put("clientId", authService.getClientId());
        status.put("effectiveRedirectUri", authService.getEffectiveRedirectUri());
        status.put("resendConfigured", resendEmailService != null && resendEmailService.isConfigured());
        status.put("resendSender", resendEmailService != null ? resendEmailService.getFromEmail() : "");
        return ResponseEntity.ok(ApiResponse.success(status, "Email & Gmail integration status retrieved"));
    }

    @PostMapping("/gmail/send-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(@RequestParam String toEmail) {
        if (!authService.isAuthorized() && (resendEmailService == null || !resendEmailService.isConfigured())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(
                    "Gmail is not authorized. Visit /api/v1/gmail/authorize in your browser to sign in and authorize email dispatch."
            ));
        }

        String testSubject = "Sanvii ATS Notification Test - Delivery Verification";
        String testBody = "<h3>Sanvii ATS Notification Test</h3><p>This email verifies automated email delivery from the Sanvii ATS Platform to <strong>" + toEmail + "</strong> via Google Gmail API.</p>";
        
        try {
            gmailClient.sendHtmlEmail(toEmail, testSubject, testBody);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of(
                            "recipient", toEmail,
                            "sent", true,
                            "message", "Test email successfully sent to " + toEmail
                    ),
                    "Email sent successfully"
            ));
        } catch (Exception ex) {
            log.error("Failed to send test email to {}: {}", toEmail, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Email delivery failed: " + ex.getMessage()
            ));
        }
    }

    @GetMapping("/gmail/authorize")
    public void redirectToGoogleAuth(HttpServletResponse response) throws IOException {
        String url = authService.buildAuthorizationUrl();
        response.sendRedirect(url);
    }

    @GetMapping("/gmail/auth-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl() {
        String url = authService.buildAuthorizationUrl();
        return ResponseEntity.ok(ApiResponse.success(Map.of("authorizationUrl", url), "Gmail OAuth authorization URL generated"));
    }

    @GetMapping("/gmail/oauth2callback")
    public void oauth2Callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {
        if (error != null && !error.isBlank()) {
            log.error("Google OAuth callback returned error: {}", error);
            response.sendRedirect("/?gmail_error=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        if (code != null && !code.isBlank()) {
            log.info("Received Google OAuth callback code. Exchanging for tokens...");
            authService.exchangeCodeForTokens(code);
            response.sendRedirect("/?gmail_connected=true");
            return;
        }
        response.sendRedirect("/?gmail_error=No+authorization+code+received");
    }

    @PostMapping("/gmail/save-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveManualRefreshToken(
            @RequestParam("refreshToken") String refreshToken) {
        authService.saveManualRefreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "saved", true,
                        "authorized", authService.isAuthorized(),
                        "hasRefreshToken", authService.hasRefreshToken()
                ),
                "Refresh token registered successfully. Gmail OAuth is now connected."
        ));
    }

    @PostMapping("/gmail/disconnect")
    public ResponseEntity<ApiResponse<String>> disconnectGmail() {
        authService.purgeStoredCredential();
        return ResponseEntity.ok(ApiResponse.success("Gmail OAuth credential purged. Integration disconnected."));
    }

    @PostMapping("/gmail/poll-now")
    public ResponseEntity<ApiResponse<String>> pollNow() {
        pollingService.pollMailbox();
        return ResponseEntity.ok(ApiResponse.success("On-demand Gmail mailbox polling sweep triggered."));
    }

    @PostMapping(value = "/candidates/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateResponseDto>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobId", required = false) UUID jobId,
            @RequestParam(value = "senderEmail", required = false) String senderEmail) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null, "Uploaded file is empty"));
        }

        Candidate candidate = emailIngestionPipeline.processManualResumeUpload(
                file.getBytes(),
                file.getOriginalFilename(),
                jobId,
                senderEmail != null ? senderEmail : "manual@upload.local"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(candidateMapper.toResponseDto(candidate), "Resume uploaded and screened successfully"));
    }

    @PostMapping(value = "/gmail/simulate-inbound", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateResponseDto>> simulateInboundEmail(
            @RequestParam("senderEmail") String senderEmail,
            @RequestParam(value = "senderName", required = false) String senderName,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "body", required = false) String body,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "jobId", required = false) String jobId) throws IOException {

        UUID parsedJobId = null;
        if (jobId != null && !jobId.isBlank() && !jobId.contains("{{")) {
            try {
                parsedJobId = UUID.fromString(jobId.trim());
            } catch (IllegalArgumentException ignored) {}
        }

        Gender explicitGender = null;
        if (gender != null && !gender.isBlank() && !gender.equalsIgnoreCase("AUTO")) {
            try {
                explicitGender = Gender.valueOf(gender.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        boolean hasAttachment = file != null && !file.isEmpty();
        java.util.List<String> missingItems = emailIngestionPipeline.validateInboundEmail(subject, body, hasAttachment);

        if (!missingItems.isEmpty()) {
            Candidate rejectedCandidate = emailIngestionPipeline.handleIncompleteApplication(
                    senderEmail,
                    senderName,
                    subject,
                    missingItems,
                    parsedJobId
            );
            if (explicitGender != null) {
                rejectedCandidate.setGender(explicitGender);
                rejectedCandidate = candidateRepository.save(rejectedCandidate);
            }
            return ResponseEntity.ok(ApiResponse.success(
                    candidateMapper.toResponseDto(rejectedCandidate),
                    "Application rejected: Incomplete submission. Rejection notice email shot to candidate."
            ));
        }

        Candidate candidate = emailIngestionPipeline.processManualResumeUpload(
                file.getBytes(),
                file.getOriginalFilename(),
                parsedJobId,
                senderEmail
        );
        if (explicitGender != null) {
            candidate.setGender(explicitGender);
            candidate = candidateRepository.save(candidate);
        }
        return ResponseEntity.ok(ApiResponse.success(
                candidateMapper.toResponseDto(candidate),
                "Application processed and screened successfully."
        ));
    }
}
