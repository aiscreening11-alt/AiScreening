package com.company.recruitment.service.resume.storage;

public interface ResumeStorageService {
    String store(byte[] file, String fileName);
    byte[] retrieve(String storageKey);
    boolean exists(String storageKey);
    void delete(String storageKey);
}
