package com.company.recruitment.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResumePdfGeneratorTest {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 40f;
    private static final float USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;
    private float currentY;

    @Test
    public void generateJessySmithResumePdf() throws IOException {
        document = new PDDocument();
        startNewPage();

        // 1. HEADER BANNER
        drawHeaderBanner();

        // 2. PROFESSIONAL SUMMARY
        drawSectionHeader("SUMMARY");
        drawParagraph(
                "Professional Java developer with 4+ years of experience building client-server web applications. " +
                "Aspires to join Scaler as a Senior Java Developer to help design game-changing e-commerce solutions " +
                "and to improve problem-solving skills. Decreased startup times for Java-powered applications by 70% in " +
                "previous roles. Possesses unmatched coding skills and testing expertise to deliver client-specific software."
        );
        currentY -= 10;

        // 3. TECHNICAL SKILLS
        drawSectionHeader("TECHNICAL SKILLS");
        drawBullet("Languages: Java, Node.js, Bootstrap, JavaScript, SQL, Python");
        drawBullet("Frameworks: Spring Boot, Spring Data JPA, Hibernate, J2EE, Spring, JSF, Apache Struts");
        drawBullet("Web & APIs: REST API, Microservices, SOAP, Servlets, HTML, CSS, jQuery, PHP");
        drawBullet("Databases & Cloud: PostgreSQL, MySQL, Redis, AWS, Docker, Kubernetes");
        drawBullet("DevOps & Tools: Git, Maven, Jenkins CI/CD, Eclipse, WebLogic, JVM Performance Tuning");
        currentY -= 10;

        // 4. PROFESSIONAL EXPERIENCE
        drawSectionHeader("PROFESSIONAL EXPERIENCE");
        drawSubHeader("Senior Java Developer", "InterviewBit | Bangalore", "August 2020 - Present");
        drawBullet("Designed software solutions by analyzing system performance standards, resulting in 38% higher performance efficiency.");
        drawBullet("Utilized relevant technical skills to deliver specifications, program changes, unit test scripts, and documentation.");
        drawBullet("Supported programming changes during quality assurance, user acceptance testing, and post-implementation follow-up.");
        drawBullet("Improved code inefficiencies, resulting in a 40% increase in app stability and reduced downtime by 27%.");
        drawBullet("Utilized DevOps, agile principles, and Jenkins for CI/CD. Identified low-performing workflows by writing automated scripts.");
        drawBullet("Tutored and trained 10 junior Java developers in the development of enterprise applications using J2EE and Spring Boot.");
        currentY -= 10;

        // 5. ACHIEVEMENTS
        drawSectionHeader("ACHIEVEMENTS");
        drawBullet("Improved overall application throughput and performance by 30%.");
        drawBullet("Enhanced code quality and reduced production downtime by 27%.");
        drawBullet("2 times selected as Employee of the Month for outstanding engineering performance.");
        drawBullet("Trained over 10 junior engineers in developing high-scale microservices.");
        drawBullet("Nominated as technical project lead for mission-critical payment workflows.");
        currentY -= 10;

        // Check if page break needed
        checkPageBreak(160);

        // 6. CERTIFICATIONS
        drawSectionHeader("CERTIFICATIONS");
        drawBullet("Building Scalable Java Microservices - Coursera Certificate");
        drawBullet("Software Engineering Master Certification - IEEE Computer Society");
        drawBullet("Spring Professional Certification (Pivotal / VMware)");
        drawBullet("Professional Scrum Master (PSM I)");
        drawBullet("Oracle Certified Professional, Java SE 8 Programmer");
        currentY -= 10;

        // 7. EDUCATION
        drawSectionHeader("EDUCATION");
        drawSubHeader("Bachelor of Science in Computer Science and Network Security", "Institute of Engineering & Science, IPS Academy, Indore", "2012 - 2015");
        drawBullet("Excelled in Cisco Networking, Distributed Systems, and Database Management.");
        drawBullet("Member of NCC Army wing and Student Engineering Council.");
        currentY -= 4;
        drawSubHeader("Higher Secondary School (Class XII)", "Army Public School, Mhow, MP", "2011 - 2012");
        drawBullet("Graduated with 92% aggregate in Mathematics, Physics, and Chemistry.");
        currentY -= 10;

        // 8. LANGUAGES & VOLUNTEERING
        checkPageBreak(120);
        drawSectionHeader("LANGUAGES & COMMUNITY");
        drawBullet("Languages: English (Fluent), French (Intermediate), German (Basic)");
        drawBullet("Volunteering: Active coordinator in the Indore Student Food Relief Organization supplying food provisions to over 200 community villages.");

        contentStream.close();

        File outputFile = new File("Jessy_Smith_Resume.pdf");
        document.save(outputFile);
        document.close();

        assertTrue(outputFile.exists(), "Resume PDF must be successfully created");
        System.out.println(">>> Generated PDF saved at: " + outputFile.getAbsolutePath() + " (Size: " + outputFile.length() + " bytes)");
    }

    @Test
    public void generateAmitSinghResumePdf() throws IOException {
        document = new PDDocument();
        startNewPage();

        // 1. HEADER BANNER
        drawHeaderBanner(
                "Amit Singh",
                "Digital Marketing & Graphic Design Specialist",
                "Email: amit.singh.recruitment.test@gmail.com  |  Phone: +91-98765-43210  |  Application ID: " + System.currentTimeMillis(),
                "Portfolio: behance.net/amitsingh  |  LinkedIn: linkedin.com/in/amit-singh"
        );

        // 2. PROFESSIONAL SUMMARY
        drawSectionHeader("SUMMARY");
        drawParagraph(
                "Creative and detail-oriented Digital Marketing & Graphic Design Specialist with 1.5 years of experience " +
                "in social media branding, digital campaigns, and visual asset creation. Seeking opportunities to transition " +
                "into corporate operations and technical teams. Skilled in Adobe Creative Suite, Canva, and content management systems."
        );
        currentY -= 10;

        // 3. TECHNICAL & DESIGN SKILLS
        drawSectionHeader("KEY SKILLS & TOOLS");
        drawBullet("Design Tools: Adobe Photoshop, Adobe Illustrator, Canva, Figma, InDesign");
        drawBullet("Digital Marketing: Social Media Marketing (Instagram, Facebook), SEO Basics, Google Ads");
        drawBullet("Web Platforms: WordPress, Shopify, Basic HTML5, Basic CSS3");
        drawBullet("Content & Office: Copywriting, MS Office Suite, Google Analytics, Trello");
        currentY -= 10;

        // 4. PROFESSIONAL EXPERIENCE
        drawSectionHeader("PROFESSIONAL EXPERIENCE");
        drawSubHeader("Junior Graphic Designer", "Creative Spark Media | New Delhi", "July 2022 - August 2023");
        drawBullet("Designed marketing collateral, social media banners, and print brochures for local commercial brands.");
        drawBullet("Managed Instagram and Facebook ad visuals, boosting organic brand engagement by 15%.");
        drawBullet("Collaborated with content writers to align graphic themes with promotional campaign calendars.");
        drawBullet("Maintained digital asset libraries and optimized image formats for fast website loading.");
        currentY -= 10;

        drawSubHeader("Digital Marketing Intern", "BrandVibe Studios | Noida", "January 2022 - June 2022");
        drawBullet("Created daily social media creatives using Canva and Adobe Photoshop.");
        drawBullet("Monitored Google Analytics traffic reports for WordPress blogs and created monthly performance decks.");
        currentY -= 10;

        // 5. ACHIEVEMENTS
        drawSectionHeader("KEY HIGHLIGHTS");
        drawBullet("Successfully executed digital branding campaign for 12 local e-commerce stores.");
        drawBullet("Increased social media follower base by 25% over a 6-month period.");
        currentY -= 10;

        // 6. CERTIFICATIONS
        checkPageBreak(120);
        drawSectionHeader("CERTIFICATIONS");
        drawBullet("Google Digital Garage - Fundamentals of Digital Marketing");
        drawBullet("HubSpot Academy - Inbound Marketing Certification");
        drawBullet("Canva Design Foundations Certificate");
        currentY -= 10;

        // 7. EDUCATION
        drawSectionHeader("EDUCATION");
        drawSubHeader("Bachelor of Arts (B.A.) in English Literature", "Delhi University, New Delhi", "2018 - 2021");
        drawBullet("Active member of the University Fine Arts Club and Editorial Committee.");
        currentY -= 4;
        drawSubHeader("Higher Secondary School (Class XII)", "Kendriya Vidyalaya, New Delhi", "2017 - 2018");
        drawBullet("Graduated with 84% aggregate in Humanities & Arts stream.");
        currentY -= 10;

        // 8. LANGUAGES
        drawSectionHeader("LANGUAGES");
        drawBullet("Languages: Hindi (Native), English (Proficient)");

        contentStream.close();

        File outputFile = new File("Amit_Singh_Resume.pdf");
        document.save(outputFile);
        document.close();

        assertTrue(outputFile.exists(), "Amit Singh Resume PDF must be successfully created");
        System.out.println(">>> Generated PDF saved at: " + outputFile.getAbsolutePath() + " (Size: " + outputFile.length() + " bytes)");
    }

    private void startNewPage() throws IOException {
        if (contentStream != null) {
            contentStream.close();
        }
        currentPage = new PDPage(PDRectangle.A4);
        document.addPage(currentPage);
        contentStream = new PDPageContentStream(document, currentPage);
        currentY = PAGE_HEIGHT - MARGIN;
    }

    private void checkPageBreak(float requiredHeight) throws IOException {
        if (currentY - requiredHeight < MARGIN) {
            startNewPage();
        }
    }

    private void drawHeaderBanner() throws IOException {
        drawHeaderBanner(
                "Jessy Smith",
                "Senior Java Developer",
                "Email: jessy.smith.dev@gmail.com  |  Phone: (939)-555-0113  |  Application ID: " + System.currentTimeMillis(),
                "LinkedIn: linkedin.com/in/jessy.smith  |  Portfolio: example123.com"
        );
    }

    private void drawHeaderBanner(String name, String title, String contact1, String contact2) throws IOException {
        float bannerHeight = 85f;
        float bannerY = PAGE_HEIGHT - bannerHeight;

        // Draw dark header background
        contentStream.setNonStrokingColor(new Color(24, 33, 56)); // Navy dark
        contentStream.addRect(0, bannerY, PAGE_WIDTH, bannerHeight);
        contentStream.fill();

        // Name
        contentStream.beginText();
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);
        contentStream.newLineAtOffset(MARGIN, bannerY + 54);
        contentStream.showText(name);
        contentStream.endText();

        // Title
        contentStream.beginText();
        contentStream.setNonStrokingColor(new Color(96, 165, 250)); // Light blue
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
        contentStream.newLineAtOffset(MARGIN, bannerY + 36);
        contentStream.showText(title);
        contentStream.endText();

        // Contact info line 1
        contentStream.beginText();
        contentStream.setNonStrokingColor(new Color(229, 231, 235)); // Off white
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(MARGIN, bannerY + 18);
        contentStream.showText(contact1);
        contentStream.endText();

        // Contact info line 2
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, bannerY + 6);
        contentStream.showText(contact2);
        contentStream.endText();

        currentY = bannerY - 20;
    }

    private void drawSectionHeader(String title) throws IOException {
        checkPageBreak(40);
        contentStream.setNonStrokingColor(new Color(30, 64, 175)); // Royal blue
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(title);
        contentStream.endText();

        // Draw accent line
        contentStream.setStrokingColor(new Color(219, 234, 254)); // Light blue line
        contentStream.setLineWidth(1.2f);
        contentStream.moveTo(MARGIN, currentY - 3);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, currentY - 3);
        contentStream.stroke();

        currentY -= 16;
    }

    private void drawSubHeader(String title, String org, String period) throws IOException {
        checkPageBreak(25);
        contentStream.setNonStrokingColor(new Color(17, 24, 39)); // Near black
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10.5f);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, currentY);
        contentStream.showText(title);
        contentStream.endText();

        contentStream.setNonStrokingColor(new Color(75, 85, 99)); // Gray
        contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9.5f);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, currentY - 11);
        contentStream.showText(org + "  |  " + period);
        contentStream.endText();

        currentY -= 22;
    }

    private void drawParagraph(String text) throws IOException {
        contentStream.setNonStrokingColor(new Color(55, 65, 81));
        contentStream.setFont(PDType1Font.HELVETICA, 9.5f);
        List<String> lines = wrapText(text, USONT_WIDTH(USABLE_WIDTH), PDType1Font.HELVETICA, 9.5f);

        for (String line : lines) {
            checkPageBreak(14);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, currentY);
            contentStream.showText(line);
            contentStream.endText();
            currentY -= 13;
        }
    }

    private void drawBullet(String text) throws IOException {
        contentStream.setNonStrokingColor(new Color(55, 65, 81));
        contentStream.setFont(PDType1Font.HELVETICA, 9.5f);
        float bulletIndent = 12f;
        List<String> lines = wrapText(text, USONT_WIDTH(USABLE_WIDTH - bulletIndent), PDType1Font.HELVETICA, 9.5f);

        for (int i = 0; i < lines.size(); i++) {
            checkPageBreak(13);
            contentStream.beginText();
            if (i == 0) {
                // Bullet symbol
                contentStream.newLineAtOffset(MARGIN + 2, currentY);
                contentStream.showText("-  " + lines.get(i));
            } else {
                contentStream.newLineAtOffset(MARGIN + bulletIndent + 2, currentY);
                contentStream.showText(lines.get(i));
            }
            contentStream.endText();
            currentY -= 12.5f;
        }
    }

    private float USONT_WIDTH(float width) {
        return width;
    }

    private List<String> wrapText(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float lineWidth = (font.getStringWidth(testLine) / 1000f) * fontSize;
            if (lineWidth > width) {
                if (currentLine.length() > 0) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    result.add(testLine);
                    currentLine = new StringBuilder();
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }
        return result;
    }
}
