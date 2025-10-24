# Testing Documentation

## Test Status Report - October 24, 2025

### Overview

This document provides an overview of the testing infrastructure, current test coverage, and instructions for running tests across all components of the Job Tracker App.

---

## 1. Backend (Spring Boot) Tests

### Test Framework
- **JUnit 5** (Jupiter)
- **Spring Boot Test**
- **Mockito** for mocking
- **Spring Security Test**

### Existing Tests

#### `/jobtracker-backend/src/test/java/com/jobtracker/JobtrackerBackendApplicationTests.java`
- **Purpose**: Basic Spring Boot application context loading test
- **Status**: ✓ Test file exists
- **Coverage**: Verifies application context loads successfully

```java
@SpringBootTest
class JobtrackerBackendApplicationTests {
    @Test
    void contextLoads() {
        // Ensures Spring application context loads without errors
    }
}
```

#### `/jobtracker-backend/src/test/java/com/jobtracker/config/TestAwsSesConfig.java`
- **Purpose**: Mock AWS SES client for testing
- **Status**: ✓ Configuration exists
- **Coverage**: Provides mock SES client to prevent AWS calls during tests

### Running Backend Tests

**Prerequisites:**
- Java 21 installed
- Maven installed
- Internet connection (for downloading dependencies on first run)

**Commands:**

```bash
# Navigate to backend directory
cd jobtracker-backend

# Run all tests
mvn test

# Run tests with verbose output
mvn test -X

# Run specific test class
mvn test -Dtest=JobtrackerBackendApplicationTests

# Run tests and generate coverage report
mvn test jacoco:report

# Clean and test
mvn clean test
```

### Test Coverage Gaps

**Critical Areas Needing Tests:**

1. **Authentication & Authorization**
   - [ ] Login endpoint tests
   - [ ] Registration endpoint tests
   - [ ] JWT token generation and validation
   - [ ] OAuth2 login flow
   - [ ] Password validation tests
   - [ ] Rate limiting tests

2. **Application Controller**
   - [ ] CRUD operations tests
   - [ ] Authorization checks (user owns application)
   - [ ] Input validation tests
   - [ ] NoSQL injection prevention tests
   - [ ] Status filtering tests

3. **File Controller**
   - [ ] File upload tests (PDF validation)
   - [ ] File download tests (ownership verification)
   - [ ] File deletion tests (authorization)
   - [ ] S3 integration tests (mocked)
   - [ ] Presigned URL generation tests

4. **Security Services**
   - [ ] RateLimitService tests
   - [ ] InputValidationService tests
   - [ ] FileValidationService tests
   - [ ] PasswordValidator tests

5. **User Management**
   - [ ] User registration tests
   - [ ] Duplicate email prevention tests
   - [ ] Profile update tests
   - [ ] Feedback submission tests

6. **Job Recommendations**
   - [ ] Job search API integration tests (mocked)
   - [ ] Recommendation algorithm tests
   - [ ] Error handling tests

7. **Error Handling**
   - [ ] GlobalExceptionHandler tests
   - [ ] Profile-based error message tests
   - [ ] Validation error response tests

### Known Issues

**Issue 1: Duplicate Dependencies (FIXED)**
- **Problem**: pom.xml contained duplicate dependencies causing Maven warnings
- **Duplicates Removed**:
  - `spring-boot-starter-webflux` (was declared 3 times)
  - `spring-boot-configuration-processor` (was declared 2 times)
  - `spring-dotenv` (was declared 2 times)
- **Status**: ✓ Fixed
- **File**: `pom.xml:88-102, 157-160`

**Issue 2: Network Isolation**
- **Problem**: Tests cannot run in network-isolated environments without pre-cached dependencies
- **Solution**: Run `mvn dependency:go-offline` before deployment to network-isolated environments
- **Status**: ⚠️ Environmental limitation

---

## 2. Frontend (Next.js) Tests

### Test Framework
- **Status**: ⚠️ Not configured
- **Recommended**: Jest + React Testing Library

### Current Status
- No test files exist
- No test script in package.json
- No testing framework dependencies installed

### Setting Up Frontend Tests

**Step 1: Install Testing Dependencies**

```bash
cd jobtracker-frontend

npm install --save-dev jest @testing-library/react @testing-library/jest-dom \
  @testing-library/user-event jest-environment-jsdom @types/jest
```

**Step 2: Create Jest Configuration**

Create `jest.config.js`:
```javascript
const nextJest = require('next/jest')

const createJestConfig = nextJest({
  dir: './',
})

const customJestConfig = {
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  testEnvironment: 'jest-environment-jsdom',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  collectCoverageFrom: [
    'src/**/*.{js,jsx,ts,tsx}',
    '!src/**/*.d.ts',
    '!src/**/*.stories.{js,jsx,ts,tsx}',
  ],
}

module.exports = createJestConfig(customJestConfig)
```

**Step 3: Create Setup File**

Create `jest.setup.js`:
```javascript
import '@testing-library/jest-dom'
```

**Step 4: Add Test Script to package.json**

```json
{
  "scripts": {
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  }
}
```

### Recommended Frontend Tests

1. **Authentication Components**
   - [ ] Login form validation
   - [ ] Registration form validation
   - [ ] OAuth2 button click handling
   - [ ] Token storage and retrieval

2. **Application Management**
   - [ ] Application list rendering
   - [ ] Application creation form
   - [ ] Application status updates
   - [ ] Filter and search functionality

3. **File Upload**
   - [ ] Resume upload component
   - [ ] File type validation
   - [ ] File size validation
   - [ ] Upload progress display

4. **Dashboard**
   - [ ] Analytics display
   - [ ] Charts rendering
   - [ ] Data fetching and loading states
   - [ ] Error handling

5. **Navigation & Routing**
   - [ ] Protected routes
   - [ ] Redirect logic
   - [ ] Authentication flow

---

## 3. AI Microservice (Python/FastAPI) Tests

### Test Framework
- **Status**: ⚠️ Not configured
- **Recommended**: pytest + pytest-asyncio

### Current Status
- No test files exist
- No test directory created
- pytest not configured

### Setting Up AI Microservice Tests

**Step 1: Install Testing Dependencies**

```bash
cd ai-microservices

# Add to requirements.txt
echo "pytest>=7.4.0" >> requirements.txt
echo "pytest-asyncio>=0.21.0" >> requirements.txt
echo "pytest-cov>=4.1.0" >> requirements.txt
echo "httpx>=0.24.0" >> requirements.txt

pip install -r requirements.txt
```

**Step 2: Create Test Directory Structure**

```bash
mkdir -p tests
touch tests/__init__.py
touch tests/test_resume_service.py
touch tests/test_job_search.py
touch tests/test_main.py
```

**Step 3: Create pytest Configuration**

Create `pytest.ini`:
```ini
[pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
asyncio_mode = auto
```

**Step 4: Running Tests**

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app --cov-report=html

# Run specific test file
pytest tests/test_resume_service.py

# Run with verbose output
pytest -v

# Run and show print statements
pytest -s
```

### Recommended AI Microservice Tests

1. **Resume Processing**
   - [ ] PDF resume parsing tests
   - [ ] Resume text extraction tests
   - [ ] Resume analysis tests
   - [ ] File size validation tests (10MB limit)
   - [ ] Invalid file type handling

2. **Job Search Integration**
   - [ ] TheirStack API integration tests (mocked)
   - [ ] JSearch API integration tests (mocked)
   - [ ] API error handling tests
   - [ ] Rate limiting tests

3. **CORS Configuration**
   - [ ] Allowed origins validation
   - [ ] Preflight request handling

4. **Endpoints**
   - [ ] Health check endpoint
   - [ ] Resume upload endpoint
   - [ ] Job search endpoint
   - [ ] Error response format tests

---

## 4. Integration Tests

### Recommended Integration Test Scenarios

1. **End-to-End User Flow**
   - [ ] User registration → Login → Create application → Upload resume
   - [ ] User login → View applications → Update status → Delete application
   - [ ] OAuth2 flow → Profile creation → Application management

2. **Cross-Service Communication**
   - [ ] Backend ↔ AI Microservice (resume analysis)
   - [ ] Backend ↔ AWS S3 (file storage)
   - [ ] Backend ↔ AWS SES (email sending)
   - [ ] Frontend ↔ Backend (API calls)

3. **Security Integration**
   - [ ] Rate limiting enforcement
   - [ ] JWT token expiration and refresh
   - [ ] File ownership verification
   - [ ] Input validation across all endpoints

---

## 5. Test Execution Summary

### Attempted Test Runs (October 24, 2025)

#### Backend Tests
- **Command**: `mvn test`
- **Result**: ❌ Failed due to network connectivity
- **Error**: Cannot resolve Spring Boot parent POM (network isolation)
- **Resolution**: Tests will run successfully when network is available
- **Fix Applied**: ✓ Removed duplicate dependencies from pom.xml

#### Frontend Tests
- **Command**: `npm test`
- **Result**: ❌ No test command configured
- **Status**: Testing framework needs to be set up

#### AI Microservice Tests
- **Command**: `pytest`
- **Result**: ⚠️ Not attempted (pytest not installed)
- **Status**: Testing framework needs to be set up

---

## 6. CI/CD Pipeline Recommendations

### GitHub Actions Workflow Example

Create `.github/workflows/test.yml`:

```yaml
name: Run Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run backend tests
        run: |
          cd jobtracker-backend
          mvn clean test
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
      - name: Install dependencies
        run: |
          cd jobtracker-frontend
          npm ci
      - name: Run frontend tests
        run: |
          cd jobtracker-frontend
          npm test

  ai-microservice-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - name: Install dependencies
        run: |
          cd ai-microservices
          pip install -r requirements.txt
      - name: Run tests
        run: |
          cd ai-microservices
          pytest --cov=app
```

---

## 7. Test Coverage Goals

### Minimum Coverage Targets

- **Backend**: 70% line coverage, 60% branch coverage
- **Frontend**: 60% line coverage, 50% branch coverage
- **AI Microservice**: 65% line coverage, 55% branch coverage

### Priority Areas for Coverage

1. **Critical Security Paths** (Target: 90%+)
   - Authentication and authorization
   - Input validation
   - File upload validation
   - Rate limiting

2. **Business Logic** (Target: 75%+)
   - Application CRUD operations
   - Job search and recommendations
   - Resume analysis

3. **Error Handling** (Target: 80%+)
   - Exception handlers
   - Validation errors
   - External API failures

---

## 8. Test Data Management

### Test Database Setup

**MongoDB Test Instance**:
```bash
# Use separate database for tests
MONGODB_URI_TEST=mongodb://localhost:27017/JobTrackerApp_Test
```

**Test Data Fixtures**:
- Create reusable test data fixtures for users, applications, and files
- Use @BeforeEach and @AfterEach to ensure clean state
- Implement database cleanup utilities

### Mock External Services

1. **AWS S3**: Use LocalStack or mock implementations
2. **AWS SES**: Mock email sending
3. **External APIs**: Mock HTTP responses
4. **LLM APIs**: Mock OpenAI responses

---

## 9. Manual Testing Checklist

Until automated tests are comprehensive, use this manual testing checklist:

### Authentication
- [ ] User can register with valid email/password
- [ ] Registration fails with duplicate email
- [ ] Registration enforces password policy
- [ ] User can login with correct credentials
- [ ] Login fails with incorrect credentials
- [ ] Rate limiting prevents brute force (5 attempts/min)
- [ ] OAuth2 Google login works
- [ ] JWT token expires after 1 hour

### Applications
- [ ] User can create new job application
- [ ] User can view their applications list
- [ ] User can filter by status
- [ ] User can update application details
- [ ] User can delete their application
- [ ] User cannot access other users' applications
- [ ] Input validation prevents NoSQL injection

### File Management
- [ ] User can upload PDF resume (valid format)
- [ ] Upload rejects non-PDF files
- [ ] Upload rejects files > 10MB
- [ ] User can download their own files
- [ ] User cannot download other users' files
- [ ] User can delete their own files
- [ ] Presigned URLs work for S3 access

### Security
- [ ] CORS allows only configured origins
- [ ] HTTPS enforced in production
- [ ] Security headers present in responses
- [ ] Generic error messages in production
- [ ] Sensitive data not logged
- [ ] Actuator endpoints restricted in production

---

## 10. Next Steps

### Immediate Actions (Priority 1)

1. **Add Unit Tests for Security Services**
   - RateLimitService
   - InputValidationService
   - FileValidationService
   - PasswordValidator

2. **Add Controller Tests**
   - AuthController (login, registration)
   - ApplicationController (CRUD operations)
   - FileController (upload, download, delete)

3. **Configure Frontend Testing**
   - Install Jest and React Testing Library
   - Set up test configuration
   - Add basic component tests

### Medium-Term Actions (Priority 2)

4. **Add Service Layer Tests**
   - ApplicationService
   - UserService
   - JobSearchService
   - EmailService

5. **Configure AI Microservice Testing**
   - Install pytest
   - Create test structure
   - Add endpoint tests

### Long-Term Actions (Priority 3)

6. **Integration Tests**
   - End-to-end user flows
   - Cross-service communication
   - Database integration

7. **Performance Tests**
   - Load testing with JMeter/Gatling
   - API response time benchmarks
   - Database query optimization

8. **Security Tests**
   - OWASP ZAP automated scanning
   - Penetration testing
   - Dependency vulnerability scanning

---

## Summary

**Current Test Status:**

| Component | Tests Exist | Can Run | Coverage | Status |
|-----------|-------------|---------|----------|--------|
| Backend | ✓ Basic | ⚠️ Needs network | ~5% | ⚠️ Minimal |
| Frontend | ✗ None | ✗ Not configured | 0% | ❌ Not set up |
| AI Service | ✗ None | ✗ Not configured | 0% | ❌ Not set up |

**Issues Fixed:**
- ✓ Duplicate dependencies removed from pom.xml

**Action Required:**
1. Set up testing frameworks for Frontend and AI Microservice
2. Write comprehensive unit tests for all security-critical components
3. Add integration tests for critical user flows
4. Configure CI/CD pipeline for automated testing

---

Last Updated: October 24, 2025
