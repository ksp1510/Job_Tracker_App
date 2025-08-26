from pydantic import BaseModel, Field, validator
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
    location: Optional[str] = Field(None, description="Preferred job location")
    skills: Optional[List[str]] = Field(default=[], description="Required skills")
    experience_level: Optional[ExperienceLevel] = Field(None, description="Experience level")
    salary_min: Optional[int] = Field(None, ge=0, description="Minimum salary")
    salary_max: Optional[int] = Field(None, ge=0, description="Maximum salary")
    remote_only: bool = Field(default=False, description="Remote jobs only")
    employment_type: Optional[str] = Field(None, description="Full-time, Part-time, Contract")
    company_size: Optional[str] = Field(None, description="Startup, Medium, Large")
    use_resume_matching: bool = Field(default=True, description="Use AI resume matching")
    resume_file_id: Optional[str] = Field(None, description="Resume file ID from MongoDB")
    limit: int = Field(default=20, ge=1, le=100, description="Number of jobs to return")

    @validator('salary_max')
    def salary_max_greater_than_min(cls, v, values):
        if v is not None and 'salary_min' in values and values['salary_min'] is not None:
            if v < values['salary_min']:
                raise ValueError('salary_max must be greater than salary_min')
        return v

class JobMatchRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_text: Optional[str] = Field(None, description="Resume text content")
    resume_file_id: Optional[str] = Field(None, description="Resume file ID from MongoDB")
    job_descriptions: List[Dict[str, Any]] = Field(..., description="List of job descriptions to match")
    match_threshold: float = Field(default=0.7, ge=0.0, le=1.0, description="Similarity threshold")

# ============ RESUME ANALYSIS REQUESTS ============

class ResumeAnalysisRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_text: Optional[str] = Field(None, description="Resume text content")
    resume_file_id: Optional[str] = Field(None, description="Resume file ID from MongoDB")
    job_description: Optional[str] = Field(None, description="Job description for optimization")
    analysis_type: str = Field(default="full", description="full, keywords, skills, experience")
    
    @validator('resume_text', 'resume_file_id', pre=True, always=True)
    def validate_resume_input(cls, v, values):
        resume_text = values.get('resume_text')
        resume_file_id = values.get('resume_file_id')
        if not resume_text and not resume_file_id:
            raise ValueError('Either resume_text or resume_file_id must be provided')
        return v

class ResumeOptimizationRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_text: str = Field(..., min_length=100, description="Current resume text")
    job_description: str = Field(..., min_length=50, description="Target job description")
    optimization_focus: List[str] = Field(default=["keywords", "skills", "experience"], 
                                        description="Areas to focus optimization on")
    preserve_format: bool = Field(default=True, description="Maintain original formatting")

# ============ INTERVIEW REQUESTS ============

class InterviewQuestionsRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_text: Optional[str] = Field(None, description="Resume text content")
    resume_file_id: Optional[str] = Field(None, description="Resume file ID")
    job_description: str = Field(..., min_length=50, description="Job description")
    job_title: str = Field(..., min_length=2, description="Job title")
    company_name: Optional[str] = Field(None, description="Company name")
    interview_type: InterviewType = Field(default=InterviewType.MIXED, description="Type of interview")
    difficulty_level: str = Field(default="medium", description="easy, medium, hard")
    num_questions: int = Field(default=10, ge=1, le=20, description="Number of questions to generate")
    
    @validator('resume_text', 'resume_file_id', pre=True, always=True)
    def validate_resume_input(cls, v, values):
        resume_text = values.get('resume_text')
        resume_file_id = values.get('resume_file_id')
        if not resume_text and not resume_file_id:
            raise ValueError('Either resume_text or resume_file_id must be provided')
        return v

class MockInterviewRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_text: Optional[str] = Field(None, description="Resume text content")
    resume_file_id: Optional[str] = Field(None, description="Resume file ID")
    job_description: str = Field(..., description="Job description")
    job_title: str = Field(..., description="Job title")
    company_name: Optional[str] = Field(None, description="Company name")
    interview_duration: int = Field(default=30, ge=10, le=60, description="Interview duration in minutes")
    interview_type: InterviewType = Field(default=InterviewType.MIXED, description="Interview type")
    
class MockAnswerRequest(BaseModel):
    session_id: str = Field(..., description="Mock interview session ID")
    question_id: str = Field(..., description="Current question ID")
    answer: str = Field(..., min_length=1, description="User's answer")
    time_taken: Optional[int] = Field(None, description="Time taken to answer in seconds")

# ============ UTILITY REQUESTS ============

class TextEmbeddingRequest(BaseModel):
    texts: List[str] = Field(..., min_items=1, max_items=100, description="Texts to embed")
    model: Optional[str] = Field(None, description="Embedding model to use")

class FileProcessingRequest(BaseModel):
    file_path: str = Field(..., description="S3 file path or local path")
    file_type: str = Field(..., description="File type: pdf, docx, txt")
    processing_options: Dict[str, Any] = Field(default={}, description="Processing options")

class BatchJobMatchRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    resume_file_ids: List[str] = Field(..., min_items=1, description="List of resume file IDs")
    job_search_criteria: JobSearchRequest = Field(..., description="Job search parameters")
    match_threshold: float = Field(default=0.7, ge=0.0, le=1.0, description="Match threshold")

# ============ CONFIGURATION REQUESTS ============

class UserPreferencesRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    preferred_skills: List[str] = Field(default=[], description="Preferred skills")
    preferred_locations: List[str] = Field(default=[], description="Preferred locations")
    salary_expectations: Optional[Dict[str, int]] = Field(None, description="Min/max salary")
    work_preferences: Dict[str, Any] = Field(default={}, description="Remote, hybrid, etc.")
    career_goals: Optional[str] = Field(None, description="Career objectives")

class AIModelConfigRequest(BaseModel):
    use_openai: bool = Field(default=False, description="Use OpenAI or HuggingFace")
    llm_model: Optional[str] = Field(None, description="LLM model to use")
    embedding_model: Optional[str] = Field(None, description="Embedding model to use")
    temperature: float = Field(default=0.7, ge=0.0, le=1.0, description="Model temperature")
    max_tokens: int = Field(default=1000, ge=100, le=4000, description="Max tokens for generation")