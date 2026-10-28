package com.company.recruitment.service.resume.storage;

import com.company.recruitment.exception.ResumeProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
public class FileSystemResumeStorageService implements ResumeStorageService {

    private final Path storageDirectory;

    public FileSystemResumeStorageService(@Value("${storage.local.directory:./storage/resumes}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
                log.info("Created resume storage directory at: {}", storageDirectory);
            }
        } catch (IOException e) {
            throw new ResumeProcessingException("Failed to initialize storage directory: " + storageDirectory, e);
        }
    }

    @Override
    public String store(byte[] file, String fileName) {
        if (file == null || file.length == 0) {
            throw new ResumeProcessingException("Cannot store empty resume file");
        }
        try {
            String extension = "";
            if (fileName != null && fileName.contains(".")) {
                extension = fileName.substring(fileName.lastIndexOf("."));
            }
            String storageKey = UUID.randomUUID() + extension;
            Path targetFile = storageDirectory.resolve(storageKey);
            Files.write(targetFile, file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Stored resume file [{} bytes] with key: {}", file.length, storageKey);
            return storageKey;
        } catch (IOException e) {
            throw new ResumeProcessingException("Failed to store resume file: " + fileName, e);
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResumeProcessingException("Invalid storage key");
        }
        try {
            Path targetFile = storageDirectory.resolve(storageKey).normalize();
            if (!targetFile.startsWith(storageDirectory) || !Files.exists(targetFile)) {
                throw new ResumeProcessingException("Resume file not found for key: " + storageKey);
            }
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new ResumeProcessingException("Failed to retrieve resume for key: " + storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return false;
        Path targetFile = storageDirectory.resolve(storageKey).normalize();
        return targetFile.startsWith(storageDirectory) && Files.exists(targetFile);
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        try {
            Path targetFile = storageDirectory.resolve(storageKey).normalize();
            if (targetFile.startsWith(storageDirectory)) {
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException e) {
            log.warn("Failed to delete stored file: {}", storageKey, e);
        }
    }
}
