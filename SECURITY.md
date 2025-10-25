# Security Documentation

## Overview

This document outlines all security measures implemented in the Job Tracker App, including configuration requirements, best practices, and production deployment guidelines.

## Table of Contents

1. [Security Features](#security-features)
2. [Environment Variables](#environment-variables)
3. [Production Deployment](#production-deployment)
4. [Security Best Practices](#security-best-practices)
5. [Vulnerability Fixes](#vulnerability-fixes)

---

## Security Features

### 1. Authentication & Authorization

**JWT-based Authentication**
- Tokens expire after 1 hour (configurable)
- UTF-8 encoding to prevent character encoding attacks
- Secure cookie settings with HttpOnly and SameSite=Strict
- Location: `jobtracker-backend/src/main/java/com/jobtracker/config/JwtTokenProvider.java:18`

**Password Security**
- BCrypt hashing with strength 12
- Password policy enforcement:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one number
  - At least one special character
- Location: `jobtracker-backend/src/main/java/com/jobtracker/util/PasswordValidator.java:13`

**OAuth2 Integration**
- Google OAuth2 login support
- Secure redirect URIs
- Support for multiple authentication providers
- Location: `jobtracker-backend/src/main/java/com/jobtracker/config/OAuth2SuccessHandler.java:28`

### 2. Rate Limiting

**Brute Force Protection**
- Login attempts: 5 per minute per IP address
- Registration attempts: 10 per hour per IP address
- General API: 100 requests per minute per IP address
- Supports X-Forwarded-For header for proxy detection
- Location: `jobtracker-backend/src/main/java/com/jobtracker/config/RateLimitService.java:13`

**Implementation:**
```java
// Login rate limiting
Bucket bucket = rateLimitService.resolveBucketForLogin(clientIp);
if (!bucket.tryConsume(1)) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body("Too many login attempts. Please try again later");
}
```

### 3. Input Validation

**NoSQL Injection Prevention**
- Sanitizes all user inputs
- Blocks MongoDB operators ($where, $regex, etc.)
- Validates status values against whitelist
- Location: `jobtracker-backend/src/main/java/com/jobtracker/service/InputValidationService.java:10`

**File Upload Validation**
- PDF magic byte verification (%PDF)
- File size limits (10MB max)
- Content-type validation
- Location: `jobtracker-backend/src/main/java/com/jobtracker/util/FileValidationService.java:16`

### 4. Authorization Controls

**IDOR Prevention**
- File ownership verification on download: `jobtracker-backend/src/main/java/com/jobtracker/controller/FileController.java:67`
- File ownership verification on delete: `jobtracker-backend/src/main/java/com/jobtracker/controller/FileController.java:94`
- Presigned URL ownership verification: `jobtracker-backend/src/main/java/com/jobtracker/controller/FileController.java:127`

**Implementation:**
```java
// Verify user owns the file
if (!file.getUserId().equals(userId)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body("Access denied: You don't have permission to access this file");
}
```

### 5. CORS Configuration

**Development:**
- Allows multiple localhost ports (3000, 4200, 8080, 8001)
- Location: `application-dev.yml:17`

**Production:**
- Configurable via `CORS_ALLOWED_ORIGINS` environment variable
- No wildcard origins allowed
- Location: `application-prod.yml:13`

### 6. Information Disclosure Prevention

**Profile-based Error Messages**
- Development: Detailed error messages with class names
- Production: Generic error messages
- All errors logged server-side with full stack traces
- Location: `jobtracker-backend/src/main/java/com/jobtracker/exception/GlobalExceptionHandler.java:107`

**Implementation:**
```java
if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
    message = "An unexpected error occurred. Please try again later.";
} else {
    message = "Error: " + ex.getClass().getSimpleName();
}
```

### 7. Security Headers

**Implemented Headers:**
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security: max-age=31536000; includeSubDomains
- Content-Security-Policy: default-src 'self'
- Location: `jobtracker-backend/src/main/java/com/jobtracker/config/SecurityConfig.java:97`

### 8. HTTPS/SSL

**Production Requirements:**
- SSL enabled by default in production profile
- Configurable key store path and password
- Supports PKCS12 and JKS formats
- HTTP to HTTPS redirect enforced
- Location: `application-prod.yml:3`

### 9. Actuator Security

**Endpoint Restrictions:**
- Development: health, info, metrics, env, beans
- Production: health, info only
- Health details hidden in production
- Location: `application-prod.yml:25`

### 10. Logging Security

**Best Practices:**
- No sensitive data logged (passwords, tokens, etc.)
- Profile-based log levels (DEBUG in dev, WARN/ERROR in prod)
- Location: `application-prod.yml:7`

---

## Environment Variables

### Required for Production

#### Backend (Spring Boot)

```bash
# MongoDB
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/

# JWT
JWT_SECRET=your-secure-secret-key-minimum-256-bits

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# AWS
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-s3-bucket-name
AWS_SES_FROM_EMAIL=noreply@yourdomain.com
AWS_SES_TEST_EMAIL=test@yourdomain.com

# External APIs
SERPAPI_KEY=your-serpapi-key
RAPIDAPI_KEY=your-rapidapi-key

# Security
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
SPRING_PROFILES_ACTIVE=prod

# SSL/HTTPS
SSL_ENABLED=true
SSL_KEY_STORE=/path/to/keystore.p12
SSL_KEY_STORE_PASSWORD=your-keystore-password
SSL_KEY_STORE_TYPE=PKCS12
```

#### Frontend (Next.js)

```bash
# API URL
NEXT_PUBLIC_API_URL=https://api.yourdomain.com

# OAuth2 Redirect
NEXT_PUBLIC_OAUTH2_REDIRECT_URI=https://yourdomain.com/auth/callback
```

#### AI Microservice (FastAPI)

```bash
# OpenAI/LLM
OPENAI_API_KEY=your-openai-api-key
LLM_MODEL=gpt-4

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://api.yourdomain.com

# Job Search APIs
THEIRSTACK_API_KEY=your-theirstack-api-key
JSEARCH_API_KEY=your-jsearch-api-key
```

### Optional Configuration

```bash
# Job Seeding
JOB_SEED_ENABLED=false  # Set to false in production

# Rate Limiting (uses defaults if not set)
RATE_LIMIT_LOGIN_PER_MINUTE=5
RATE_LIMIT_REGISTRATION_PER_HOUR=10
RATE_LIMIT_API_PER_MINUTE=100
```

---

## Production Deployment

### 1. SSL Certificate Setup

**Option A: Let's Encrypt (Recommended)**

```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate
sudo certbot certonly --standalone -d yourdomain.com

# Convert to PKCS12 format
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out keystore.p12 \
  -name tomcat
```

**Option B: Commercial Certificate**

Follow your certificate provider's instructions and convert to PKCS12 format.

### 2. Database Security

**MongoDB Atlas Configuration:**
- Enable IP whitelist
- Use strong passwords (generated, not manual)
- Enable database encryption at rest
- Configure VPC peering for additional security
- Enable MongoDB audit logs

### 3. AWS Security

**S3 Bucket Configuration:**
```bash
# Block public access
aws s3api put-public-access-block \
  --bucket your-bucket-name \
  --public-access-block-configuration \
  "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket your-bucket-name \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket your-bucket-name \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

**IAM Policies:**
- Use principle of least privilege
- Create separate IAM users for different services
- Enable MFA for AWS console access
- Rotate access keys regularly

### 4. Application Deployment

**Docker Deployment (Recommended):**

```dockerfile
# Backend Dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080

# Run as non-root user
RUN useradd -m -u 1000 appuser
USER appuser

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

**Environment Variables:**
```bash
# Use secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)
# NEVER commit .env files to version control
```

### 5. Reverse Proxy (Nginx)

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 6. Monitoring & Alerting

**Log Aggregation:**
- Use ELK Stack (Elasticsearch, Logstash, Kibana)
- Or CloudWatch Logs for AWS deployments

**Security Monitoring:**
- Monitor failed login attempts
- Track rate limit violations
- Alert on unusual API usage patterns
- Monitor file upload sizes and frequencies

---

## Security Best Practices

### Development

1. **Never commit secrets** to version control
   - Use `.env` files (add to `.gitignore`)
   - Use environment variables
   - Consider git-secrets tool

2. **Use HTTPS in development**
   - Test SSL configuration locally
   - Use mkcert for local SSL certificates

3. **Keep dependencies updated**
   ```bash
   # Backend
   mvn versions:display-dependency-updates

   # Frontend
   npm audit
   npm audit fix

   # AI Microservice
   pip list --outdated
   ```

4. **Code review security checklist**
   - Input validation on all user inputs
   - Authorization checks on all protected resources
   - No hardcoded credentials
   - Proper error handling
   - Secure password handling

### Production

1. **Regular security audits**
   - Quarterly penetration testing
   - Automated security scanning (OWASP ZAP, Burp Suite)
   - Dependency vulnerability scanning

2. **Incident response plan**
   - Document breach response procedures
   - Maintain contact list
   - Regular backup and recovery testing

3. **Database backups**
   - Daily automated backups
   - Test restore procedures monthly
   - Store backups in separate region

4. **Rate limiting tuning**
   - Monitor normal usage patterns
   - Adjust limits based on legitimate traffic
   - Implement progressive delays for repeated violations

5. **Security headers verification**
   ```bash
   # Test security headers
   curl -I https://yourdomain.com | grep -E '(X-Frame|X-Content|X-XSS|Strict-Transport)'
   ```

### API Security

1. **API versioning**
   - All endpoints versioned (/api/v1/)
   - Maintain backward compatibility
   - Deprecation notices with timeline

2. **Input validation**
   - Server-side validation (never trust client)
   - Whitelist approach for allowed values
   - Sanitize all inputs before database operations

3. **Output encoding**
   - Escape user-generated content
   - Use Content-Type headers correctly
   - Prevent XSS in API responses

---

## Vulnerability Fixes

### Critical Vulnerabilities (Fixed)

1. **CORS Wildcard Origins**
   - **Before:** `allow_origins=["*"]`
   - **After:** Configurable origins via `CORS_ALLOWED_ORIGINS`
   - **Files:** `SecurityConfig.java:57`, `ai-microservices/app/main.py:73`

2. **IDOR File Access**
   - **Before:** No ownership verification
   - **After:** User ownership check on all file operations
   - **Files:** `FileController.java:67,94,127`

3. **NoSQL Injection**
   - **Before:** Direct user input to database queries
   - **After:** Input sanitization and MongoDB operator blocking
   - **Files:** `InputValidationService.java:10`, `ApplicationController.java:70`

4. **Weak Password Policy**
   - **Before:** Minimum 6 characters only
   - **After:** Complex password requirements enforced
   - **Files:** `PasswordValidator.java:13`

5. **Missing Rate Limiting**
   - **Before:** No brute force protection
   - **After:** IP-based rate limiting with Bucket4j
   - **Files:** `RateLimitService.java:13`, `AuthController.java:42`

### High Vulnerabilities (Fixed)

6. **Information Disclosure**
   - **Before:** Detailed error messages exposed to clients
   - **After:** Generic messages in production, detailed logging server-side
   - **Files:** `GlobalExceptionHandler.java:107`

7. **Insecure File Upload**
   - **Before:** No file validation
   - **After:** Magic byte verification and size limits
   - **Files:** `FileValidationService.java:16`, `ai-microservices/app/main.py:139`

8. **Missing Security Headers**
   - **Before:** No security headers configured
   - **After:** Full suite of security headers
   - **Files:** `SecurityConfig.java:97`

9. **Insecure Cookies**
   - **Before:** Default cookie settings
   - **After:** HttpOnly, Secure, SameSite=Strict
   - **Files:** `JwtTokenProvider.java:18`

10. **No HTTPS Enforcement**
    - **Before:** HTTP allowed in production
    - **After:** HTTPS required, HTTP redirects to HTTPS
    - **Files:** `application-prod.yml:3`

### Medium Vulnerabilities (Fixed)

11. **Exposed Actuator Endpoints**
    - **Before:** All endpoints exposed
    - **After:** Only health and info in production
    - **Files:** `application-prod.yml:25`

12. **Sensitive Data Logging**
    - **Before:** Passwords logged in error messages
    - **After:** Sensitive fields excluded from logs
    - **Files:** `AuthService.java`, `UserService.java`

13. **Hardcoded Credentials**
    - **Before:** Email addresses hardcoded in config
    - **After:** All values from environment variables
    - **Files:** `application.yml:46`

---

## Security Contact

For security issues, please contact:
- Email: security@yourdomain.com
- Report vulnerabilities: https://github.com/yourusername/Job_Tracker_App/security/advisories/new

## Security Updates

This document is maintained alongside security implementations. Last updated: 2025-10-24

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
