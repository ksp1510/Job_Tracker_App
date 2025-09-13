"""
Pydantic settings for the AI microservice.

All configurable options for the microservice are defined here.  Environment variables
may be provided via a `.env` file in the repository root or via the host environment.
"""

from typing import Optional, List, Dict, Any
from pydantic_settings import BaseSettings
from pydantic import Field

class Settings(BaseSettings):
    """Application settings loaded from environment variables or a .env file."""
    # API Keys for external job search APIs.  No OpenAI keys are required
    # because this microservice runs exclusively on self‑hosted HuggingFace models.
    THEIRSTACK_API_KEY: Optional[str] = None
    JSEARCH_API_KEY: Optional[str] = None
    HF_TOKEN: Optional[str] = Field(None, alias="HF_TOKEN")
    
    # Model Configuration
    LLM_MODEL: str = "microsoft/DialoGPT-medium"   # switched from Llama
    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    
    # Job Search Configuration
    USE_MOCK_DATA: bool = True  # Start with mock, switch to real APIs
    MAX_JOBS_PER_SEARCH: int = 50
    
    # Vector Database
    VECTOR_DB_PATH: str = "./vector_db"
    CHUNK_SIZE: int = 1000
    CHUNK_OVERLAP: int = 200
    
    # File Processing
    MAX_FILE_SIZE_MB: int = 10
    ALLOWED_FILE_TYPES: list = [".pdf", ".docx", ".txt"]
    
    # Spring Boot Backend
    SPRING_BOOT_URL: str = "http://localhost:8080"
    
    # AWS S3 (for resume access)
    AWS_ACCESS_KEY_ID: Optional[str] = None
    AWS_SECRET_ACCESS_KEY: Optional[str] = None
    AWS_REGION: str = "us-east-1"
    AWS_S3_BUCKET: Optional[str] = None
    
    # Interview Configuration
    MAX_INTERVIEW_QUESTIONS: int = 10
    INTERVIEW_SESSION_TIMEOUT: int = 3600  # 1 hour
    
    # Performance
    MAX_CONCURRENT_REQUESTS: int = 10
    REQUEST_TIMEOUT: int = 30
    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FILE: str = "ai_service.log"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        populate_by_name = True  # <-- important for alias support
        extra = "allow"          # <-- allow extra env vars instead of throwing error

# Global settings instance
settings = Settings()

# API Configuration
class APIConfig:
    """Static helper for API endpoints and headers."""
    # Base URLs for third‑party APIs
    THEIRSTACK_BASE_URL = "https://api.theirstack.com/v1"
    JSEARCH_BASE_URL = "https://jsearch.p.rapidapi.com"
    # No OpenAI base URL because we do not call OpenAI

    @classmethod
    def get_job_search_headers(cls, api_name: str) -> Dict[str, str]:
        if api_name == "theirstack":
            return {
                "Authorization": f"Bearer {settings.THEIRSTACK_API_KEY}",
                "Content-Type": "application/json",
            }
        if api_name == "jsearch":
            return {
                "X-RapidAPI-Key": settings.JSEARCH_API_KEY or "",
                "X-RapidAPI-Host": "jsearch.p.rapidapi.com",
                "Content-Type": "application/json",
            }
        return {}


# Mock Data Configuration
class MockDataConfig:
    """Configuration for mock job data during development
    Sample data used during development when `USE_MOCK_DATA` is True."""
    # Example job postings
    SAMPLE_JOBS : List[Dict[str, Any]] = [
        {
            "id": "job_1",
            "title": "Senior Software Engineer",
            "company": "Tech Corp",
            "location": "San Francisco, CA",
            "salary_min": 120000,
            "salary_max": 180000,
            "description": """We are seeking a Senior Software Engineer to join our dynamic team. 
            Requirements: 5+ years of experience in Java, Spring Boot, microservices architecture, 
            AWS cloud services, and agile development. Experience with React and MongoDB preferred.""",
            "skills": ["Java", "Spring Boot", "AWS", "React", "MongoDB", "Microservices"],
            "employment_type": "Full-time",
            "remote": True,
            "posted_date": "2024-08-20"
        },
        {
            "id": "job_2", 
            "title": "Full Stack Developer",
            "company": "StartupXYZ",
            "location": "Austin, TX",
            "salary_min": 90000,
            "salary_max": 130000,
            "description": """Join our growing startup as a Full Stack Developer. Work with modern 
            technologies including Angular, Node.js, Python, and PostgreSQL. Experience with 
            Docker and Kubernetes is a plus.""",
            "skills": ["Angular", "Node.js", "Python", "PostgreSQL", "Docker", "Kubernetes"],
            "employment_type": "Full-time", 
            "remote": False,
            "posted_date": "2024-08-22"
        },
        {
            "id": "job_3",
            "title": "DevOps Engineer",
            "company": "CloudTech Solutions",
            "location": "New York, NY",
            "salary_min": 110000,
            "salary_max": 160000,
            "description": """We need a DevOps Engineer with expertise in CI/CD pipelines, 
            containerization, and cloud infrastructure. Required skills: AWS, Docker, Kubernetes, 
            Jenkins, Terraform, and monitoring tools like Prometheus.""",
            "skills": ["AWS", "Docker", "Kubernetes", "Jenkins", "Terraform", "CI/CD"],
            "employment_type": "Full-time",
            "remote": True,
            "posted_date": "2024-08-21"
        }
    ]

    # Example interview questions grouped by category
    SAMPLE_INTERVIEW_QUESTIONS: Dict[str, List[str]] = {
        "general": [
            "Tell me about yourself",
            "Why are you interested in this role?",
            "What are your greatest strengths?",
            "Describe a challenging project you worked on"
        ],
        "technical": [
            "Explain the difference between REST and GraphQL",
            "How do you handle error handling in microservices?",
            "Describe your experience with cloud platforms",
            "What is your approach to writing unit tests?"
        ],
        "behavioral": [
            "Describe a time when you had to work with a difficult team member",
            "How do you handle tight deadlines?",
            "Tell me about a time you made a mistake and how you handled it",
            "Describe your ideal work environment"
        ]
    }