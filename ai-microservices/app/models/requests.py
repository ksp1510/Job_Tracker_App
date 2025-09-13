from pydantic import BaseModel, Field, model_validator
from typing import List, Optional, Dict, Any
from enum import Enum


class JobSearchType(str, Enum):
    KEYWORD = "keyword"
    SKILLS_BASED = "skills_based"
    LOCATION_BASED = "location_based"
    SALARY_BASED = "salary_based"


class ExperienceLevel(str, Enum):
    ENTRY = "entry"
    MID = "mid"
    SENIOR = "senior"
    LEAD = "lead"
    EXECUTIVE = "executive"


class InterviewType(str, Enum):
    TECHNICAL = "technical"
    BEHAVIORAL = "behavioral"
    GENERAL = "general"
    MIXED = "mixed"


# ============ JOB SEARCH REQUESTS ============
class JobSearchRequest(BaseModel):
    user_id: str = Field(..., description="User ID from Spring Boot backend")
    query: str = Field(..., min_length=1, description="Job search query")
    location: Optional[str] = None
    skills: Optional[List[str]] = []
    experience_level: Optional[ExperienceLevel] = None
    salary_min: Optional[int] = Field(None, ge=0)
    salary_max: Optional[int] = Field(None, ge=0)
    remote_only: bool = False
    employment_type: Optional[str] = None
    company_size: Optional[str] = None
    use_resume_matching: bool = True
    resume_file_id: Optional[str] = None
    resume_text: Optional[str] = None
    limit: int = Field(default=20, ge=1, le=100)

    @model_validator(mode="after")
    def check_salary_range(cls, values):
        min_salary = values.salary_min
        max_salary = values.salary_max
        if min_salary and max_salary and max_salary < min_salary:
            raise ValueError("salary_max must be greater than or equal to salary_min")
        return values


class JobMatchRequest(BaseModel):
    user_id: str
    resume_text: Optional[str] = None
    resume_file_id: Optional[str] = None
    job_descriptions: List[Dict[str, Any]]
    match_threshold: float = Field(default=0.7, ge=0.0, le=1.0)


# ============ RESUME ANALYSIS REQUESTS ============
class ResumeAnalysisRequest(BaseModel):
    user_id: str
    resume_text: Optional[str] = None
    resume_file_id: Optional[str] = None
    job_description: Optional[str] = None
    analysis_type: str = "full"

    @model_validator(mode="after")
    def check_resume_input(cls, values):
        if not values.resume_text and not values.resume_file_id:
            raise ValueError("Either resume_text or resume_file_id must be provided")
        return values


class ResumeOptimizationRequest(BaseModel):
    user_id: str
    resume_text: str = Field(..., min_length=100)
    job_description: str = Field(..., min_length=50)
    optimization_focus: List[str] = ["keywords", "skills", "experience"]
    preserve_format: bool = True


# ============ INTERVIEW REQUESTS ============
class InterviewQuestionsRequest(BaseModel):
    user_id: str
    resume_text: Optional[str] = None
    resume_file_id: Optional[str] = None
    job_description: str = Field(..., min_length=50)
    job_title: str = Field(..., min_length=2)
    company_name: Optional[str] = None
    interview_type: InterviewType = InterviewType.MIXED
    difficulty_level: str = "medium"
    num_questions: int = Field(default=10, ge=1, le=20)

    @model_validator(mode="after")
    def check_resume_input(cls, values):
        if not values.resume_text and not values.resume_file_id:
            raise ValueError("Either resume_text or resume_file_id must be provided")
        return values


class MockInterviewRequest(BaseModel):
    user_id: str
    resume_text: Optional[str] = None
    resume_file_id: Optional[str] = None
    job_description: str
    job_title: str
    company_name: Optional[str] = None
    interview_duration: int = Field(default=30, ge=10, le=60)
    interview_type: InterviewType = InterviewType.MIXED

    @model_validator(mode="after")
    def check_resume_input(cls, values):
        if not values.resume_text and not values.resume_file_id:
            raise ValueError("Either resume_text or resume_file_id must be provided")
        return values


class MockAnswerRequest(BaseModel):
    session_id: str
    question_id: str
    answer: str = Field(..., min_length=1)
    time_taken: Optional[int] = None


# ============ UTILITY REQUESTS ============
class TextEmbeddingRequest(BaseModel):
    texts: List[str] = Field(..., min_items=1, max_items=100)
    model: Optional[str] = None


class FileProcessingRequest(BaseModel):
    file_path: str
    file_type: str
    processing_options: Dict[str, Any] = {}


class BatchJobMatchRequest(BaseModel):
    user_id: str
    resume_file_ids: List[str] = Field(..., min_items=1)
    job_search_criteria: JobSearchRequest
    match_threshold: float = Field(default=0.7, ge=0.0, le=1.0)


# ============ CONFIGURATION REQUESTS ============
class UserPreferencesRequest(BaseModel):
    user_id: str
    preferred_skills: List[str] = []
    preferred_locations: List[str] = []
    salary_expectations: Optional[Dict[str, int]] = None
    work_preferences: Dict[str, Any] = {}
    career_goals: Optional[str] = None


class AIModelConfigRequest(BaseModel):
    use_openai: bool = False
    llm_model: Optional[str] = None
    embedding_model: Optional[str] = None
    temperature: float = Field(default=0.7, ge=0.0, le=1.0)
    max_tokens: int = Field(default=1000, ge=100, le=4000)
