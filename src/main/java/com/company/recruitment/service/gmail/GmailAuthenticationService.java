package com.company.recruitment.service.gmail;

import com.company.recruitment.exception.GmailIntegrationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class GmailAuthenticationService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
    );
    private static final String USER_ID = "me";

    @Value("${gmail.client-id:dummy-client-id}")
    private String clientId;

    @Value("${gmail.client-secret:dummy-client-secret}")
    private String clientSecret;

    @Value("${gmail.redirect-uri:http://localhost:8080/api/v1/gmail/oauth2callback}")
    private String redirectUri;

    @Value("${gmail.tokens-directory-path:./tokens}")
    private String tokensDirectoryPath;

    private GoogleAuthorizationCodeFlow flow;
    private NetHttpTransport httpTransport;

    @PostConstruct
    public void init() {
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            File tokensDir = new File(tokensDirectoryPath);
            if (!tokensDir.exists()) {
                tokensDir.mkdirs();
            }
            if (clientId != null && !clientId.contains("dummy") && clientSecret != null && !clientSecret.contains("dummy")) {
                this.flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                        .setAccessType("offline")
                        .build();
                log.info("Gmail OAuth flow initialized for client ID: {}", clientId.substring(0, Math.min(clientId.length(), 10)) + "...");
            } else {
                log.info("Gmail OAuth client ID/secret not configured with real values. Mock/dev mode available.");
            }
        } catch (Exception e) {
            log.warn("Gmail OAuth flow could not be fully initialized on startup: {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.contains("dummy") && clientSecret != null && !clientSecret.contains("dummy");
    }

    public boolean isAuthorized() {
        try {
            if (flow == null) return false;
            return flow.loadCredential(USER_ID) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public String getEffectiveRedirectUri() {
        if (redirectUri != null && !redirectUri.contains("localhost")) {
            return redirectUri;
        }
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl != null && !renderUrl.isBlank()) {
            return renderUrl.replaceAll("/+$", "") + "/api/v1/gmail/oauth2callback";
        }
        String appBase = System.getenv("APP_BASE_URL");
        if (appBase != null && !appBase.isBlank()) {
            return appBase.replaceAll("/+$", "") + "/api/v1/gmail/oauth2callback";
        }
        return redirectUri;
    }

    public String buildAuthorizationUrl() {
        if (flow == null) {
            throw new GmailIntegrationException("Gmail OAuth is not configured with valid Client ID / Secret in application.yml or environment");
        }
        return flow.newAuthorizationUrl().setRedirectUri(getEffectiveRedirectUri()).build();
    }

    public void exchangeCodeForTokens(String code) {
        if (flow == null) {
            throw new GmailIntegrationException("Gmail OAuth flow is not initialized");
        }
        try {
            GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(getEffectiveRedirectUri()).execute();
            flow.createAndStoreCredential(response, USER_ID);
            log.info("Successfully exchanged authorization code and stored Gmail OAuth credentials.");
        } catch (IOException e) {
            throw new GmailIntegrationException("Failed to exchange authorization code for Gmail tokens", e);
        }
    }

    public void purgeStoredCredential() {
        try {
            if (flow != null && flow.getCredentialDataStore() != null) {
                flow.getCredentialDataStore().delete(USER_ID);
                log.info("Purged expired/revoked Gmail OAuth credential.");
            }
        } catch (Exception e) {
            log.warn("Could not purge credential: {}", e.getMessage());
        }
    }

    public Gmail getGmailService() {
        if (!isAuthorized()) {
            return null;
        }
        try {
            return new Gmail.Builder(httpTransport, JSON_FACTORY, flow.loadCredential(USER_ID))
                    .setApplicationName("RecruitmentATSPlatform")
                    .build();
        } catch (IOException e) {
            throw new GmailIntegrationException("Failed to instantiate Gmail API client", e);
        }
    }
}
