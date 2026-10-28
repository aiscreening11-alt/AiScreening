package com.company.recruitment.exception;

import java.util.UUID;

public class JobNotFoundException extends ResourceNotFoundException {
    public JobNotFoundException(UUID id) {
        super("Job not found with ID: " + id);
    }
    public JobNotFoundException(String message) {
        super(message);
    }
}
