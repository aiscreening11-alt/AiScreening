package com.company.recruitment.service.resume;

public interface ResumeParserService {
    String extractText(byte[] resume, String fileName);
    String detectMimeType(byte[] resume, String fileName);
}
