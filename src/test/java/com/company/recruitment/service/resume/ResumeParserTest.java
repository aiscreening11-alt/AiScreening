package com.company.recruitment.service.resume;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ResumeParserTest {

    private TikaResumeParserServiceImpl resumeParserService;
    private CandidateInfoExtractor candidateInfoExtractor;

    @BeforeEach
    void setUp() {
        resumeParserService = new TikaResumeParserServiceImpl();
        candidateInfoExtractor = new CandidateInfoExtractor();
    }

    private byte[] createSamplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
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
    void testExtractTextFromPdf() throws IOException {
        String sampleContent = "John Doe\nEmail: john.doe@example.com\nPhone: 555-123-4567\nExperience: 6.5 years\nSkills: Java, Spring Boot, Kafka, AWS\nDegree: B.Tech in Computer Science";
        byte[] pdfBytes = createSamplePdf(sampleContent);

        String extracted = resumeParserService.extractText(pdfBytes, "John_Doe_Resume.pdf");

        assertNotNull(extracted);
        assertTrue(extracted.contains("John Doe"));
        assertTrue(extracted.contains("john.doe@example.com"));
        assertTrue(extracted.contains("Spring Boot"));
    }

    @Test
    void testCandidateInfoExtraction() {
        String resumeText = "John Doe\nEmail: john.doe@example.com\nPhone: (555) 123-4567\nExperience: 6.5 years with Spring Boot\nDegree: B.Tech in Computer Science\nCurrently working at TechCorp";

        CandidateInfoExtractor.ExtractedCandidateInfo info = candidateInfoExtractor.extract(resumeText, "John_Doe_Resume.pdf", "john.doe@example.com");

        assertNotNull(info);
        assertEquals("John Doe", info.getFullName());
        assertEquals("john.doe@example.com", info.getEmail());
        assertEquals("6.5", info.getTotalExperienceYears().toString());
        assertEquals("B.Tech", info.getDegree());
        assertEquals("TechCorp", info.getCurrentCompany());
    }
}
