package com.company.recruitment.exception;

public class GmailIntegrationException extends RuntimeException {
    public GmailIntegrationException(String message) {
        super(message);
    }
    public GmailIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
