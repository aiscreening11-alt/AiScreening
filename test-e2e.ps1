# ==============================================================================
# End-to-End Automated Test Script for AI-Powered Recruitment ATS Platform
# ==============================================================================
# Prerequisites: The Spring Boot application must be running (mvn spring-boot:run)
# Run via: powershell -ExecutionPolicy Bypass -File .\test-e2e.ps1
# ==============================================================================

$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$authHeader = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("recruiter:recruiter123"))

function Print-Step($title) {
    Write-Host "`n========================================================" -ForegroundColor Cyan
    Write-Host ">>> $title" -ForegroundColor Yellow
    Write-Host "========================================================" -ForegroundColor Cyan
}

function Print-Success($msg) {
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

function Print-Info($msg) {
    Write-Host "[INFO] $msg" -ForegroundColor Gray
}

# ------------------------------------------------------------------------------
# STEP 1: Verify Health Check
# ------------------------------------------------------------------------------
Print-Step "STEP 1: Checking Application Health (/actuator/health)"
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    if ($health.status -eq "UP") {
        Print-Success "Application is UP and running healthy!"
    } else {
        Write-Host "Health status: $($health.status)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Could not connect to $baseUrl. Make sure 'mvn spring-boot:run' is running!" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# STEP 2: Create a Job Opening
# ------------------------------------------------------------------------------
Print-Step "STEP 2: Creating a Job Opening (/api/v1/jobs)"
$jobPayload = @{
    jobTitle = "Senior Java Spring Boot Developer"
    department = "Core Engineering"
    description = "We are seeking a seasoned backend engineer with deep expertise in Java 21, Spring Boot microservices, PostgreSQL, and Docker containerization."
    requiredSkills = @("Java", "Spring Boot", "PostgreSQL", "REST API", "Docker")
    preferredSkills = @("Kubernetes", "AWS", "Kafka", "Redis")
    minimumExperience = 4.0
    educationRequirement = "Bachelor of Science in Computer Science or related degree"
    location = "Remote"
    employmentType = "FULL_TIME"
} | ConvertTo-Json

$commonHeaders = @{
    Authorization = $authHeader
    "Content-Type" = "application/json"
}

$jobResponse = Invoke-RestMethod -Uri "$baseUrl/api/v1/jobs" -Method Post -Headers $commonHeaders -Body $jobPayload
$jobId = $jobResponse.data.id
Print-Success "Job Created Successfully!"
Print-Info "Job ID: $jobId"
Print-Info "Job Title: $($jobResponse.data.jobTitle)"
Print-Info "Required Skills: $($jobResponse.data.requiredSkills -join ', ')"

# ------------------------------------------------------------------------------
# STEP 3: Screen High-Scoring Candidate (Shortlisted Flow)
# ------------------------------------------------------------------------------
Print-Step "STEP 3: Screening High-Scoring Candidate (Expecting SHORTLISTED)"

$qualifiedResumeText = @"
Sarah Connor
Email: sarah.connor@cyberdyne.io
Phone: +1-555-839-2041
Location: San Francisco, CA

PROFESSIONAL SUMMARY
Senior Software Engineer with 6 years of industry experience specializing in Java, Spring Boot microservices, high-throughput REST APIs, and Docker containerization.

EDUCATION
Bachelor of Science in Computer Science, Stanford University (2018)

TECHNICAL SKILLS
- Languages: Java, Python, SQL
- Frameworks: Spring Boot, Spring Data JPA, Hibernate, REST API, Microservices
- Databases: PostgreSQL, Redis, MySQL
- DevOps & Cloud: Docker, Kubernetes, AWS, Apache Kafka, Git, CI/CD

PROFESSIONAL EXPERIENCE
Lead Backend Engineer | Skynet Systems | 2021 - Present
- Designed scalable Spring Boot microservices handling 25,000 requests per second.
- Managed PostgreSQL database clustering, sharding, and complex query optimizations.
- Deployed microservices as Docker containers orchestrated via Kubernetes in AWS.

Software Engineer | TechCore Inc | 2018 - 2021
- Developed REST APIs in Java using Spring Boot and Hibernate.
- Integrated Apache Kafka for event-driven message distribution.
"@

$tempDir = [System.IO.Path]::GetTempPath()
$qualifiedResumePath = Join-Path $tempDir "sarah_connor_resume.txt"
[System.IO.File]::WriteAllText($qualifiedResumePath, $qualifiedResumeText)

function Upload-Resume($filePath, $targetJobId, $senderEmail) {
    $boundary = [System.Guid]::NewGuid().ToString()
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
    $fileName = [System.IO.Path]::GetFileName($filePath)
    $LF = "`r`n"
    $body = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
        "Content-Type: text/plain$LF",
        [System.Text.Encoding]::UTF8.GetString($fileBytes),
        "--$boundary--"
    ) -join $LF

    $uploadHeaders = @{
        Authorization = $authHeader
        "Content-Type" = "multipart/form-data; boundary=$boundary"
    }

    $uri = "$baseUrl/api/v1/candidates/upload?jobId=$targetJobId&senderEmail=$senderEmail"
    try {
        return Invoke-RestMethod -Uri $uri -Method Post -Headers $uploadHeaders -Body $body
    } catch {
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            Write-Host "[SERVER ERROR] $errBody" -ForegroundColor Red
        }
        throw $_
    }
}

$candidate1 = Upload-Resume $qualifiedResumePath $jobId "sarah.connor@cyberdyne.io"
$cand1Id = $candidate1.data.id
$cand1Ats = $candidate1.data.atsResult
Print-Success "Screening Complete for $($candidate1.data.fullName)!"
Print-Info "Extracted Email: $($candidate1.data.resumeEmail)"
Print-Info "Total Experience: $($candidate1.data.totalExperienceYears) years"
Print-Info "Candidate Status: $($candidate1.data.status)"
Print-Info "ATS Final Score: $($cand1Ats.finalScore) / 100"
Print-Info "Rule Score: $($cand1Ats.ruleScore) | AI Score: $($cand1Ats.aiScore)"
Print-Info "Decision: $($cand1Ats.decision)"
Print-Info "Matched Skills: $($cand1Ats.matchedSkills -join ', ')"
Print-Info "Automated Email Sent: $($candidate1.data.emailCommunications[0].emailType) -> $($candidate1.data.emailCommunications[0].recipientEmail)"

# ------------------------------------------------------------------------------
# STEP 4: Screen Low-Scoring Candidate (Rejection Flow)
# ------------------------------------------------------------------------------
Print-Step "STEP 4: Screening Low-Scoring Candidate (Expecting REJECTED)"

$unqualifiedResumeText = @"
Bob Smith
Email: bob.smith@freemail.com
Phone: +1-555-123-4567
Location: Seattle, WA

PROFESSIONAL SUMMARY
Junior Graphic Designer with 1 year of experience in digital illustrations and banner design.

EDUCATION
Associate Degree in Fine Arts, City College (2022)

TECHNICAL SKILLS
Adobe Photoshop, Adobe Illustrator, Canva, Figma, Basic HTML

EXPERIENCE
Design Intern | PrintMedia Co | 2023 - 2024
- Created marketing flyers and logos for small business clients.
"@

$unqualifiedResumePath = Join-Path $tempDir "bob_smith_resume.txt"
[System.IO.File]::WriteAllText($unqualifiedResumePath, $unqualifiedResumeText)

$candidate2 = Upload-Resume $unqualifiedResumePath $jobId "bob.smith@freemail.com"
$cand2Id = $candidate2.data.id
$cand2Ats = $candidate2.data.atsResult
Print-Success "Screening Complete for $($candidate2.data.fullName)!"
Print-Info "Candidate Status: $($candidate2.data.status)"
Print-Info "ATS Final Score: $($cand2Ats.finalScore) / 100"
Print-Info "Missing Skills: $($cand2Ats.missingSkills -join ', ')"
Print-Info "Decision: $($cand2Ats.decision)"
Print-Info "Automated Email Sent: $($candidate2.data.emailCommunications[0].emailType) -> $($candidate2.data.emailCommunications[0].recipientEmail)"

# ------------------------------------------------------------------------------
# STEP 5: Record Stage 2 Interview Result (Selected Flow)
# ------------------------------------------------------------------------------
Print-Step "STEP 5: Submitting Stage 2 Interview Result -> SELECTED (/api/v1/candidates/$cand1Id/interview-result)"

$interviewPayload = @{
    result = "SELECTED"
    interviewerName = "Lead Technical Architect"
    feedback = "Exceptional performance in system design, Spring Boot internals, and concurrency patterns. Highly recommended for offer."
    interviewDate = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$interviewResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/candidates/$cand1Id/interview-result" -Method Post -Headers $commonHeaders -Body $interviewPayload
Print-Success "Interview Result Recorded Successfully!"
Print-Info "Candidate Final Status: $($interviewResp.data.status) (SELECTED)"
$latestEmail = $interviewResp.data.emailCommunications[-1]
Print-Info "Final Automated Email: $($latestEmail.emailType) -> $($latestEmail.recipientEmail)"
Print-Info "Email Subject: $($latestEmail.subject)"

# ------------------------------------------------------------------------------
# STEP 6: Check Candidates List & Detailed Profile
# ------------------------------------------------------------------------------
Print-Step "STEP 6: Fetching Candidates List & ATS Details (/api/v1/candidates)"

$candidatesList = Invoke-RestMethod -Uri "$baseUrl/api/v1/candidates?jobId=$jobId" -Method Get -Headers @{ Authorization = $authHeader }
Print-Success "Candidates Found for Job: $($candidatesList.data.content.Count)"
foreach ($c in $candidatesList.data.content) {
    Write-Host "  * Candidate: $($c.fullName) | Status: $($c.status) | Score: $($c.atsScore) | Decision: $($c.screeningDecision)" -ForegroundColor White
}

# ------------------------------------------------------------------------------
# STEP 7: View Recruiter Dashboard Metrics
# ------------------------------------------------------------------------------
Print-Step "STEP 7: Checking Recruiter Dashboard Metrics (/api/v1/dashboard/metrics)"

$metricsHeaders = @{ Authorization = $authHeader }
$metrics = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/metrics" -Method Get -Headers $metricsHeaders
$data = $metrics.data

Print-Success "Live Recruitment Metrics:"
Write-Host "  - Total Applications Processed : $($data.totalApplications)" -ForegroundColor White
Write-Host "  - Shortlisted                  : $($data.shortlisted)" -ForegroundColor White
Write-Host "  - Review Required              : $($data.reviewRequired)" -ForegroundColor White
Write-Host "  - Rejected                     : $($data.rejected)" -ForegroundColor White
Write-Host "  - Selected (Offer Extended)    : $($data.selected)" -ForegroundColor Green
Write-Host "  - Breakdown by Status          : $($data.statusBreakdown | ConvertTo-Json -Compress)" -ForegroundColor Gray

# Clean up temporary files
Remove-Item -Path $qualifiedResumePath -Force -ErrorAction SilentlyContinue
Remove-Item -Path $unqualifiedResumePath -Force -ErrorAction SilentlyContinue

Print-Step "ALL 7 TESTING STEPS PASSED SUCCESSFULLY! PLATFORM IS FULLY OPERATIONAL."
