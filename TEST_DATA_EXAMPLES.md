# Test Data Examples & Quick Reference

Quick reference guide with ready-to-use test data for API testing.

---

## Quick Start Test Data

### User Registration Data

#### User 1 - Valid Registration
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

#### User 2 - Valid Registration
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "password": "AnotherPass456!"
}
```

#### User 3 - Test User
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test.user@example.com",
  "password": "TestPass789!"
}
```

### Invalid Registration Data (for testing validation)

#### Missing Required Fields
```json
{
  "firstName": "John",
  "email": "john@example.com"
}
```
**Expected:** 400 Bad Request

#### Weak Password
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "weak"
}
```
**Expected:** 400 Bad Request - Password must be 8+ chars with uppercase, lowercase, number, special char

#### Invalid Email
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "invalid-email",
  "password": "SecurePass123!"
}
```
**Expected:** 400 Bad Request

---

## Application Test Data

### Application 1 - Software Engineer
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Senior Software Engineer",
  "location": "New York, NY",
  "status": "APPLIED",
  "applicationDate": "2025-10-20",
  "jobDescription": "We are seeking an experienced Senior Software Engineer to join our backend team. The ideal candidate will have 5+ years of experience with Python, Java, and microservices architecture.",
  "notes": "Applied through company website. Recruiter mentioned 2-week response time.",
  "salary": "$120,000 - $150,000",
  "jobUrl": "https://techcorp.com/careers/senior-engineer"
}
```

### Application 2 - Data Scientist
```json
{
  "companyName": "DataCo Analytics",
  "jobTitle": "Data Scientist",
  "location": "San Francisco, CA",
  "status": "INTERVIEW",
  "applicationDate": "2025-10-18",
  "interviewDate": "2025-11-01T14:00:00",
  "jobDescription": "Looking for a Data Scientist with expertise in machine learning, Python, and data visualization. Must have experience with large-scale data processing.",
  "notes": "Phone screen scheduled with hiring manager. Need to prepare ML case study.",
  "salary": "$130,000 - $160,000"
}
```

### Application 3 - Frontend Developer
```json
{
  "companyName": "WebDesign Inc",
  "jobTitle": "Frontend Developer",
  "location": "Remote",
  "status": "ASSESSMENT",
  "applicationDate": "2025-10-15",
  "assessmentDeadline": "2025-10-30T23:59:59",
  "jobDescription": "Frontend Developer position focusing on React and TypeScript. Building responsive, accessible web applications.",
  "notes": "Take-home coding challenge received. Due by Oct 30. Build a dashboard component.",
  "salary": "$100,000 - $130,000"
}
```

### Application 4 - DevOps Engineer
```json
{
  "companyName": "CloudSystems",
  "jobTitle": "DevOps Engineer",
  "location": "Seattle, WA",
  "status": "OFFER",
  "applicationDate": "2025-10-10",
  "offerDate": "2025-10-24",
  "jobDescription": "DevOps Engineer to manage cloud infrastructure, CI/CD pipelines, and container orchestration.",
  "notes": "Offer received! $140k base + $20k signing bonus. Need to respond by Oct 31.",
  "salary": "$140,000 + bonus"
}
```

### Application 5 - Product Manager
```json
{
  "companyName": "StartupXYZ",
  "jobTitle": "Senior Product Manager",
  "location": "Austin, TX",
  "status": "REJECTED",
  "applicationDate": "2025-10-05",
  "jobDescription": "Product Manager for B2B SaaS platform. Define product roadmap and work with engineering teams.",
  "notes": "Rejected after final round. Feedback: Strong technical skills but looking for more B2B experience.",
  "salary": "$130,000 - $150,000"
}
```

### Update Application Status Examples

#### Update to Interview Stage
```json
{
  "status": "INTERVIEW",
  "interviewDate": "2025-11-05T15:30:00",
  "notes": "Technical interview with engineering team. 1 hour, prepare for system design questions."
}
```

#### Update to Assessment Stage
```json
{
  "status": "ASSESSMENT",
  "assessmentDeadline": "2025-11-08T23:59:59",
  "notes": "Coding challenge: Build a REST API with authentication. Est. 4-6 hours."
}
```

#### Update to Offer Stage
```json
{
  "status": "OFFER",
  "offerDate": "2025-10-25",
  "salary": "$145,000 + 10% bonus + equity",
  "notes": "Offer received! Benefits include health insurance, 401k match, unlimited PTO. Decision deadline: Nov 3."
}
```

---

## Notification Test Data

### Interview Reminder
```json
{
  "applicationId": "{{applicationId}}",
  "interviewDate": "2025-11-01T14:00:00",
  "customMessage": "Technical phone screen with Sarah Chen (Engineering Manager). Review system design patterns."
}
```

### Assessment Deadline Reminder
```json
{
  "applicationId": "{{applicationId}}",
  "assessmentDeadline": "2025-10-30T23:59:59",
  "customMessage": "Coding challenge due! Build REST API with user authentication. Estimated 4-6 hours."
}
```

### Custom Notification - Follow Up
```json
{
  "applicationId": "{{applicationId}}",
  "message": "Follow up with recruiter if no response received",
  "notifyAt": "2025-11-03T10:00:00",
  "type": "CUSTOM"
}
```

### Custom Notification - Application Deadline
```json
{
  "message": "Application deadline for Google SWE position",
  "notifyAt": "2025-10-28T23:59:00",
  "type": "CUSTOM"
}
```

### Notification Preferences
```json
{
  "emailEnabled": true,
  "inAppEnabled": true
}
```

---

## Job Search Query Parameters

### Search 1 - Software Engineer in New York
```
query=software engineer
location=New York
jobType=FULL_TIME
minSalary=100000
maxSalary=150000
skills=Python,Java,React
page=0
size=12
```

### Search 2 - Remote Data Science
```
query=data scientist
location=Remote
jobType=FULL_TIME
minSalary=120000
skills=Python,Machine Learning,TensorFlow
page=0
size=20
```

### Search 3 - Entry Level Developer
```
query=junior developer
location=San Francisco
jobType=FULL_TIME,INTERNSHIP
maxSalary=90000
skills=JavaScript,React
page=0
size=15
```

### Search 4 - DevOps Engineer
```
query=devops engineer
location=Seattle
jobType=FULL_TIME
minSalary=110000
maxSalary=160000
skills=AWS,Docker,Kubernetes
page=0
size=10
```

---

## AI Job Search Request Data

### AI Job Search 1 - Senior Python Developer
```json
{
  "user_id": "user123",
  "query": "senior python developer",
  "location": "San Francisco, CA",
  "skills": ["Python", "Django", "PostgreSQL", "Redis"],
  "experience_level": "senior",
  "salary_min": 120000,
  "salary_max": 180000,
  "remote_only": false,
  "employment_type": "full-time",
  "use_resume_matching": false,
  "limit": 20
}
```

### AI Job Search 2 - Remote Data Engineer
```json
{
  "user_id": "user456",
  "query": "data engineer",
  "location": "Remote",
  "skills": ["Python", "Spark", "AWS", "SQL"],
  "experience_level": "mid",
  "salary_min": 100000,
  "salary_max": 150000,
  "remote_only": true,
  "employment_type": "full-time",
  "use_resume_matching": true,
  "resume_file_id": "resume_123",
  "limit": 30
}
```

### AI Job Search 3 - Frontend Developer
```json
{
  "user_id": "user789",
  "query": "frontend developer",
  "location": "New York, NY",
  "skills": ["React", "TypeScript", "CSS", "JavaScript"],
  "experience_level": "junior",
  "salary_min": 70000,
  "salary_max": 100000,
  "remote_only": false,
  "employment_type": "full-time",
  "company_size": "startup",
  "limit": 25
}
```

---

## Resume Test Data

### Sample Resume 1 - Software Engineer
```
JOHN DOE
Senior Software Engineer
john.doe@example.com | (555) 123-4567 | LinkedIn: linkedin.com/in/johndoe

PROFESSIONAL SUMMARY
Results-driven Senior Software Engineer with 7+ years of experience in full-stack development,
specializing in microservices architecture, cloud technologies, and agile methodologies.
Proven track record of leading teams and delivering scalable solutions.

TECHNICAL SKILLS
Languages: Python, Java, JavaScript, TypeScript, SQL, Go
Frameworks: Spring Boot, Django, React, Node.js, Flask
Databases: PostgreSQL, MongoDB, Redis, MySQL
DevOps: Docker, Kubernetes, AWS (EC2, S3, Lambda, RDS), Jenkins, GitLab CI
Tools: Git, JIRA, Confluence, Agile/Scrum

PROFESSIONAL EXPERIENCE

Senior Software Engineer | Tech Corp | New York, NY | Jan 2020 - Present
• Led team of 5 engineers in developing microservices-based e-commerce platform serving 2M+ users
• Improved system performance by 40% through Redis caching and database query optimization
• Implemented CI/CD pipeline reducing deployment time from 2 hours to 15 minutes
• Architected and deployed AWS infrastructure supporting 10K+ requests per second
• Technologies: Python, Java, Spring Boot, React, PostgreSQL, Docker, Kubernetes, AWS

Software Engineer | StartupXYZ | San Francisco, CA | Jun 2018 - Dec 2019
• Developed RESTful APIs serving 1M+ daily requests with 99.9% uptime
• Built responsive frontend using React and TypeScript, improving user engagement by 30%
• Collaborated with product team using agile methodologies to deliver features on time
• Implemented automated testing achieving 85% code coverage
• Technologies: Python, Django, React, MongoDB, Redis, AWS

Junior Developer | WebSolutions Inc | Remote | Aug 2016 - May 2018
• Developed and maintained web applications for 15+ clients
• Created reusable React components reducing development time by 25%
• Performed code reviews and mentored 2 junior developers
• Technologies: JavaScript, React, Node.js, MySQL

EDUCATION
Bachelor of Science in Computer Science | State University | 2016
GPA: 3.7/4.0 | Dean's List 2014-2016

CERTIFICATIONS
• AWS Certified Solutions Architect - Associate (2022)
• Certified Kubernetes Administrator (CKA) (2021)
• Oracle Certified Professional, Java SE 11 Developer (2020)

PROJECTS
• Open Source Contributor: Contributed to Django REST Framework (50+ commits)
• Personal Project: Built job tracking application using Spring Boot and React (GitHub: 200+ stars)

ACHIEVEMENTS
• Tech Corp Excellence Award for Outstanding Performance (2022)
• Led migration to microservices architecture, reducing infrastructure costs by 30%
• Presented at local tech meetup on "Scaling Microservices with Kubernetes" (2023)
```

### Sample Resume 2 - Data Scientist
```
JANE SMITH
Data Scientist
jane.smith@email.com | (555) 987-6543

SUMMARY
Data Scientist with 5+ years of experience in machine learning, statistical analysis, and data visualization.
Expert in Python, R, and SQL with a strong background in predictive modeling and A/B testing.

SKILLS
Languages: Python, R, SQL, Scala
ML/AI: Scikit-learn, TensorFlow, PyTorch, Keras, XGBoost
Data Tools: Pandas, NumPy, Spark, Hadoop, Airflow
Visualization: Tableau, Matplotlib, Seaborn, Plotly
Cloud: AWS (SageMaker, EMR, Redshift), GCP

EXPERIENCE

Senior Data Scientist | DataCo | San Francisco, CA | 2021 - Present
• Built recommendation system increasing user engagement by 35%
• Developed churn prediction model with 89% accuracy, saving $2M annually
• Led team of 3 data scientists on ML pipeline optimization project
• Technologies: Python, TensorFlow, Spark, AWS SageMaker

Data Scientist | Analytics Inc | Boston, MA | 2019 - 2021
• Created customer segmentation models using clustering algorithms
• Performed A/B testing for product features, improving conversion by 20%
• Built real-time dashboards for executive team using Tableau
• Technologies: Python, R, SQL, Tableau

EDUCATION
Master of Science in Data Science | MIT | 2019
Bachelor of Science in Statistics | Boston University | 2017

CERTIFICATIONS
• Google Cloud Professional Data Engineer (2022)
• AWS Certified Machine Learning - Specialty (2021)
```

### Sample Resume 3 - Frontend Developer
```
ALEX JOHNSON
Frontend Developer
alex.johnson@email.com | Portfolio: alexjohnson.dev

SUMMARY
Creative Frontend Developer with 4 years of experience building responsive, accessible web applications.
Passionate about user experience and modern web technologies.

SKILLS
Languages: JavaScript, TypeScript, HTML5, CSS3
Frameworks: React, Next.js, Vue.js, Angular
Styling: Tailwind CSS, SASS, Styled Components
Tools: Git, Webpack, Vite, Jest, Cypress
Other: Responsive Design, Web Accessibility (WCAG), RESTful APIs

EXPERIENCE

Frontend Developer | WebDesign Inc | Remote | 2022 - Present
• Developed responsive web applications for 10+ clients
• Improved website performance by 50% using code splitting and lazy loading
• Implemented accessibility features achieving WCAG 2.1 Level AA compliance
• Technologies: React, TypeScript, Next.js, Tailwind CSS

Junior Frontend Developer | Creative Agency | Los Angeles, CA | 2020 - 2022
• Built interactive landing pages increasing conversion rates by 25%
• Collaborated with designers to implement pixel-perfect UI components
• Maintained and updated 20+ client websites
• Technologies: React, JavaScript, CSS3, HTML5

EDUCATION
Bachelor of Arts in Web Design | Design University | 2020

PROJECTS
• Open Source: Contributed to React component library (500+ GitHub stars)
• Personal: Built portfolio website with Next.js and Tailwind CSS
```

---

## Resume Analysis Request Data

### Full Resume Analysis
```json
{
  "user_id": "user123",
  "resume_text": "[Insert resume text from samples above]",
  "job_description": "We are seeking a Senior Software Engineer with 5+ years of experience in Python, microservices architecture, and team leadership. Experience with cloud platforms (AWS/GCP) and CI/CD is highly desirable. The ideal candidate will have strong problem-solving skills and experience scaling systems.",
  "analysis_type": "full"
}
```

### Skills Analysis Only
```json
{
  "user_id": "user123",
  "resume_text": "[Insert resume text]",
  "analysis_type": "skills"
}
```

### Keywords Extraction
```json
{
  "user_id": "user123",
  "resume_text": "[Insert resume text]",
  "job_description": "[Insert job description]",
  "analysis_type": "keywords"
}
```

---

## Resume Optimization Request Data

### Optimize for Senior Software Engineer
```json
{
  "user_id": "user123",
  "resume_text": "[Insert current resume text]",
  "job_description": "We are seeking a Senior Software Engineer with 5+ years of experience in Python, microservices architecture, and team leadership. The ideal candidate will have:\n\n- Strong experience with Python, Java, or Go\n- Expertise in microservices architecture and distributed systems\n- Experience with cloud platforms (AWS, GCP, or Azure)\n- Knowledge of containerization (Docker, Kubernetes)\n- Experience with CI/CD pipelines\n- Strong communication and leadership skills\n- Bachelor's degree in Computer Science or related field\n\nNice to have:\n- Experience with message queues (Kafka, RabbitMQ)\n- Knowledge of system design and architecture patterns\n- Open source contributions\n- Experience mentoring junior engineers"
}
```

---

## Interview Questions Request Data

### Technical Interview - Medium Difficulty
```json
{
  "user_id": "user123",
  "resume_text": "[Insert resume text for Senior Software Engineer]",
  "job_description": "Senior Software Engineer position focusing on backend development with Python and microservices",
  "job_title": "Senior Software Engineer",
  "company_name": "Tech Corp",
  "interview_type": "technical",
  "difficulty_level": "medium",
  "num_questions": 10
}
```

### Behavioral Interview - Easy Difficulty
```json
{
  "user_id": "user456",
  "resume_text": "[Insert resume text]",
  "job_description": "Looking for a team player who can collaborate effectively",
  "job_title": "Software Engineer",
  "company_name": "StartupXYZ",
  "interview_type": "behavioral",
  "difficulty_level": "easy",
  "num_questions": 8
}
```

### System Design Interview - Hard Difficulty
```json
{
  "user_id": "user789",
  "resume_text": "[Insert senior level resume]",
  "job_description": "Principal Engineer role requiring expertise in large-scale distributed systems",
  "job_title": "Principal Engineer",
  "company_name": "BigTech Co",
  "interview_type": "system_design",
  "difficulty_level": "hard",
  "num_questions": 5
}
```

---

## Mock Interview Request Data

### Start Mock Interview - Technical
```json
{
  "user_id": "user123",
  "resume_text": "[Insert resume text]",
  "job_description": "Senior Software Engineer with Python, microservices, and cloud experience",
  "job_title": "Senior Software Engineer",
  "company_name": "Tech Corp",
  "interview_duration": 30,
  "interview_type": "technical"
}
```

### Mock Interview Answer Examples

#### Good Technical Answer
```
In my previous role at Tech Corp, I implemented a Redis caching layer for our API that significantly improved performance. The system was handling 10,000+ requests per second, and response times were averaging 500ms.

I analyzed the data access patterns and identified that 80% of requests were for the same data. I implemented a two-tier caching strategy: an in-memory L1 cache for frequently accessed data and Redis for L2 cache.

The results were impressive - we reduced average response time to 50ms (a 90% improvement) and decreased database load by 70%. I also implemented cache invalidation strategies to ensure data consistency.

The key lesson was the importance of measuring first, then optimizing based on data rather than assumptions.
```

#### Good Behavioral Answer
```
I'll give you an example from my time leading the microservices migration project. We had a conflict between the frontend and backend teams regarding API design.

The frontend team wanted a specific JSON structure that was convenient for their components, but the backend team argued it would create inefficient database queries and coupling.

I organized a meeting where both teams could present their perspectives. I facilitated a discussion focusing on the underlying needs rather than specific solutions. We discovered that the real issue was the lack of a clear API contract and documentation.

We agreed to:
1. Use OpenAPI/Swagger for API documentation
2. Design APIs based on domain models, not UI requirements
3. Implement a BFF (Backend for Frontend) layer for UI-specific needs

This resolved the immediate conflict and improved our overall development process. The teams appreciated being heard and involved in the solution.
```

---

## Feedback Test Data

### Feature Request Feedback
```json
{
  "type": "FEATURE_REQUEST",
  "message": "Would love to see integration with LinkedIn for importing job applications automatically. This would save a lot of manual data entry time.",
  "email": "user@example.com"
}
```

### Bug Report Feedback
```json
{
  "type": "BUG",
  "message": "When uploading a resume PDF larger than 5MB, the upload fails silently without showing an error message. Steps to reproduce: 1. Go to application page, 2. Click upload resume, 3. Select large PDF, 4. No error shown but file not uploaded.",
  "email": "user@example.com"
}
```

### General Feedback
```json
{
  "type": "GENERAL",
  "message": "Love the application! The notification system is really helpful for tracking interview dates. The UI is clean and intuitive.",
  "email": "user@example.com"
}
```

---

## Saved Job Test Data

### Save Job with Notes
```json
{
  "notes": "Great opportunity! Company culture seems excellent based on Glassdoor reviews. Salary range matches expectations. Applied 10/25, expecting response within 2 weeks."
}
```

### Save Job - Minimal
```json
{
  "notes": "Interesting position, need to research company more"
}
```

---

## Interview Prep Test Data

### Behavioral Question
```json
{
  "question": "Tell me about a time you had to deal with a difficult team member",
  "answer": "In my previous role, I worked with a senior developer who was resistant to code reviews. I scheduled a one-on-one to understand their concerns. They felt reviews were criticizing their work. I explained that code reviews help everyone learn and catch bugs early. I also started highlighting positive aspects in reviews. Over time, they became more receptive and even started requesting reviews.",
  "category": "BEHAVIORAL"
}
```

### Technical Question
```json
{
  "question": "Explain the difference between SQL and NoSQL databases and when you would use each",
  "answer": "SQL databases (like PostgreSQL) use structured schemas and ACID transactions. They're great for complex queries and relationships. NoSQL databases (like MongoDB) are more flexible and scale horizontally easily. I use SQL for transactional data requiring consistency (e.g., financial data) and NoSQL for unstructured data or when horizontal scaling is critical (e.g., user activity logs, product catalogs).",
  "category": "TECHNICAL"
}
```

### System Design Question
```json
{
  "question": "Design a URL shortening service like bit.ly",
  "answer": "Key components:\n1. URL shortening: Generate short code using base62 encoding or hashing\n2. Storage: NoSQL database (DynamoDB/Cassandra) for scalability\n3. Redirect service: Simple lookup and 301 redirect\n4. Analytics: Queue-based system (Kafka) for tracking clicks\n5. Caching: Redis for hot URLs\n6. API rate limiting to prevent abuse\n\nScale considerations: Sharding by hash of short code, CDN for static content, geographic distribution for low latency.",
  "category": "SYSTEM_DESIGN"
}
```

---

## Common Test Scenarios

### Test Scenario 1: Rate Limiting
**Objective:** Verify login rate limiting (5 requests per minute)

1. Make 5 login requests within 1 minute
2. Make 6th login request
3. **Expected:** 429 Too Many Requests

### Test Scenario 2: Authorization
**Objective:** Verify users can only access their own data

1. Register User A and login
2. Create application for User A
3. Register User B and login
4. Try to access User A's application using User B's token
5. **Expected:** 403 Forbidden or empty result

### Test Scenario 3: Input Validation
**Objective:** Verify NoSQL injection prevention

1. Try to create application with malicious input:
```json
{
  "companyName": "{ $ne: null }",
  "jobTitle": "Engineer"
}
```
2. **Expected:** Input sanitized or rejected

### Test Scenario 4: File Upload Validation
**Objective:** Verify file type and size restrictions

1. Try to upload non-PDF file
2. **Expected:** 400 Bad Request
3. Try to upload PDF > 10MB
4. **Expected:** 413 Payload Too Large

---

## Response Status Codes Reference

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Delete successful |
| 400 | Bad Request | Invalid request body, validation failed |
| 401 | Unauthorized | Missing or invalid auth token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (duplicate) |
| 413 | Payload Too Large | File size exceeds limit |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error |

---

## Tips for Effective Testing

1. **Use Variables** - Always use `{{variableName}}` instead of hardcoding IDs
2. **Check Auto-Saved Variables** - Login, Register, Create requests auto-save IDs
3. **Test Edge Cases** - Empty strings, very long strings, special characters
4. **Test Error Handling** - Invalid data, missing fields, wrong data types
5. **Verify Response Schema** - Check all expected fields are present
6. **Check Response Times** - APIs should respond within 2 seconds
7. **Test Pagination** - Try different page sizes, last page, invalid page numbers
8. **Cleanup After Tests** - Delete test data to avoid clutter

---

**Last Updated:** October 25, 2025
