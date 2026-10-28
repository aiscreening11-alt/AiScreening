package com.company.recruitment.config;

import com.company.recruitment.entity.Interviewer;
import com.company.recruitment.repository.InterviewerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewerDataInitializer implements CommandLineRunner {

    private final InterviewerRepository interviewerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking and initializing default Interviewer accounts...");

        String defaultEncodedPassword = passwordEncoder.encode("Sanvii@2016");

        seedOrUpdateInterviewer(
                "Lead Technical Architect",
                "lead.architect@sanvii.com",
                defaultEncodedPassword,
                "Senior Technical Architect",
                "Engineering"
        );

        seedOrUpdateInterviewer(
                "Vikram Sharma",
                "vikram.sharma@sanvii.com",
                defaultEncodedPassword,
                "Lead Full Stack Engineer",
                "Engineering"
        );

        seedOrUpdateInterviewer(
                "Pooja Verma",
                "pooja.verma@sanvii.com",
                defaultEncodedPassword,
                "Engineering Manager",
                "Technology & Management"
        );

        log.info("Interviewer accounts initialized successfully.");
    }

    private void seedOrUpdateInterviewer(String fullName, String email, String encodedPassword, String designation, String department) {
        Optional<Interviewer> existing = interviewerRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            Interviewer interviewer = existing.get();
            // Ensure valid BCrypt hash matches Sanvii@2016
            if (!passwordEncoder.matches("Sanvii@2016", interviewer.getPasswordHash())) {
                interviewer.setPasswordHash(encodedPassword);
                interviewerRepository.save(interviewer);
                log.info("Updated password hash for default interviewer: {}", email);
            }
        } else {
            Interviewer interviewer = Interviewer.builder()
                    .fullName(fullName)
                    .email(email)
                    .passwordHash(encodedPassword)
                    .designation(designation)
                    .department(department)
                    .role("INTERVIEWER")
                    .active(true)
                    .build();
            interviewerRepository.save(interviewer);
            log.info("Created default interviewer account: {}", email);
        }
    }
}
