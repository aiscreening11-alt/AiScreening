package com.company.recruitment;

import com.company.recruitment.dto.request.InterviewResultRequestDto;
import com.company.recruitment.dto.request.JobRequestDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.JobResponseDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.EmailCommunication;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.entity.enums.EmailType;
import com.company.recruitment.entity.enums.InterviewResult;
import com.company.recruitment.repository.EmailCommunicationRepository;
import com.company.recruitment.service.CandidateService;
import com.company.recruitment.service.JobService;
import com.company.recruitment.service.gmail.EmailIngestionPipeline;
import com.company.recruitment.service.interview.InterviewService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EndToEndRecruitmentFlowTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private EmailIngestionPipeline emailIngestionPipeline;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private EmailCommunicationRepository emailCommunicationRepository;

    private byte[] generateJohnDoePdfResume() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);

                String[] lines = new String[] {
                        "John Doe",
                        "Email: john@gmail.com",
                        "Phone: +1 555-0199",
                        "Summary: Senior Backend Engineer with 6.5 years experience.",
                        "Skills: Java 17, Spring Boot, Microservices, Kafka, SQL, AWS, Docker",
                        "Experience:",
                        "- Architected microservices with Spring Boot and Kafka",
                        "- Deployed scalable solutions on AWS and PostgreSQL",
                        "Education: B.Tech in Computer Science"
                };

                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -15);
                }
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void testEndToEndRecruitmentWorkflow() throws IOException {
        // Step 1: Recruiter creates "Java Spring Boot Developer" Job
        JobRequestDto jobReq = JobRequestDto.builder()
                .jobTitle("Java Spring Boot Developer")
                .department("Engineering")
                .description("Build microservices backend")
                .requiredSkills(List.of("Java", "Spring Boot", "Microservices", "Kafka", "SQL", "AWS"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .minimumExperience(5.0)
                .educationRequirement("B.Tech")
                .build();

        JobResponseDto createdJob = jobService.createJob(jobReq);
        assertNotNull(createdJob);
        assertNotNull(createdJob.getId());

        // Step 2-10: Candidate emails resume -> Ingestion -> Text extraction -> Hybrid ATS -> Shortlisted -> Email dispatched
        byte[] pdfBytes = generateJohnDoePdfResume();
        Candidate candidate = emailIngestionPipeline.processManualResumeUpload(pdfBytes, "John_Doe_Resume.pdf", createdJob.getId(), "john@gmail.com");

        assertNotNull(candidate);
        assertEquals("John Doe", candidate.getFullName());
        assertEquals("john@gmail.com", candidate.getResumeEmail());
        assertEquals(CandidateStatus.SHORTLISTED, candidate.getStatus());
        assertNotNull(candidate.getAtsResult());
        assertTrue(candidate.getAtsResult().getFinalScore() >= 80.0);

        // Verify Shortlist email was recorded in database
        List<EmailCommunication> initialEmails = emailCommunicationRepository.findByCandidateIdOrderBySentAtDesc(candidate.getId());
        assertFalse(initialEmails.isEmpty(), "Email communication history should contain the shortlist email");
        assertEquals(EmailType.SHORTLISTED, initialEmails.getFirst().getEmailType());

        // Step 11-13: Recruiter conducts interview and records result = SELECTED -> Triggers Selection Email
        InterviewResultRequestDto interviewReq = InterviewResultRequestDto.builder()
                .result(InterviewResult.SELECTED)
                .interviewerName("Sarah Recruiter")
                .feedback("Outstanding candidate. Strong system design and hands-on Spring Boot / Kafka skills.")
                .build();

        CandidateResponseDto finalResult = interviewService.recordInterviewResult(candidate.getId(), interviewReq);

        assertNotNull(finalResult);
        assertEquals(CandidateStatus.SELECTED, finalResult.getStatus());
        assertEquals(1, finalResult.getInterviews().size());
        assertEquals(InterviewResult.SELECTED, finalResult.getInterviews().getFirst().getResult());

        // Verify Selection email was recorded
        boolean hasSelectionEmail = finalResult.getEmailCommunications().stream()
                .anyMatch(e -> e.getEmailType() == EmailType.INTERVIEW_SELECTED);
        assertTrue(hasSelectionEmail, "Selection email should have been generated and recorded");
    }
}
