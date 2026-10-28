package com.company.recruitment.service.resume;

import com.company.recruitment.entity.enums.Gender;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CandidateInfoExtractor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}|(?:\\+?\\d{1,3}[- .]?)?\\d{10}"
    );

    private static final Pattern EXP_YEARS_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*\\+?\\s*(?:years?|yrs?|yr)\\b", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "\\b(Bachelor(?:'s)?|Master(?:'s)?|B\\.Tech|M\\.Tech|B\\.S\\.?|M\\.S\\.?|B\\.E\\.?|M\\.E\\.?|BCA|MCA|MBA|Ph\\.?D|B\\.Sc|M\\.Sc)\\b", Pattern.CASE_INSENSITIVE
    );

    @Data
    @Builder
    public static class ExtractedCandidateInfo {
        private String fullName;
        private String email;
        private String phoneNumber;
        private Double totalExperienceYears;
        private String currentCompany;
        private String previousCompanies;
        private String degree;
        private String education;
        private String location;
        private String noticePeriod;
        private Gender gender;
        private List<String> extractedSkills;
    }

    public ExtractedCandidateInfo extract(String resumeText, String fileName, String senderEmail) {
        if (resumeText == null) {
            resumeText = "";
        }

        String email = extractEmail(resumeText);
        String phone = extractPhone(resumeText);
        String name = extractFullName(resumeText, fileName, senderEmail);
        Double experience = extractExperience(resumeText);
        String degree = extractDegree(resumeText);
        String currentCompany = extractCurrentCompany(resumeText);
        String location = extractLocation(resumeText);
        String noticePeriod = extractNoticePeriod(resumeText);
        Gender gender = inferGender(name, resumeText);

        return ExtractedCandidateInfo.builder()
                .fullName(name)
                .email(email)
                .phoneNumber(phone)
                .totalExperienceYears(experience)
                .currentCompany(currentCompany)
                .previousCompanies(null)
                .degree(degree)
                .education(degree != null ? degree + " Degree" : null)
                .location(location)
                .noticePeriod(noticePeriod)
                .gender(gender)
                .extractedSkills(Collections.emptyList())
                .build();
    }

    public String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toLowerCase().trim();
        }
        return null;
    }

    public String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    public String extractFullName(String text, String fileName, String senderEmail) {
        // 1. Try to read the top 3-4 lines of the resume
        String[] lines = text.split("\n");
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() < 3 || line.length() > 50) continue;
            // Skip headers like "RESUME", "CURRICULUM VITAE", "PROFILE", email addresses, phone numbers
            String lower = line.toLowerCase();
            if (lower.contains("resume") || lower.contains("curriculum") || lower.contains("cv") ||
                    lower.contains("page") || lower.contains("@") || lower.contains("linkedin") ||
                    lower.contains("github") || lower.contains("contact") || lower.contains("phone") ||
                    lower.matches(".*\\d{3,}.*")) {
                continue;
            }
            // Check if line looks like a person's name (2-4 capitalized words)
            if (line.matches("^[A-Z][a-zA-Z'.-]+(\\s+[A-Z][a-zA-Z'.-]+){1,3}$")) {
                return line;
            }
        }

        // 2. Try to infer from fileName (e.g. John_Doe_Resume.pdf -> John Doe)
        if (fileName != null && !fileName.isBlank()) {
            String cleanName = fileName.replaceFirst("[.][^.]+$", "")
                    .replaceAll("(?i)(resume|cv|profile|updated|latest|v\\d+|202\\d)", "")
                    .replaceAll("[_-]+", " ")
                    .trim();
            if (cleanName.matches("^[A-Za-z]+(\\s+[A-Za-z]+)+$") && cleanName.length() >= 3 && cleanName.length() <= 40) {
                return capitalizeWords(cleanName);
            }
        }

        // 3. Try to infer from sender email if available (e.g. john.doe@gmail.com -> John Doe)
        if (senderEmail != null && senderEmail.contains("@")) {
            String prefix = senderEmail.substring(0, senderEmail.indexOf('@'))
                    .replaceAll("\\d+", "")
                    .replaceAll("[._]+", " ")
                    .trim();
            if (prefix.contains(" ") && prefix.length() >= 4) {
                return capitalizeWords(prefix);
            }
        }

        return "Candidate";
    }

    public Double extractExperience(String text) {
        Matcher matcher = EXP_YEARS_PATTERN.matcher(text);
        double maxExp = 0.0;
        while (matcher.find()) {
            try {
                double val = Double.parseDouble(matcher.group(1));
                if (val > maxExp && val <= 40.0) {
                    maxExp = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return maxExp;
    }

    public String extractDegree(String text) {
        Matcher matcher = DEGREE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public String extractCurrentCompany(String text) {
        Pattern companyPattern = Pattern.compile("(?:Currently working at|Current Company|Employer|Company):?\\s*([A-Za-z0-9&, .]{3,40})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = companyPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public String extractLocation(String text) {
        Pattern locationPattern = Pattern.compile("(?:Location|Address|City):?\\s*([A-Za-z, -]{3,40})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = locationPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public String extractNoticePeriod(String text) {
        Pattern noticePattern = Pattern.compile("(?:Notice Period|Notice):?\\s*(\\d+\\s*(?:days?|months?|weeks?)|Immediate(?:ly)?|Serving Notice)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = noticePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static final Set<String> MALE_NAMES = new HashSet<>(Arrays.asList(
            "amit", "rajesh", "rahul", "abhishek", "suresh", "ramesh", "rohan", "vikas", "vivek",
            "sachin", "sunil", "anil", "deepak", "rohit", "sanjay", "manish", "ajay", "manoj",
            "arjun", "aditya", "alok", "ashish", "gaurav", "nikhil", "prateek", "sumit", "vijay",
            "vishal", "harish", "sandeep", "dinesh", "mayank", "kunal", "mohit", "ankit", "akash",
            "pankaj", "varun", "vinay", "saurabh", "shivam", "shubham", "sourabh", "tarun", "tushar",
            "utkarsh", "vaibhav", "yash", "kartik", "karan", "karthik", "dev", "harsh", "aman", "ayush",
            "raman", "chetan", "hemant", "naveen", "pravin", "parth", "dhruv",
            "john", "david", "michael", "james", "robert", "william", "richard", "joseph", "thomas",
            "charles", "daniel", "matthew", "anthony", "donald", "paul", "mark", "george", "steven",
            "kenneth", "andrew", "edward", "brian", "kevin", "ronald", "timothy", "jason", "jeffrey",
            "ryan", "jacob", "gary", "nicholas", "eric", "jonathan", "stephen", "larry", "justin",
            "scott", "brandon", "benjamin", "samuel", "gregory", "alexander", "frank", "patrick",
            "raymond", "jack", "dennis", "jerry", "tyler", "aaron", "adam", "nathan", "henry",
            "zachary", "peter", "kyle", "jeremy", "ethan", "christian", "sean", "austin", "noah",
            "jesse", "joe", "bryan", "billy", "jordan", "albert", "dylan", "gabriel", "logan",
            "alan", "juan", "wayne", "vincent", "elijah", "alex", "chris", "sam"
    ));

    private static final Set<String> FEMALE_NAMES = new HashSet<>(Arrays.asList(
            "priya", "neha", "pooja", "anjali", "sneha", "ritu", "divya", "sunita", "kavita",
            "shweta", "swati", "deepa", "megha", "rashmi", "priyanka", "simran", "aarti", "jyoti",
            "nisha", "rekha", "anita", "geeta", "sangeeta", "bhavna", "sonam", "shilpa", "tannu",
            "tanvi", "tanu", "preeti", "payal", "komal", "kajal", "sakshi", "riya", "shruti",
            "srishti", "sonali", "mansi", "muskan", "pallavi", "renu", "vidya", "kriti", "sonia",
            "garima", "radha", "lakshmi", "nupur",
            "jessy", "jessie", "jessica", "sarah", "emily", "emma", "olivia", "sophia", "isabella",
            "ava", "mia", "charlotte", "amelia", "harper", "evelyn", "abigail", "elizabeth", "mila",
            "ella", "avery", "sofia", "camila", "aria", "scarlett", "victoria", "madison", "luna",
            "grace", "chloe", "penelope", "layla", "riley", "zoey", "nora", "lily", "eleanor",
            "hannah", "lillian", "addison", "aubrey", "ellie", "stella", "natalie", "zoe", "leah",
            "hazel", "violet", "aurora", "savannah", "audrey", "brooklyn", "bella", "claire",
            "skylar", "lucy", "paisley", "everly", "anna", "caroline", "nova", "genesis", "emilia",
            "kennedy", "samantha", "maya", "willow", "kinsley", "naomi", "aaliyah", "elena",
            "allison", "gabriella", "alice", "madelyn", "cora", "ruby", "eva", "serenity", "autumn",
            "adeline", "hailey", "gianna", "valentine", "maria", "mary", "jennifer", "linda",
            "patricia", "susan", "barbara", "karen", "nancy", "lisa", "betty", "margaret", "sandra",
            "ashley", "kimberly", "donna", "carol", "michelle", "laura"
    ));

    public Gender inferGender(String name, String resumeText) {
        // 1. Check explicit Gender / Sex field in resume text
        if (resumeText != null && !resumeText.isBlank()) {
            Pattern explicitGenderPattern = Pattern.compile("\\b(?:Gender|Sex)\\s*[:\\-]?\\s*(Male|Female|Non-binary|Other|M|F)\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = explicitGenderPattern.matcher(resumeText);
            if (matcher.find()) {
                String val = matcher.group(1).trim().toLowerCase();
                if (val.equals("male") || val.equals("m")) return Gender.MALE;
                if (val.equals("female") || val.equals("f")) return Gender.FEMALE;
                if (val.equals("other") || val.equals("non-binary")) return Gender.OTHER;
            }

            // Check honorifics in resume text or near candidate header
            Pattern honorificPattern = Pattern.compile("\\b(Mr\\.?|Shri|Mrs\\.?|Ms\\.?|Miss|Smt\\.?)\\b", Pattern.CASE_INSENSITIVE);
            Matcher honMatcher = honorificPattern.matcher(resumeText.substring(0, Math.min(resumeText.length(), 400)));
            if (honMatcher.find()) {
                String hon = honMatcher.group(1).toLowerCase().replace(".", "");
                if (hon.equals("mr") || hon.equals("shri")) return Gender.MALE;
                if (hon.equals("ms") || hon.equals("mrs") || hon.equals("miss") || hon.equals("smt")) return Gender.FEMALE;
            }
        }

        // 2. Check Candidate First Name and name tokens
        if (name != null && !name.isBlank()) {
            String cleanName = name.replaceAll("[^a-zA-Z\\s]", " ").trim().toLowerCase();
            String[] tokens = cleanName.split("\\s+");
            for (String token : tokens) {
                if (FEMALE_NAMES.contains(token)) {
                    return Gender.FEMALE;
                }
                if (MALE_NAMES.contains(token)) {
                    return Gender.MALE;
                }
            }
        }

        // 3. Pronoun analysis across resume text if available
        if (resumeText != null && !resumeText.isBlank()) {
            int heCount = 0;
            int sheCount = 0;
            Matcher heMatcher = Pattern.compile("\\b(he|him|his)\\b", Pattern.CASE_INSENSITIVE).matcher(resumeText);
            while (heMatcher.find()) heCount++;

            Matcher sheMatcher = Pattern.compile("\\b(she|her|hers)\\b", Pattern.CASE_INSENSITIVE).matcher(resumeText);
            while (sheMatcher.find()) sheCount++;

            if (heCount >= 3 && heCount > sheCount * 2) {
                return Gender.MALE;
            } else if (sheCount >= 3 && sheCount > heCount * 2) {
                return Gender.FEMALE;
            }
        }

        return Gender.UNSPECIFIED;
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
