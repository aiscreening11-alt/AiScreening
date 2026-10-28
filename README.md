# AI-Powered Candidate ATS Screening & Automated Email Platform

Production-ready automated recruitment platform built with **Java 21**, **Spring Boot 3.3.4**, **PostgreSQL**, **Flyway**, **Apache Tika**, **Google Gmail API (OAuth 2.0)**, and **Pluggable AI Analysis (OpenAI / Gemini / Mock)**.

---

## 🚀 Key Features

1. **Autonomous Gmail Ingestion:**
   - Periodically polls official recruitment inbox (`recruitment@company.com`) via official Gmail API & OAuth 2.0.
   - Guaranteed **idempotency** using unique `gmail_message_id` database constraints.
2. **Multi-Format Resume Text Extraction:**
   - Extracts text and metadata from PDF, DOC, DOCX files using **Apache Tika** with **Apache PDFBox** fallback.
3. **Intelligent Information Extraction:**
   - Extracts Full Name, Resume Email vs. Sender Email (discrepancy detection), Phone, Total Experience, Education/Degree, Current Company, and normalized skills.
4. **Hybrid ATS Scoring Engine:**
   - **Deterministic Rule-Based Score (70%):** Configurable weighted scoring (Skills 40%, Experience 20%, Education 10%, Keywords 10%, Responsibilities 10%, Certifications 5%, Resume Quality 5%).
   - **Extensible AI Analysis (30%):** Pluggable LLM provider (`mock`, `openai`, `gemini`) returning validated JSON with PII anonymization.
   - **Combined Score:** $(\text{RuleScore} \times 0.70) + (\text{AIScore} \times 0.30)$.
5. **Two-Stage Decision Workflow:**
   - **Stage 1 (Screening):** System calculates score $\rightarrow$ `SHORTLISTED` ($\ge 80$), `REVIEW_REQUIRED` ($60-79$), `REJECTED` ($< 60$). Automatically triggers Stage 1 candidate emails using Thymeleaf HTML templates.
   - **Stage 2 (Interview & Hiring):** Recruiter submits verdict via REST API $\rightarrow$ `SELECTED` or `NOT_SELECTED`. Triggers Stage 2 candidate notification emails.
6. **Recruiter & Admin REST Dashboard:**
   - Full REST endpoints to view metrics, query candidate dossiers, rescreen applicants, and override statuses with audit logging.

---

## 🛠️ Technology Stack

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Security, Validation, Thymeleaf, Actuator)
- **Database:** PostgreSQL 16 (H2 in-memory for testing mode)
- **Migrations:** Flyway Database Migration (`V1__init_schema.sql`)
- **Document Processing:** Apache Tika 2.9.2 & Apache PDFBox 2.0.31
- **Email Ingestion & Sending:** Google Gmail API v1 & Google OAuth2 Client
- **AI Integrations:** OpenAI GPT-4o / GPT-4o-mini & Google Gemini 1.5 Flash (with fallback Mock provider)
- **Containerization:** Docker & Docker Compose

---

## 💻 Windows 11 Local Development Setup

### 1. Prerequisites Check
Open PowerShell and verify Java 21 and Maven installations:

```powershell
java -version
mvn -v
```

*Note: If Java 21 or Maven are installed in `C:\tools`, set your environment variables:*
```powershell
$env:JAVA_HOME = "C:\tools\jdk-21.0.6+7"
$env:PATH = "C:\tools\jdk-21.0.6+7\bin;C:\tools\apache-maven-3.9.9\bin;" + $env:PATH
```

### 2. PostgreSQL Database Setup
Ensure PostgreSQL is running locally or via Docker Compose:

```powershell
# Option A: Start PostgreSQL via Docker Compose
docker compose up -d postgres

# Option B: Local PostgreSQL Service
# Create Database
psql -U postgres -c "CREATE DATABASE recruitment_db;"
```

### 3. Build & Run Unit/Integration Tests
```powershell
mvn clean test
```

### 4. Run Spring Boot Backend
```powershell
mvn spring-boot:run
```

---

## 🔐 Gmail OAuth 2.0 Setup Documentation

Follow these steps to connect your company's recruitment Gmail address (`recruitment@company.com`):

1. **Create Google Cloud Project:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/).
   - Create a new project named `Recruitment-ATS-Platform`.
2. **Enable Gmail API:**
   - Navigate to **APIs & Services > Library**.
   - Search for **Gmail API** and click **Enable**.
3. **Configure OAuth Consent Screen:**
   - Go to **APIs & Services > OAuth consent screen**.
   - Select **User Type: External** (or Internal for Workspace).
   - Add scopes:
     - `https://www.googleapis.com/auth/gmail.readonly`
     - `https://www.googleapis.com/auth/gmail.send`
     - `https://www.googleapis.com/auth/gmail.modify`
4. **Create OAuth Credentials:**
   - Go to **APIs & Services > Credentials > Create Credentials > OAuth client ID**.
   - Select Application Type: **Web application**.
   - Add Authorized Redirect URI: `http://localhost:8080/api/v1/gmail/oauth2callback`.
   - Download client credentials JSON or copy the **Client ID** and **Client Secret**.
5. **Configure Environment Variables:**
   ```properties
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-client-secret
   GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/gmail/oauth2callback
   RECRUITMENT_EMAIL=recruitment@company.com
   ```
6. **Authenticate Mailbox:**
   - Open browser to: `http://localhost:8080/api/v1/gmail/auth-url`
   - Visit the generated `authorizationUrl`, log in with `recruitment@company.com`, and authorize the permissions.
   - Google redirects to the callback endpoint and securely saves access & refresh tokens to `./tokens`.

---

## 📡 REST API Reference Guide

### Basic Authentication
- **Username:** `recruiter`
- **Password:** `recruiter123`

### 1. Job Management APIs

#### Create a Job
```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -u recruiter:recruiter123 \
  -H "Content-Type: application/json" \
  -d '{
    "jobTitle": "Java Spring Boot Developer",
    "department": "Engineering",
    "description": "Build resilient microservices backend with Spring Boot, Kafka, AWS, and SQL.",
    "requiredSkills": ["Java", "Spring Boot", "Microservices", "Kafka", "SQL", "AWS"],
    "preferredSkills": ["Docker", "Kubernetes", "Redis"],
    "minimumExperience": 5.0,
    "educationRequirement": "B.Tech",
    "location": "Remote / New York",
    "employmentType": "FULL_TIME"
  }'
```

#### List All Jobs
```bash
curl -X GET http://localhost:8080/api/v1/jobs -u recruiter:recruiter123
```

---

### 2. Candidate & Screening APIs

#### List Candidates (Paginated & Filtered)
```bash
curl -X GET "http://localhost:8080/api/v1/candidates?status=SHORTLISTED&page=0&size=10" \
  -u recruiter:recruiter123
```

#### Get Candidate Full Dossier
```bash
curl -X GET http://localhost:8080/api/v1/candidates/{candidateId} \
  -u recruiter:recruiter123
```

#### Get ATS Score Breakdown
```bash
curl -X GET http://localhost:8080/api/v1/candidates/{candidateId}/ats-result \
  -u recruiter:recruiter123
```

#### Direct Resume File Upload & Instant Screening
```bash
curl -X POST http://localhost:8080/api/v1/candidates/upload \
  -u recruiter:recruiter123 \
  -F "file=@John_Doe_Resume.pdf" \
  -F "jobId={jobId}" \
  -F "senderEmail=john@gmail.com"
```

---

### 3. Stage 2 Interview & Hiring Decision API

#### Record Interview Result (`SELECTED` or `NOT_SELECTED`)
```bash
curl -X POST http://localhost:8080/api/v1/candidates/{candidateId}/interview-result \
  -u recruiter:recruiter123 \
  -H "Content-Type: application/json" \
  -d '{
    "result": "SELECTED",
    "interviewerName": "Sarah Senior Architect",
    "feedback": "Outstanding candidate. Excellent microservices architectural depth and Spring Boot experience."
  }'
```
*Triggers automated "Congratulations – You Have Been Selected" email.*

---

### 4. Recruiter Dashboard & Mailbox Controls

#### Dashboard Metrics
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/metrics -u recruiter:recruiter123
```

#### Trigger On-Demand Gmail Sweeper
```bash
curl -X POST http://localhost:8080/api/v1/gmail/poll-now -u recruiter:recruiter123
```

---

## 🧪 End-to-End Workflow Demonstration

Here is the exact lifecycle verified by our integration tests:

1. **Step 1:** Recruiter creates **Java Spring Boot Developer** opening via `POST /api/v1/jobs`.
2. **Step 2:** Candidate `john@gmail.com` sends `John_Doe_Resume.pdf` to `recruitment@company.com`.
3. **Step 3:** `@Scheduled` job in `GmailPollingService` detects new unread message.
4. **Step 4-6:** PDF bytes downloaded $\rightarrow$ `ResumeParserService` extracts text $\rightarrow$ `CandidateInfoExtractor` extracts profile.
5. **Step 7-8:** `ATSScoringService` evaluates rule-based score (82/100) + AI score (87/100) $\rightarrow$ Combined Score: **83.5**.
6. **Step 9:** Final score $83.5 \ge 80.0 \rightarrow$ Decision: **`SHORTLISTED`**.
7. **Step 10:** `EmailService` renders `candidate-shortlisted.html` and sends email to candidate.
8. **Step 11-12:** Recruiter conducts interview and posts verdict `SELECTED` via `POST /api/v1/candidates/{id}/interview-result`.
9. **Step 13:** Candidate receives **"Congratulations – You Have Been Selected"** email notification.

---

## 🐳 Running with Docker Compose

```bash
docker compose up -d --build
```
This boots both **PostgreSQL 16** and the **Spring Boot Recruitment Backend** in isolated containers with auto-migrations.
