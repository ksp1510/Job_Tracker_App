# Job Tracker API Testing Guide

Complete guide for testing backend endpoints using Postman.

## Table of Contents
1. [Quick Start](#quick-start)
2. [Environment Setup](#environment-setup)
3. [Importing the Collection](#importing-the-collection)
4. [Testing Workflows](#testing-workflows)
5. [Troubleshooting](#troubleshooting)
6. [API Reference](#api-reference)

---

## Quick Start

### Prerequisites
- Postman installed (download from https://www.postman.com/downloads/)
- Backend servers running:
  - Spring Boot: `http://localhost:8080`
  - FastAPI AI Services: `http://localhost:8001`

### 5-Minute Setup

1. **Import the Postman Collection**
   - Open Postman
   - Click "Import" button
   - Select `Job_Tracker_Postman_Collection.json`
   - Collection will appear in your workspace

2. **Create Environment**
   - Click "Environments" in left sidebar
   - Click "+" to create new environment
   - Name it "Job Tracker Local"
   - Add variables (see [Environment Setup](#environment-setup))

3. **Test Authentication**
   - Open "Authentication & Users" folder
   - Run "Register New User" request
   - Run "Login" request (token auto-saved)
   - Run "Get Current User Profile" to verify

4. **Start Testing**
   - All requests are now ready to use
   - Auth token is automatically applied to protected endpoints

---

## Environment Setup

### Create Postman Environment

1. Click "Environments" tab
2. Click "+" to create new environment
3. Name: `Job Tracker Local`

### Required Environment Variables

Add these variables to your environment:

| Variable | Initial Value | Current Value | Description |
|----------|---------------|---------------|-------------|
| `baseUrl` | `http://localhost:8080` | - | Spring Boot backend URL |
| `aiBaseUrl` | `http://localhost:8001` | - | FastAPI AI services URL |
| `authToken` | (leave empty) | - | Auto-populated after login |
| `userId` | (leave empty) | - | Auto-populated after registration |
| `applicationId` | (leave empty) | - | Auto-populated when creating application |
| `savedJobId` | (leave empty) | - | Auto-populated when saving job |
| `jobId` | (leave empty) | - | Manually set from search results |
| `fileId` | (leave empty) | - | Manually set after file upload |
| `notificationId` | (leave empty) | - | Manually set from notifications list |
| `interviewPrepId` | (leave empty) | - | Manually set from interview prep |
| `mockInterviewSessionId` | (leave empty) | - | Auto-populated when starting mock interview |

### Production Environment

For testing against production:

| Variable | Value |
|----------|-------|
| `baseUrl` | `https://api.yourapp.com` |
| `aiBaseUrl` | `https://ai.yourapp.com` |

---

## Importing the Collection

### Method 1: File Import
1. Open Postman
2. Click "Import" button (top left)
3. Select "Upload Files"
4. Choose `Job_Tracker_Postman_Collection.json`
5. Click "Import"

### Method 2: Drag and Drop
1. Open Postman
2. Drag `Job_Tracker_Postman_Collection.json` into Postman window
3. Collection automatically imports

### Verify Import
- Collection name: "Job Tracker App - Complete API Collection"
- Should contain 11 main folders
- Total requests: 70+

---

## Testing Workflows

### Workflow 1: User Registration & Authentication

**Steps:**
1. **Register New User**
   - Folder: `Authentication & Users`
   - Request: `Register New User`
   - Update email/password in body
   - Click "Send"
   - Verify: Status 200/201, userId in response

2. **Login**
   - Request: `Login`
   - Use same email/password from registration
   - Click "Send"
   - Verify: Token automatically saved to environment

3. **Get Profile**
   - Request: `Get Current User Profile`
   - Click "Send"
   - Verify: Returns user details

**Expected Results:**
- Registration: 200/201 status, user object returned
- Login: 200 status, JWT token returned and saved
- Get Profile: 200 status, user details displayed

---

### Workflow 2: Job Search & Save

**Steps:**
1. **Login** (if not already logged in)

2. **Search Jobs**
   - Folder: `Job Search & Saved Jobs`
   - Request: `Search Jobs`
   - Modify query parameters:
     - `query`: "software engineer"
     - `location`: "New York"
     - `jobType`: FULL_TIME
   - Click "Send"
   - Copy a job ID from results

3. **Get Job Details**
   - Request: `Get Job Details`
   - Set environment variable: `jobId` = copied job ID
   - Click "Send"
   - Verify job details

4. **Save Job**
   - Request: `Save Job`
   - Add notes in request body (optional)
   - Click "Send"
   - Verify: savedJobId auto-saved to environment

5. **Get Saved Jobs**
   - Request: `Get Saved Jobs`
   - Click "Send"
   - Verify: Your saved job appears in list

**Expected Results:**
- Search: Paginated job listings
- Job Details: Complete job information
- Save: Job saved successfully
- Saved Jobs List: Contains your saved job

---

### Workflow 3: Application Management

**Steps:**
1. **Create Application**
   - Folder: `Applications`
   - Request: `Create Application`
   - Modify request body:
     ```json
     {
       "companyName": "Tech Corp",
       "jobTitle": "Senior Software Engineer",
       "location": "New York, NY",
       "status": "APPLIED",
       "applicationDate": "2025-10-25",
       "jobDescription": "Looking for experienced engineer...",
       "notes": "Applied via LinkedIn",
       "salary": "$120,000 - $150,000"
     }
     ```
   - Click "Send"
   - Verify: applicationId saved to environment

2. **Get All Applications**
   - Request: `Get All Applications`
   - Click "Send"
   - Verify: Your application appears

3. **Update Application Status**
   - Request: `Update Application`
   - Modify body:
     ```json
     {
       "status": "INTERVIEW",
       "interviewDate": "2025-11-01T14:00:00",
       "notes": "Phone screen scheduled"
     }
     ```
   - Click "Send"

4. **Get Applications by Status**
   - Request: `Get Applications by Status`
   - Set query param: `status=INTERVIEW`
   - Click "Send"

**Expected Results:**
- Create: 200/201 status, application object
- Get All: Array of applications
- Update: Updated application returned
- Filter by Status: Only INTERVIEW status applications

**Application Status Values:**
- APPLIED
- INTERVIEW
- ASSESSMENT
- OFFER
- REJECTED
- HIRED
- WITHDRAWN

---

### Workflow 4: File Management

**Important:** File uploads require actual PDF files on your system.

**Steps:**
1. **Prepare Files**
   - Have a PDF resume ready (max 10MB)
   - Have a PDF cover letter ready (max 10MB)

2. **Create Application First** (if not done)
   - Follow Workflow 3, Step 1

3. **Upload Resume**
   - Folder: `File Management`
   - Request: `Upload Resume`
   - In Body tab:
     - Select "form-data"
     - Key: "file" (type: File)
     - Click "Select Files" and choose your resume PDF
   - Verify query param: `applicationId={{applicationId}}`
   - Click "Send"
   - Copy file ID from response

4. **Upload Cover Letter**
   - Request: `Upload Cover Letter`
   - Select your cover letter PDF
   - Click "Send"

5. **Get Files by Application**
   - Request: `Get Files by Application`
   - Click "Send"
   - Verify: Both files listed

6. **Get Presigned URL**
   - Request: `Get Presigned URL`
   - Set environment variable: `fileId` = copied file ID
   - Click "Send"
   - Copy presigned URL from response
   - Open URL in browser to download file

**Expected Results:**
- Upload: Success message with filename
- Get Files: Array of file metadata
- Presigned URL: Valid S3 URL (expires in 1 hour)

**Testing Notes:**
- S3 must be properly configured with AWS credentials
- Files are stored in path: `{firstName}_{lastName}_{userId}/{type}/{filename}.pdf`
- Presigned URLs expire after 1 hour

---

### Workflow 5: Notifications & Reminders

**Steps:**
1. **Create Interview Reminder**
   - Folder: `Notifications`
   - Request: `Create Interview Reminder`
   - Modify body:
     ```json
     {
       "applicationId": "{{applicationId}}",
       "interviewDate": "2025-11-01T14:00:00",
       "customMessage": "Technical phone screen with hiring manager"
     }
     ```
   - Click "Send"
   - Note: Notification set for 1 day before interview

2. **Create Custom Notification**
   - Request: `Create Custom Notification`
   - Modify body:
     ```json
     {
       "applicationId": "{{applicationId}}",
       "message": "Follow up with recruiter",
       "notifyAt": "2025-10-30T10:00:00",
       "type": "CUSTOM"
     }
     ```
   - Click "Send"

3. **Get All Notifications**
   - Request: `Get All Notifications`
   - Click "Send"

4. **Get Unread Notifications**
   - Request: `Get Unread Notifications`
   - Click "Send"
   - Note: Only returns "due" notifications (notifyAt <= now)

5. **Mark as Read**
   - Copy notification ID from response
   - Set environment variable: `notificationId`
   - Request: `Mark Notification as Read`
   - Click "Send"

6. **Update Notification Preferences**
   - Request: `Update Notification Preferences`
   - Modify body:
     ```json
     {
       "emailEnabled": true,
       "inAppEnabled": true
     }
     ```
   - Click "Send"

**Expected Results:**
- Create: Notification object with scheduled time
- Get All: List of all notifications
- Get Unread: Only unread + due notifications
- Mark Read: Notification marked as read
- Preferences: Updated preferences returned

**Notification Types:**
- INTERVIEW
- DEADLINE
- CUSTOM

---

### Workflow 6: AI Job Search

**Steps:**
1. **Check AI Service Health**
   - Folder: `AI Microservices`
   - Request: `Health Check`
   - Click "Send"
   - Verify: Service status "healthy"

2. **Get Models Status**
   - Request: `Get Models Status`
   - Click "Send"
   - Verify: Model details returned

3. **AI-Powered Job Search**
   - Request: `AI Job Search`
   - Modify body:
     ```json
     {
       "user_id": "user123",
       "query": "senior python developer",
       "location": "San Francisco, CA",
       "skills": ["Python", "Django", "PostgreSQL"],
       "experience_level": "senior",
       "salary_min": 120000,
       "salary_max": 180000,
       "remote_only": false,
       "employment_type": "full-time",
       "use_resume_matching": false,
       "limit": 20
     }
     ```
   - Click "Send"

4. **Review Results**
   - Check `jobs` array in response
   - Note `match_score` for each job (0.0-1.0)
   - Review `ai_insights` section
   - Check `search_metadata` for API info

**Expected Results:**
- Jobs with AI-generated match scores
- AI insights and recommendations
- Processing time and metadata
- Ranked results based on relevance

**Note:** AI features require API keys:
- `THEIRSTACK_API_KEY`
- `JSEARCH_API_KEY` (RapidAPI)
- Optional: `OPENAI_API_KEY` (for enhanced AI features)

---

### Workflow 7: Resume Analysis & Optimization

**Steps:**
1. **Analyze Resume**
   - Folder: `AI Microservices`
   - Request: `Analyze Resume`
   - Modify body:
     ```json
     {
       "user_id": "user123",
       "resume_text": "JOHN DOE\nSenior Software Engineer\n\nEXPERIENCE\nSenior Software Engineer at Tech Corp (2020-Present)\n- Led team of 5 engineers\n- Developed microservices using Python and Java\n- Improved system performance by 40%\n\nSKILLS\nPython, Java, React, PostgreSQL, Docker, Kubernetes, AWS",
       "job_description": "We are seeking a Senior Software Engineer with 5+ years of experience in Python, microservices architecture, and team leadership.",
       "analysis_type": "full"
     }
     ```
   - Click "Send"

2. **Review Analysis Results**
   - Check `extracted_skills` with proficiency levels
   - Review `experience_summary`
   - Note `total_experience_years` and `career_level`
   - Check `ats_score` (0-100)
   - Review `skill_gaps` and `improvement_suggestions`

3. **Optimize Resume**
   - Request: `Optimize Resume`
   - Use same resume_text and job_description
   - Click "Send"

4. **Review Optimization**
   - Check `optimized_sections` with improvements
   - Review `added_keywords`
   - Compare `original_score` vs `optimized_score`
   - Note `improvement_percentage`
   - Review `recommendations`

**Alternative: Upload Resume File**
- Request: `Upload and Analyze Resume`
- Select PDF/DOCX/TXT file (max 10MB)
- Click "Send"

**Expected Results:**
- Analysis: Detailed skills, experience, education extraction
- ATS score and improvement suggestions
- Optimization: Improved resume sections
- Keyword recommendations
- Score improvements

**Analysis Types:**
- `full`: Complete analysis
- `keywords`: Keywords only
- `skills`: Skills extraction only
- `experience`: Experience summary only

---

### Workflow 8: Mock Interview

**Steps:**
1. **Generate Interview Questions**
   - Folder: `AI Microservices`
   - Request: `Generate Interview Questions`
   - Modify body:
     ```json
     {
       "user_id": "user123",
       "resume_text": "Senior Software Engineer with 5+ years in Python, Java, microservices...",
       "job_description": "Looking for Senior Engineer to lead backend team...",
       "job_title": "Senior Software Engineer",
       "company_name": "Tech Corp",
       "interview_type": "technical",
       "difficulty_level": "medium",
       "num_questions": 10
     }
     ```
   - Click "Send"
   - Review generated questions

2. **Start Mock Interview**
   - Request: `Start Mock Interview`
   - Modify body (similar to above, add duration):
     ```json
     {
       "user_id": "user123",
       "resume_text": "...",
       "job_description": "...",
       "job_title": "Senior Software Engineer",
       "company_name": "Tech Corp",
       "interview_duration": 30,
       "interview_type": "technical"
     }
     ```
   - Click "Send"
   - Session ID auto-saved to environment
   - Note first question from response

3. **Submit Answer**
   - Request: `Submit Mock Interview Answer`
   - Update query params:
     - `session_id`: {{mockInterviewSessionId}}
     - `question_id`: (from question)
     - `answer`: Your answer text
   - Click "Send"
   - Review evaluation

4. **Continue Interview**
   - Repeat Step 3 for each question
   - Review scores and feedback after each answer

5. **Get Session Details**
   - Request: `Get Mock Interview Session`
   - Click "Send"
   - Review overall performance
   - Check progress metrics

**Expected Results:**
- Questions: Relevant interview questions with expected answers
- Session: Active interview with progress tracking
- Answer Evaluation: Scores (0-10) with detailed feedback
- Overall Performance: Aggregated scores and trends

**Interview Types:**
- technical
- behavioral
- system_design
- coding

**Difficulty Levels:**
- easy
- medium
- hard

---

### Workflow 9: Reporting

**Steps:**
1. **Generate Excel Report**
   - Folder: `Reports`
   - Request: `Generate Excel Report`
   - Optional: Add query param `status=INTERVIEW`
   - Click "Send"
   - Click "Save Response" → "Save to a file"
   - Save as `applications_report.xlsx`
   - Open in Excel/Google Sheets

2. **Generate PDF Report**
   - Request: `Generate PDF Report`
   - Optional: Filter by status
   - Click "Send"
   - Save as `applications_report.pdf`
   - Open in PDF viewer

**Expected Results:**
- Excel: Formatted spreadsheet with application data
- PDF: Professional report with application details
- Both: Can be filtered by application status

---

## Troubleshooting

### Common Issues

#### 1. "Authorization header missing" Error

**Problem:** Forgot to login or token expired

**Solutions:**
- Run "Login" request to get new token
- Verify environment is selected (top-right dropdown)
- Check `authToken` variable is populated
- Ensure Authorization header format: `Bearer {{authToken}}`

#### 2. "Invalid JWT token" Error

**Problem:** Token expired or malformed

**Solutions:**
- Login again to get fresh token
- Check token doesn't have extra spaces
- Verify JWT_SECRET matches backend configuration

#### 3. File Upload Fails

**Problem:** File validation or S3 issues

**Solutions:**
- Verify file is PDF format
- Check file size < 10MB
- Ensure AWS credentials are configured in backend
- Verify S3 bucket exists and has correct permissions

#### 4. Job Search Returns Empty

**Problem:** External APIs not configured or mock data disabled

**Solutions:**
- Check environment variables:
  - `THEIRSTACK_API_KEY`
  - `RAPIDAPI_KEY` (for JSearch)
- For testing: Set `USE_MOCK_DATA=true` in AI service
- Verify API keys are valid

#### 5. AI Features Return Errors

**Problem:** AI service not running or models not loaded

**Solutions:**
- Check AI service is running: `http://localhost:8001/health`
- Verify models are loaded: `/api/v1/models/status`
- Check FastAPI logs for errors
- Ensure sufficient memory for models

#### 6. SSE Notification Stream Doesn't Work in Postman

**Problem:** Postman doesn't handle SSE well

**Solutions:**
- Use curl instead:
  ```bash
  curl -H "Authorization: Bearer YOUR_TOKEN" \
       -N http://localhost:8080/notifications/stream
  ```
- Or use EventSource in browser console:
  ```javascript
  const eventSource = new EventSource('http://localhost:8080/notifications/stream');
  eventSource.onmessage = (event) => console.log(event.data);
  ```

#### 7. CORS Errors

**Problem:** Frontend origin not allowed

**Solutions:**
- Check backend CORS configuration
- Add your origin to `CORS_ALLOWED_ORIGINS`
- For local testing: `http://localhost:3000`

#### 8. Rate Limiting Errors

**Problem:** Too many requests

**Solutions:**
- Login: Wait 1 minute (5 requests/minute limit)
- Registration: Wait 1 hour (10 requests/hour limit)
- Use different email/IP for testing

---

### Debugging Tips

1. **Check Response Status Code**
   - 200/201: Success
   - 400: Bad request (check request body)
   - 401: Unauthorized (login required)
   - 403: Forbidden (insufficient permissions)
   - 404: Not found (check ID)
   - 500: Server error (check backend logs)

2. **View Response Body**
   - Click "Pretty" tab for formatted JSON
   - Look for error messages
   - Check error_code for specific issues

3. **Check Backend Logs**
   - Spring Boot: Console output or logs/app.log
   - FastAPI: Console output or ai_service.log
   - Look for stack traces and error messages

4. **Verify Environment Variables**
   - Click eye icon next to environment name
   - Ensure all required variables are set
   - Check Initial Value vs Current Value

5. **Use Postman Console**
   - View → Show Postman Console
   - See detailed request/response logs
   - Check headers and cookies

---

## API Reference

### Spring Boot Backend (Port 8080)

#### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and get JWT token

#### Users
- `GET /users/me` - Get current user profile
- `POST /users/change-password` - Change password
- `POST /users/change-email` - Change email
- `DELETE /users/delete-account` - Delete account

#### Job Search
- `GET /jobs/search` - Search jobs with filters
- `GET /jobs/cache/status` - Check cache status
- `GET /jobs/cache` - Get cached results
- `DELETE /jobs/cache` - Clear cache
- `GET /jobs/{id}` - Get job details
- `POST /jobs/{id}/save` - Save job
- `GET /jobs/saved` - Get saved jobs
- `DELETE /jobs/saved/{id}` - Remove saved job
- `GET /jobs/search-history` - Get search history

#### Applications
- `POST /applications` - Create application
- `GET /applications` - Get all applications
- `GET /applications/by-status` - Filter by status
- `GET /applications/{id}` - Get specific application
- `PUT /applications/{id}` - Update application
- `DELETE /applications/{id}` - Delete application
- `GET /applications/{id}/with-files` - Get with files

#### Files
- `POST /files/upload/resume` - Upload resume
- `POST /files/upload/coverletter` - Upload cover letter
- `GET /files` - List all files
- `GET /files/applications/{id}/files` - Get files by application
- `DELETE /files/delete/{id}` - Delete file
- `GET /files/download/{id}` - Download file
- `GET /files/presigned/{id}` - Get presigned URL

#### Notifications
- `GET /notifications` - Get all notifications
- `GET /notifications/unread` - Get unread notifications
- `PATCH /notifications/{id}/read` - Mark as read
- `DELETE /notifications/{id}` - Delete notification
- `POST /notifications/interview-reminder` - Create interview reminder
- `POST /notifications/deadline-reminder` - Create deadline reminder
- `POST /notifications/custom` - Create custom notification
- `GET /notifications/preferences` - Get preferences
- `PUT /notifications/preferences` - Update preferences
- `GET /notifications/stream` - SSE stream

#### Feedback & Reports
- `POST /feedback` - Submit feedback
- `GET /feedback/my-feedback` - Get my feedback
- `GET /reports/excel` - Generate Excel report
- `GET /reports/pdf` - Generate PDF report

#### AI Interview Prep
- `POST /ai/interview-prep` - Save Q&A
- `GET /ai/interview-prep` - Get all questions
- `GET /ai/interview-prep/category/{category}` - Get by category
- `POST /ai/interview-prep/{id}/bookmark` - Toggle bookmark

#### Testing
- `POST /test/send-test-email` - Send test email
- `GET /test/api-keys-status` - Check API keys

---

### FastAPI AI Services (Port 8001)

#### Health & Status
- `GET /health` - Health check
- `GET /api/v1/models/status` - Models status

#### Job Search
- `POST /api/v1/jobs/search` - AI job search
- `POST /api/v1/jobs/match` - Match jobs to resume

#### Resume Analysis
- `POST /api/v1/resume/analyze` - Analyze resume
- `POST /api/v1/resume/optimize` - Optimize resume
- `POST /api/v1/resume/upload` - Upload and analyze

#### Interview Assistance
- `POST /api/v1/interview/questions` - Generate questions
- `POST /api/v1/interview/mock-start` - Start mock interview
- `POST /api/v1/interview/mock-answer` - Submit answer
- `GET /api/v1/interview/mock-session/{id}` - Get session

#### Utilities
- `POST /api/v1/embeddings/generate` - Generate embeddings

---

## Advanced Testing Scenarios

### Scenario 1: End-to-End Job Application Flow

1. Register & Login
2. Search for jobs
3. Save interesting jobs
4. Create application from saved job
5. Upload resume and cover letter
6. Set interview reminder
7. Update application status to INTERVIEW
8. Generate interview questions (AI)
9. Practice with mock interview (AI)
10. Update status to OFFER
11. Generate reports

### Scenario 2: AI-Powered Job Matching

1. Upload resume via AI service
2. Search jobs with AI ranking
3. Match jobs to resume
4. Review match scores
5. Optimize resume for top job
6. Save optimized resume
7. Apply with optimized resume

### Scenario 3: Complete Interview Preparation

1. Create application
2. Generate interview questions
3. Start mock interview
4. Answer all questions
5. Review performance
6. Save best answers to interview prep
7. Bookmark important questions
8. Set interview reminder

---

## Performance Testing

### Load Testing Tips

1. **Use Collection Runner**
   - Select collection
   - Click "Run" button
   - Set iterations (e.g., 100)
   - Add delay between requests (e.g., 100ms)

2. **Monitor Performance**
   - Check response times
   - Watch for errors
   - Monitor backend logs
   - Check database connections

3. **Rate Limit Testing**
   - Test login rate limit: 5/minute
   - Test registration rate limit: 10/hour
   - Verify 429 status code returned

---

## Security Testing

### Authentication Testing

1. **Test without token**
   - Remove Authorization header
   - Verify 401 Unauthorized

2. **Test with expired token**
   - Use old token
   - Verify 401 Unauthorized

3. **Test with malformed token**
   - Use invalid JWT
   - Verify 401 Unauthorized

### Authorization Testing

1. **Test accessing other user's data**
   - Create two users
   - Try to access User B's data with User A's token
   - Verify 403 Forbidden or filtered results

2. **Test file access control**
   - Upload file for Application A
   - Try to access with different user
   - Verify ownership checks

---

## Best Practices

1. **Use Environment Variables**
   - Never hardcode IDs or tokens
   - Use variables for all dynamic values
   - Create separate environments for dev/staging/prod

2. **Test Scripts**
   - Use Pre-request Scripts for setup
   - Use Test Scripts for validation
   - Auto-save important IDs to environment

3. **Organization**
   - Group related requests in folders
   - Use descriptive names
   - Add descriptions to requests

4. **Documentation**
   - Document expected responses
   - Add example request bodies
   - Note any prerequisites

5. **Version Control**
   - Export collections regularly
   - Commit to git repository
   - Track changes over time

---

## Additional Resources

### Backend Logs
- Spring Boot: `jobtracker-backend/logs/`
- FastAPI: `ai-microservices/ai_service.log`

### Configuration Files
- Spring Boot: `jobtracker-backend/src/main/resources/application.yml`
- FastAPI: `ai-microservices/app/config/settings.py`

### Documentation
- Spring Boot API: Auto-generated from controllers
- FastAPI API: `http://localhost:8001/docs` (Swagger UI)
- FastAPI ReDoc: `http://localhost:8001/redoc`

---

## Support

If you encounter issues not covered in this guide:

1. Check backend logs for errors
2. Verify all environment variables are set
3. Ensure both backend services are running
4. Review request/response in Postman Console
5. Check GitHub issues for similar problems

---

## Appendix: Sample Data

### Sample User
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

### Sample Application
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Senior Software Engineer",
  "location": "New York, NY",
  "status": "APPLIED",
  "applicationDate": "2025-10-25",
  "jobDescription": "We are seeking an experienced software engineer...",
  "notes": "Applied through company website",
  "salary": "$120,000 - $150,000"
}
```

### Sample Resume Text
```
JOHN DOE
Senior Software Engineer
john.doe@example.com | (555) 123-4567

PROFESSIONAL SUMMARY
Results-driven Senior Software Engineer with 7+ years of experience in full-stack development,
specializing in microservices architecture, cloud technologies, and agile methodologies.

EXPERIENCE

Senior Software Engineer | Tech Corp | New York, NY | 2020 - Present
• Led team of 5 engineers in developing microservices-based platform
• Improved system performance by 40% through optimization and caching strategies
• Implemented CI/CD pipeline reducing deployment time by 60%
• Technologies: Python, Java, React, PostgreSQL, Docker, Kubernetes, AWS

Software Engineer | StartupXYZ | San Francisco, CA | 2018 - 2020
• Developed RESTful APIs serving 1M+ requests daily
• Built responsive frontend using React and TypeScript
• Collaborated with product team on feature development
• Technologies: Python, Django, React, MongoDB, Redis

EDUCATION
Bachelor of Science in Computer Science | State University | 2018

SKILLS
Languages: Python, Java, JavaScript, TypeScript, SQL
Frameworks: Spring Boot, Django, React, Node.js
Databases: PostgreSQL, MongoDB, Redis
DevOps: Docker, Kubernetes, AWS, CI/CD, Jenkins
Tools: Git, JIRA, Agile/Scrum

CERTIFICATIONS
• AWS Certified Solutions Architect
• Certified Kubernetes Administrator (CKA)
```

---

**Last Updated:** October 25, 2025
**Version:** 1.0
**Collection Version:** 2.1.0
