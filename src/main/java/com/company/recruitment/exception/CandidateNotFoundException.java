package com.company.recruitment.exception;

import java.util.UUID;

public class CandidateNotFoundException extends ResourceNotFoundException {
    public CandidateNotFoundException(UUID id) {
        super("Candidate not found with ID: " + id);
    }
    public CandidateNotFoundException(String message) {
        super(message);
    }
}
