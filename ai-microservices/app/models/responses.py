"""
Response models for the AI microservice.

These Pydantic models define the shape of responses returned from the various
API endpoints. They encapsulate the results of job searches, resume analysis
and optimisation, interview question generation and mock interview sessions.
Each response model closely matches the information consumed by the
corresponding request model in ``requests.py``.
"""

from __future__ import annotations

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

# ============ JOB SEARCH RESPONSES ============

class JobResult(BaseModel):
    """Represents a single job posting returned by the job search endpoints."""

    id: str = Field(..., description="Unique identifier for the job posting")
    title: str = Field(..., description="Job title")
    company: str = Field(..., description="Company name")
    location: Optional[str] = Field(None, description="Job location (city, state)")
    salary_min: Optional[int] = Field(None, description="Minimum salary on offer")
    salary_max: Optional[int] = Field(None, description="Maximum salary on offer")
    description: Optional[str] = Field(None, description="Detailed job description")
    skills: List[str] = Field(default_factory=list, description="List of skills required")
    employment_type: Optional[str] = Field(None, description="Full‑time, Part‑time, Contract, etc.")
    remote: Optional[bool] = Field(None, description="Indicates if the position is remote")
    posted_date: Optional[str] = Field(None, description="Date the job was posted (YYYY‑MM‑DD)")
    url: Optional[str] = Field(None, description="Link to the job posting")
    company_logo_url: Optional[str] = Field(None, description="URL to the company logo")
    similarity_score: Optional[float] = Field(
        None,
        description="Cosine similarity score between the resume and job description (0‑1)",
    )
    '''
    # AI-generated fields
    match_score: Optional[float] = Field(None, ge=0.0, le=1.0, description="Resume match score")
    match_quality: Optional[MatchQuality] = Field(None, description="Match quality rating")
    missing_skills: List[str] = Field(default=[], description="Skills missing from resume")
    matching_keywords: List[str] = Field(default=[], description="Keywords that match resume")
    ai_summary: Optional[str] = Field(None, description="AI-generated job summary")
    '''

class JobSearchResponse(BaseModel):
    """Response returned from the job search endpoint."""

    results: List[JobResult] = Field(..., description="List of job postings matching the query")
    total_results: int = Field(..., description="Total number of jobs returned before filtering")

    '''
    success: bool = Field(default=True, description="Request success status")
    message: Optional[str] = Field(None, description="Response message")
    total_jobs: int = Field(..., ge=0, description="Total jobs found")
    jobs: List[JobResult] = Field(..., description="List of job results")
    search_metadata: Dict[str, Any] = Field(default={}, description="Search metadata")
    ai_insights: Optional[Dict[str, Any]] = Field(None, description="AI-generated insights")
    processing_time: float = Field(..., description="Processing time in seconds")
    '''

class ResumeAnalysisResponse(BaseModel):
    """Response containing the analysis of a resume."""

    ats_score: float = Field(..., ge=0.0, le=100.0, description="ATS compatibility score (0‑100)")
    skills: List[str] = Field(default_factory=list, description="Skills extracted from the resume")
    experience: List[str] = Field(default_factory=list, description="Key experience phrases")
    education: List[str] = Field(default_factory=list, description="Educational qualifications")
    certifications: List[str] = Field(default_factory=list, description="Certifications obtained")
    skill_gap: List[str] = Field(default_factory=list, description="Skills missing relative to the job description")

class ResumeOptimizationResponse(BaseModel):
    """Response containing suggested improvements for a resume."""

    summary: str = Field(..., description="Concise summary of the current resume")
    keywords: List[str] = Field(default_factory=list, description="Relevant keywords to include")
    skills_to_add: List[str] = Field(default_factory=list, description="Missing skills to incorporate")
    experience_to_add: List[str] = Field(default_factory=list, description="Experience areas to elaborate")
    education_to_add: List[str] = Field(default_factory=list, description="Educational items to add")
    certifications_to_add: List[str] = Field(default_factory=list, description="Certifications to pursue")
    recommendations: Optional[str] = Field(None, description="Free‑form recommendations for improvement")


class InterviewQuestion(BaseModel):
    question_id: str = Field(..., description="Unique identifier for the question")
    question: str = Field(..., description="The interview question text")
    category: Optional[str] = Field(None, description="Category (general, technical, behavioral, etc.)")
    difficulty: QuestionDifficulty = Field(..., description="Difficulty level of the question")
    expected_answer_points: List[str] = Field(default_factory=list, description="Key points expected in the answer")
    followup_questions: List[str] = Field(default_factory=list, description="Follow-up questions if applicable")
    tips: List[str] = Field(default_factory=list, description="Tips or hints for answering this question")


class InterviewQuestionsResponse(BaseModel):
    """Response containing a list of structured interview questions."""

    success: bool = Field(default=True, description="Whether the request was successful")
    user_id: Optional[str] = Field(None, description="User requesting the questions")
    job_title: Optional[str] = Field(None, description="Target job title")
    company_name: Optional[str] = Field(None, description="Target company name")
    questions: List[InterviewQuestion] = Field(..., description="List of generated interview questions")
    questions_by_category: Dict[str, List[InterviewQuestion]] = Field(default_factory=dict, description="Questions grouped by category")
    key_topics: List[str] = Field(default_factory=list, description="Key topics to focus on")
    skill_focus_areas: List[str] = Field(default_factory=list, description="Key skills to emphasize")
    company_research_points: List[str] = Field(default_factory=list, description="Research points about the company")
    preparation_tips: List[str] = Field(default_factory=list, description="General preparation tips")
    processing_time: Optional[float] = Field(None, description="Time taken to generate questions")




class MockInterviewQuestion(BaseModel):
    """Represents a single question and optionally an answer in a mock interview session."""

    question_id: str = Field(..., description="Unique identifier for the question")
    question: str = Field(..., description="The interview question")
    answer: Optional[str] = Field(None, description="Candidate's answer to the question, if provided")
    evaluation: Optional[str] = Field(None, description="AI evaluation of the answer, if available")


class MockInterviewSessionResponse(BaseModel):
    """Response returned when initiating or progressing a mock interview session."""

    session_id: str = Field(..., description="Unique ID for the interview session")
    questions: List[MockInterviewQuestion] = Field(
        default_factory=list, description="Ordered list of questions in the session"
    )
    current_question_index: int = Field(
        ..., ge=0, description="Index of the question the candidate should answer next"
    )
    completed: bool = Field(
        False, description="Indicates whether the mock interview has finished"
    )


class QuestionDifficulty(str, Enum):
    EASY = "easy"
    MEDIUM = "medium"
    HARD = "hard"


class EmbeddingResponse(BaseModel):
    """Response containing one or more sentence embeddings."""

    embeddings: List[List[float]] = Field(..., description="List of embeddings for the input texts")


class FileProcessingResponse(BaseModel):
    """Response containing processed file text or metadata."""

    file_text: str = Field(..., description="Extracted plain text from the file")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Additional metadata about the file")


class HealthCheckResponse(BaseModel):
    """Simple response for health‑check endpoints."""

    status: str = Field(..., description="Health status of the service (e.g. 'ok')")
