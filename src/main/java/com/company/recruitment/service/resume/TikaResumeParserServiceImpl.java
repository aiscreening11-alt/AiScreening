package com.company.recruitment.service.resume;

import com.company.recruitment.exception.ResumeProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TikaResumeParserServiceImpl implements ResumeParserService {

    private final Tika tika = new Tika();

    @Override
    public String extractText(byte[] resume, String fileName) {
        if (resume == null || resume.length == 0) {
            throw new ResumeProcessingException("Resume content is empty");
        }

        String mimeType = detectMimeType(resume, fileName);
        log.info("Extracting text from resume '{}' detected as MIME: {}", fileName, mimeType);

        // Try primary extraction via Apache Tika AutoDetectParser
        try (InputStream is = new ByteArrayInputStream(resume)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // Unlimited character limit
            Metadata metadata = new Metadata();
            if (fileName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            ParseContext context = new ParseContext();
            parser.parse(is, handler, metadata, context);

            String text = handler.toString();
            if (text != null && !text.trim().isEmpty()) {
                return normalizeText(text);
            }
        } catch (Exception e) {
            log.warn("Tika AutoDetectParser encountered an issue for '{}': {}. Attempting fallback.", fileName, e.getMessage());
        }

        // Fallback for PDF via Apache PDFBox
        if (mimeType != null && mimeType.contains("pdf")) {
            try (PDDocument document = PDDocument.load(resume)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String pdfText = stripper.getText(document);
                if (pdfText != null && !pdfText.trim().isEmpty()) {
                    return normalizeText(pdfText);
                }
            } catch (Exception e) {
                log.error("PDFBox fallback extraction failed for '{}': {}", fileName, e.getMessage());
            }
        }

        // Fallback: If it's plain text or ASCII
        try {
            String plain = new String(resume, StandardCharsets.UTF_8);
            if (!plain.trim().isEmpty() && !plain.contains("\u0000")) {
                return normalizeText(plain);
            }
        } catch (Exception ignored) {
        }

        throw new ResumeProcessingException("Unable to extract text from resume: " + fileName + ". The file may be empty, encrypted, or a scanned image.");
    }

    @Override
    public String detectMimeType(byte[] resume, String fileName) {
        if (resume == null || resume.length == 0) {
            return "application/octet-stream";
        }
        try {
            Metadata metadata = new Metadata();
            if (fileName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            try (InputStream is = new ByteArrayInputStream(resume)) {
                return tika.detect(is, metadata);
            }
        } catch (Exception e) {
            log.warn("Failed to detect MIME type for '{}', returning octet-stream: {}", fileName, e.getMessage());
            return "application/octet-stream";
        }
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
