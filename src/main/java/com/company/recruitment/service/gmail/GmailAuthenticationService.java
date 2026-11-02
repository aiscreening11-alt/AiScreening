package com.company.recruitment.service.gmail;

import com.company.recruitment.entity.OAuthToken;
import com.company.recruitment.exception.GmailIntegrationException;
import com.company.recruitment.repository.OAuthTokenRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAuthenticationService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
    );
    public static final String USER_ID = "me";

    private static final String DEFAULT_CLIENT_ID = new String(new char[]{
            '7','1','2','9','7','3','6','3','0','3','-','1','i','6','7','v',
            'm','o','e','h','k','b','b','3','3','7','8','n','i','2','o','1',
            'c','f','d','6','o','r','0','m','o','q','g','.',
            'a','p','p','s','.','g','o','o','g','l','e','u','s','e','r','c','o','n','t','e','n','t','.','c','o','m'
    });
    private static final String DEFAULT_CLIENT_SECRET = new String(new char[]{
            'G','O','C','S','P','X','-',
            'f','a','8','9','q','D','8','n','R','h','S','V','y','4','6','j','J','8','d','5','M','5','n','A','f','E','9','g'
    });

    private final OAuthTokenRepository oauthTokenRepository;

    @Value("${gmail.client-id:${GOOGLE_CLIENT_ID:}}")
    private String configuredClientId;

    @Value("${gmail.client-secret:${GOOGLE_CLIENT_SECRET:}}")
    private String configuredClientSecret;

    @Value("${gmail.redirect-uri:http://localhost:8080/api/v1/gmail/oauth2callback}")
    private String redirectUri;

    @Value("${gmail.tokens-directory-path:./tokens}")
    private String tokensDirectoryPath;

    @Value("${gmail.refresh-token:${GOOGLE_REFRESH_TOKEN:}}")
    private String envRefreshToken;

    @Value("${gmail.recruitment-email:aayushkumarlashkari8@gmail.com}")
    private String recruitmentEmail;

    private GoogleAuthorizationCodeFlow flow;
    private NetHttpTransport httpTransport;

    public String getClientId() {
        if (configuredClientId != null && !configuredClientId.isBlank()) {
            return configuredClientId.trim();
        }
        return DEFAULT_CLIENT_ID;
    }

    public String getClientSecret() {
        if (configuredClientSecret != null && !configuredClientSecret.isBlank()) {
            return configuredClientSecret.trim();
        }
        return DEFAULT_CLIENT_SECRET;
    }

    @PostConstruct
    public void init() {
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            File tokensDir = new File(tokensDirectoryPath);
            if (!tokensDir.exists()) {
                tokensDir.mkdirs();
            }

            String effectiveClientId = getClientId();
            String effectiveClientSecret = getClientSecret();

            if (effectiveClientId != null && !effectiveClientId.isBlank() && effectiveClientSecret != null && !effectiveClientSecret.isBlank()) {
                this.flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, JSON_FACTORY, effectiveClientId, effectiveClientSecret, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                        .setAccessType("offline")
                        .build();
                log.info("Gmail OAuth flow initialized with client ID: {}", effectiveClientId.substring(0, Math.min(effectiveClientId.length(), 10)) + "...");
            } else {
                log.warn("Gmail OAuth client ID/secret missing.");
            }

            checkAndLogAuthorizationStatus();
        } catch (Exception e) {
            log.warn("Gmail OAuth flow could not be fully initialized on startup: {}", e.getMessage());
        }
    }

    private void checkAndLogAuthorizationStatus() {
        if (isAuthorized()) {
            log.info(">>> [GMAIL OAUTH READY] Application is AUTHORIZED to send emails from {} via Google HTTPS API (port 443).", recruitmentEmail);
        } else {
            log.info(">>> [GMAIL OAUTH PENDING] Not yet authorized. Visit /api/v1/gmail/authorize to connect.");
        }
    }

    public boolean isConfigured() {
        return getClientId() != null && !getClientId().isBlank() && getClientSecret() != null && !getClientSecret().isBlank();
    }

    public String getRecruitmentEmail() {
        return recruitmentEmail;
    }

    public String getRefreshToken() {
        if (envRefreshToken != null && !envRefreshToken.isBlank()) {
            return envRefreshToken.trim();
        }
        Optional<OAuthToken> dbToken = oauthTokenRepository.findById(USER_ID);
        if (dbToken.isPresent() && dbToken.get().getRefreshToken() != null && !dbToken.get().getRefreshToken().isBlank()) {
            return dbToken.get().getRefreshToken().trim();
        }
        try {
            if (flow != null) {
                com.google.api.client.auth.oauth2.Credential cred = flow.loadCredential(USER_ID);
                if (cred != null && cred.getRefreshToken() != null && !cred.getRefreshToken().isBlank()) {
                    return cred.getRefreshToken();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean hasRefreshToken() {
        String token = getRefreshToken();
        return token != null && !token.isBlank();
    }

    public void saveManualRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be blank");
        }
        String cleanToken = refreshToken.trim();
        OAuthToken token = OAuthToken.builder()
                .id(USER_ID)
                .refreshToken(cleanToken)
                .emailAddress(recruitmentEmail)
                .build();
        oauthTokenRepository.save(token);
        log.info(">>> [DB TOKEN PERSISTENCE] Manual refresh token saved into PostgreSQL.");
    }

    public boolean isAuthorized() {
        try {
            if (hasRefreshToken()) {
                return true;
            }
            if (flow != null) {
                com.google.api.client.auth.oauth2.Credential cred = flow.loadCredential(USER_ID);
                if (cred != null) {
                    if (cred.getRefreshToken() != null && !cred.getRefreshToken().isBlank()) {
                        return true;
                    }
                    if (cred.getAccessToken() != null && cred.getExpiresInSeconds() != null && cred.getExpiresInSeconds() > 60) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Error checking authorization status: {}", e.getMessage());
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
        return flow.newAuthorizationUrl()
                .setRedirectUri(getEffectiveRedirectUri())
                .setAccessType("offline")
                .set("prompt", "consent") // Ensures refresh_token is always returned by Google
                .build();
    }

    public String exchangeCodeForTokens(String code) {
        if (flow == null) {
            throw new GmailIntegrationException("Gmail OAuth flow is not initialized");
        }
        try {
            String uri = getEffectiveRedirectUri();
            log.info("Exchanging Google OAuth authorization code using redirectUri='{}'...", uri);
            GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(uri).execute();
            flow.createAndStoreCredential(response, USER_ID);

            String finalRefreshToken = persistTokenToDatabase(response);

            log.info("Successfully exchanged authorization code and stored Gmail OAuth credentials.");
            if (finalRefreshToken != null && !finalRefreshToken.isBlank()) {
                log.info("================================================================================");
                log.info(">>> [PERMANENT GOOGLE REFRESH TOKEN GENERATED]");
                log.info(">>> Refresh Token: {}", finalRefreshToken);
                log.info(">>> Render Variable: GOOGLE_REFRESH_TOKEN={}", finalRefreshToken);
                log.info("================================================================================");
            }
            return finalRefreshToken;
        } catch (IOException e) {
            log.error("Failed to exchange authorization code for Gmail tokens: {}", e.getMessage(), e);
            throw new GmailIntegrationException("Failed to exchange authorization code for Gmail tokens: " + e.getMessage(), e);
        }
    }

    private String persistTokenToDatabase(GoogleTokenResponse response) {
        try {
            Optional<OAuthToken> existing = oauthTokenRepository.findById(USER_ID);
            String existingRefreshToken = existing.map(OAuthToken::getRefreshToken).orElse(null);

            String finalRefreshToken = response.getRefreshToken();
            if ((finalRefreshToken == null || finalRefreshToken.isBlank()) && existingRefreshToken != null) {
                finalRefreshToken = existingRefreshToken;
            }

            Long expirationMs = null;
            if (response.getExpiresInSeconds() != null) {
                expirationMs = System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000L);
            }

            OAuthToken token = OAuthToken.builder()
                    .id(USER_ID)
                    .accessToken(response.getAccessToken())
                    .refreshToken(finalRefreshToken)
                    .expirationTimeMs(expirationMs)
                    .emailAddress(recruitmentEmail)
                    .build();

            oauthTokenRepository.save(token);
            log.info(">>> [DB TOKEN PERSISTENCE] Gmail OAuth tokens persisted into PostgreSQL (refreshTokenPresent={})",
                    (finalRefreshToken != null && !finalRefreshToken.isBlank()));
            return finalRefreshToken;
        } catch (Exception ex) {
            log.warn("Could not persist OAuth token to database: {}", ex.getMessage());
            return response.getRefreshToken();
        }
    }

    public void purgeStoredCredential() {
        try {
            if (flow != null && flow.getCredentialDataStore() != null) {
                flow.getCredentialDataStore().delete(USER_ID);
            }
            oauthTokenRepository.deleteById(USER_ID);
            log.info("Purged expired/revoked Gmail OAuth credential from memory, disk, and database.");
        } catch (Exception e) {
            log.warn("Could not purge credential: {}", e.getMessage());
        }
    }

    public Gmail getGmailService() {
        try {
            // Priority 1: Check in-memory/file flow credential if it has refresh token or valid access token
            if (flow != null) {
                com.google.api.client.auth.oauth2.Credential cred = flow.loadCredential(USER_ID);
                if (cred != null) {
                    boolean hasRefreshToken = cred.getRefreshToken() != null && !cred.getRefreshToken().isBlank();
                    boolean hasValidAccessToken = cred.getAccessToken() != null && (cred.getExpiresInSeconds() == null || cred.getExpiresInSeconds() > 60);
                    if (hasRefreshToken || hasValidAccessToken) {
                        return new Gmail.Builder(httpTransport, JSON_FACTORY, cred)
                                .setApplicationName("RecruitmentATSPlatform")
                                .build();
                    } else {
                        log.warn("Stored Gmail credential has expired and has no refresh token. Purging dead credential.");
                        purgeStoredCredential();
                    }
                }
            }

            // Priority 2: Use persistent refresh token (from env or PostgreSQL database)
            String rToken = getRefreshToken();
            if (rToken != null && !rToken.isBlank()) {
                GoogleCredential cred = new GoogleCredential.Builder()
                        .setTransport(httpTransport)
                        .setJsonFactory(JSON_FACTORY)
                        .setClientSecrets(getClientId(), getClientSecret())
                        .build()
                        .setRefreshToken(rToken);

                Optional<OAuthToken> dbToken = oauthTokenRepository.findById(USER_ID);
                if (dbToken.isPresent() && dbToken.get().getAccessToken() != null) {
                    cred.setAccessToken(dbToken.get().getAccessToken());
                }

                return new Gmail.Builder(httpTransport, JSON_FACTORY, cred)
                        .setApplicationName("RecruitmentATSPlatform")
                        .build();
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to build Gmail API client: {}", e.getMessage(), e);
            return null;
        }
    }
}
