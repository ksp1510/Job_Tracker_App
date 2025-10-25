# Postman API Testing - Quick Start Guide

Complete API testing suite for Job Tracker Application with 70+ endpoints.

---

## What's Included

This testing package includes:

1. **Postman Collection** - Complete API collection with all endpoints
2. **Postman Environment** - Pre-configured environment variables
3. **Testing Guide** - Comprehensive step-by-step testing instructions
4. **Test Data Examples** - Ready-to-use sample data for all endpoints

---

## Files Overview

| File | Description |
|------|-------------|
| `Job_Tracker_Postman_Collection.json` | Complete API collection (70+ requests) |
| `Job_Tracker_Postman_Environment.json` | Environment configuration file |
| `API_TESTING_GUIDE.md` | Detailed testing guide with workflows |
| `TEST_DATA_EXAMPLES.md` | Sample data and quick reference |

---

## Quick Setup (3 Steps)

### Step 1: Import Collection
1. Open Postman
2. Click **Import** button
3. Select `Job_Tracker_Postman_Collection.json`
4. Collection appears in your workspace

### Step 2: Import Environment
1. Click **Environments** tab (left sidebar)
2. Click **Import** button
3. Select `Job_Tracker_Postman_Environment.json`
4. Select environment from dropdown (top-right)

### Step 3: Start Backend Services
```bash
# Terminal 1 - Spring Boot Backend
cd jobtracker-backend
mvn spring-boot:run

# Terminal 2 - FastAPI AI Services
cd ai-microservices
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

**That's it! You're ready to test.**

---

## First Test Run (2 Minutes)

### Test Authentication Flow

1. **Register User**
   - Folder: `Authentication & Users`
   - Request: `Register New User`
   - Click **Send**
   - ✓ Status: 200/201
   - ✓ `userId` saved automatically

2. **Login**
   - Request: `Login`
   - Click **Send**
   - ✓ Status: 200
   - ✓ `authToken` saved automatically

3. **Get Profile**
   - Request: `Get Current User Profile`
   - Click **Send**
   - ✓ Status: 200
   - ✓ User details displayed

**Success!** You've completed your first test run.

---

## What Can You Test?

### Spring Boot Backend (Port 8080)

- **Authentication** - Registration, Login, JWT tokens
- **User Management** - Profile, Password change, Email change
- **Job Search** - Search with filters, Save jobs, Cache management
- **Applications** - CRUD operations, Status tracking, File attachments
- **File Management** - Upload resume/cover letter, S3 integration
- **Notifications** - Reminders, SSE streaming, Preferences
- **Reports** - Excel and PDF generation
- **Feedback** - User feedback submission
- **AI Interview Prep** - Question/Answer storage

### FastAPI AI Services (Port 8001)

- **AI Job Search** - Intelligent job matching and ranking
- **Resume Analysis** - Skills extraction, ATS scoring
- **Resume Optimization** - Job-specific improvements
- **Interview Questions** - AI-generated questions
- **Mock Interviews** - Practice with AI evaluation
- **Embeddings** - Text embedding generation

---

## Collection Structure

```
Job Tracker App - Complete API Collection
├── Authentication & Users (6 requests)
│   ├── Register, Login, Get Profile
│   └── Change Password, Change Email, Delete Account
├── Job Search & Saved Jobs (9 requests)
│   ├── Search, Cache management
│   └── Save/unsave jobs, Search history
├── Applications (7 requests)
│   ├── CRUD operations
│   └── Filter by status, Get with files
├── File Management (7 requests)
│   ├── Upload resume/cover letter
│   └── Download, Presigned URLs
├── Notifications (10 requests)
│   ├── Interview/deadline reminders
│   └── Custom notifications, Preferences, SSE stream
├── Feedback (3 requests)
├── Reports (2 requests)
├── AI Interview Prep (4 requests)
├── Test Endpoints (2 requests)
└── AI Microservices (12 requests)
    ├── Job search & matching
    ├── Resume analysis & optimization
    ├── Interview questions & mock interviews
    └── Embeddings
```

---

## Environment Variables

### Auto-Populated Variables
These are automatically saved when you run requests:

- `authToken` - Saved after login
- `userId` - Saved after registration
- `applicationId` - Saved after creating application
- `savedJobId` - Saved after saving job
- `mockInterviewSessionId` - Saved after starting mock interview

### Manual Variables
Set these manually when needed:

- `jobId` - Copy from job search results
- `fileId` - Copy from file upload response
- `notificationId` - Copy from notifications list
- `interviewPrepId` - Copy from interview prep list

### Base URLs
Pre-configured for local development:

- `baseUrl`: http://localhost:8080 (Spring Boot)
- `aiBaseUrl`: http://localhost:8001 (FastAPI)

---

## Common Testing Workflows

### Workflow 1: Complete Application Journey
1. Register & Login
2. Search for jobs
3. Save interesting job
4. Create application
5. Upload resume
6. Set interview reminder
7. Update status through stages
8. Generate report

**Time:** 10-15 minutes

### Workflow 2: AI-Powered Job Search
1. Check AI service health
2. Run AI job search
3. Match jobs to resume
4. Analyze resume
5. Optimize resume for job
6. Save optimized version

**Time:** 5-10 minutes

### Workflow 3: Interview Preparation
1. Create application
2. Generate interview questions
3. Start mock interview
4. Answer questions
5. Review feedback
6. Save best answers

**Time:** 15-20 minutes

---

## Documentation Links

- **API Testing Guide**: See `API_TESTING_GUIDE.md` for detailed workflows
- **Test Data Examples**: See `TEST_DATA_EXAMPLES.md` for sample data
- **Swagger UI** (FastAPI): http://localhost:8001/docs
- **ReDoc** (FastAPI): http://localhost:8001/redoc

---

## Features

### Automated Testing
- Auto-save authentication tokens
- Auto-save created resource IDs
- Pre-configured authorization headers
- Request examples with valid data

### Comprehensive Coverage
- 70+ API endpoints
- 9 major functional areas
- Both backend systems (Spring Boot + FastAPI)
- All CRUD operations

### Developer Friendly
- Organized folder structure
- Descriptive request names
- Inline documentation
- Example request bodies
- Test scripts for validation

---

## Prerequisites

### Required
- Postman (Desktop or Web)
- Java 17+ (for Spring Boot)
- Python 3.8+ (for FastAPI)
- MongoDB running

### Optional (for full features)
- AWS credentials (for S3 file storage)
- API keys (THEIRSTACK_API_KEY, RAPIDAPI_KEY)
- OpenAI API key (for enhanced AI features)

---

## Troubleshooting

### "Connection refused" Error
**Solution:** Ensure backend services are running on correct ports

```bash
# Check if services are running
lsof -i :8080  # Spring Boot
lsof -i :8001  # FastAPI
```

### "Authorization header missing" Error
**Solution:** Run login request to get fresh token

### "Invalid environment variable" Error
**Solution:** Ensure environment is selected in top-right dropdown

### File Upload Fails
**Solution:**
- Check file is PDF format
- Verify file size < 10MB
- Ensure AWS credentials configured

### AI Features Return Errors
**Solution:**
- Check AI service is running
- Verify models are loaded: GET /api/v1/models/status
- Check API keys are configured

---

## Testing Tips

1. **Always Login First** - Most endpoints require authentication
2. **Use Variables** - Don't hardcode IDs, use `{{variableName}}`
3. **Check Environment** - Verify correct environment is selected
4. **Monitor Responses** - Check status codes and response bodies
5. **Review Logs** - Check backend logs for detailed errors
6. **Test Edge Cases** - Invalid data, missing fields, large files
7. **Cleanup After Testing** - Delete test data to avoid clutter

---

## Next Steps

### For Manual Testing
1. Follow workflows in `API_TESTING_GUIDE.md`
2. Use sample data from `TEST_DATA_EXAMPLES.md`
3. Test each functional area systematically

### For Automated Testing
1. Use Postman Collection Runner
2. Set up test scripts for validation
3. Configure CI/CD integration

### For Load Testing
1. Use Collection Runner with iterations
2. Add delays between requests
3. Monitor response times and errors

---

## Support

### Documentation
- API Testing Guide: `API_TESTING_GUIDE.md`
- Test Data: `TEST_DATA_EXAMPLES.md`
- Backend Config: `jobtracker-backend/src/main/resources/application.yml`
- AI Config: `ai-microservices/app/config/settings.py`

### Endpoints
- Spring Boot Health: http://localhost:8080/actuator/health
- FastAPI Health: http://localhost:8001/health
- FastAPI Docs: http://localhost:8001/docs

### Logs
- Spring Boot: `jobtracker-backend/logs/`
- FastAPI: `ai-microservices/ai_service.log`

---

## Project Structure

```
Job_Tracker_App/
├── Job_Tracker_Postman_Collection.json    # Import this first
├── Job_Tracker_Postman_Environment.json   # Import this second
├── POSTMAN_TESTING_README.md              # This file
├── API_TESTING_GUIDE.md                   # Detailed testing guide
├── TEST_DATA_EXAMPLES.md                  # Sample test data
├── jobtracker-backend/                    # Spring Boot backend
└── ai-microservices/                      # FastAPI AI services
```

---

## API Statistics

- **Total Endpoints**: 70+
- **Authentication Endpoints**: 6
- **Job-Related Endpoints**: 11
- **Application Endpoints**: 7
- **File Management Endpoints**: 7
- **Notification Endpoints**: 10
- **AI Microservice Endpoints**: 12
- **Other Endpoints**: 10+

---

## Version Information

- **Collection Version**: 2.1.0
- **Postman Schema**: v2.1.0
- **Last Updated**: October 25, 2025
- **Backend**: Spring Boot 3.5.5
- **AI Service**: FastAPI (Python 3.8+)

---

## Getting Help

### Check These First
1. Backend services are running
2. Correct environment is selected
3. Token is valid (login again if needed)
4. Request body matches examples
5. Backend logs for error details

### Common Solutions
- **401 Unauthorized**: Login to get new token
- **400 Bad Request**: Check request body format
- **404 Not Found**: Verify resource ID exists
- **500 Server Error**: Check backend logs

### Resources
- Postman Documentation: https://learning.postman.com/
- Spring Boot Docs: https://docs.spring.io/spring-boot/
- FastAPI Docs: https://fastapi.tiangolo.com/

---

## License

This testing suite is part of the Job Tracker Application project.

---

**Happy Testing!** 🚀

For detailed instructions, see `API_TESTING_GUIDE.md`
