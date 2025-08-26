from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

class MatchQuality(str, Enum):
    EXCELLENT = "excellent"  # 90-100%
    GOOD = "good"           # 75-89%
    FAIR = "fair"           # 60-74%
    POOR = "poor"           # <60%

class QuestionDifficulty(str, Enum):
    EASY = "easy"
    MEDIUM = "medium"
    HARD = "hard"

# ============ JOB SEARCH RESPONSES ============

class JobResult(BaseModel):
    job_id: str = Field(..., description="Unique job identifier")
    title: str = Field(..., description="Job title")
    company: str = Field(..., description="Company name")
    location: str = Field(..., description="Job location")
    salary_min: Optional[int] = Field(None, description="Minimum salary")
    salary_max: Optional[int] = Field(None, description="Maximum salary")
    description: str = Field(..., description="Job description")
    requirements: List[str] = Field(default=[], description="Job requirements")
    skills: List[str] = Field(default=[], description="Required skills")
    employment_type: Optional[str] = Field(None, description="Employment type")
    remote: bool = Field(default=False, description="Remote work option")
    posted_date: Optional[str] = Field(None, description="Job posting date")
    job_url: Optional[str] = Field(None, description="Original job URL")
    company_logo: Optional[str] = Field(None, description="Company logo URL")
    
    # AI-generated fields
    match_score: Optional[float] = Field(None, ge=0.0, le=1.0, description="Resume match score")
    match_quality: Optional[MatchQuality] = Field(None, description="Match quality rating")
    missing_skills: List[str] = Field(default=[], description="Skills missing from resume")
    matching_keywords: List[str] = Field(default=[], description="Keywords that match resume")
    ai_summary: Optional[str] = Field(None, description="AI-generated job summary")

class JobSearchResponse(BaseModel):
    success: bool = Field(default=True, description="Request success status")
    message: Optional[str] = Field(None, description="Response message")
    total_jobs: int = Field(..., ge=0, description="Total jobs found")
    jobs: List[JobResult] = Field(..., description="List of job results")
    search_metadata: Dict[str, Any] = Field(default={}, description="Search metadata")
    ai_insights: Optional[Dict[str, Any]] = Field(None, description="AI-generated insights")
    processing_time: float = Field(..., description="Processing time in seconds")

class JobMatchResponse(BaseModel):
    success: bool = Field(default=True, description="Request success status")
    user_id: str = Field(..., description="User ID")
    total_jobs: int = Field(..., description="Total jobs analyzed")
    matched_jobs: List[JobResult] = Field(..., description="Jobs that meet match threshold")
    match_summary: Dict[str, Any] = Field(default={}, description="Overall match summary")
    recommendations: List[str] = Field(default=[], description="AI recommendations")
    processing_time: float = Field(..., description="Processing time in seconds")

# ============ RESUME ANALYSIS RESPONSES ============

class SkillAnalysis(BaseModel):
    skill: str = Field(..., description="Skill name")
    proficiency_level: Optional[str] = Field(None, description="Proficiency level")
    years_experience: Optional[int] = Field(None, description="Years of experience")
    last_used: Optional[str] = Field(None, description="When skill was last used")
    skill_category: Optional[str] = Field(None, description="Technical, soft, domain, etc.")

class ExperienceAnalysis(BaseModel):
    job_title: str = Field(..., description="Job title")
    company: str = Field(..., description="Company name")
    duration: Optional[str] = Field(None, description="Employment duration")
    key_achievements: List[str] = Field(default=[], description="Key achievements")
    relevant_skills: List[str] = Field(default=[], description="Skills from this role")

class ResumeAnalysisResponse(BaseModel):
    success: bool = Field(default=True, description="Analysis success status")
    user_id: str = Field(..., description="User ID")
    
    # Core analysis
    extracted_skills: List[SkillAnalysis] = Field(..., description="Extracted skills analysis")
    experience_summary: List[ExperienceAnalysis] = Field(..., description="Experience analysis")
    education: List[Dict[str, str]] = Field(default=[], description="Education details")
    certifications: List[str] = Field(default=[], description="Certifications")
    
    # Analytics
    total_experience_years: Optional[int] = Field(None, description="Total years of experience")
    career_level: Optional[str] = Field(None, description="Career level assessment")
    skill_gaps: List[str] = Field(default=[], description="Potential skill gaps")
    strengths: List[str] = Field(default=[], description="Resume strengths")
    improvement_suggestions: List[str] = Field(default=[], description="Improvement suggestions")
    
    # Keywords and matching
    keywords: List[str] = Field(default=[], description="Important keywords found")
    ats_score: Optional[float] = Field(None, ge=0.0, le=100.0, description="ATS compatibility score")
    
    # Processing info
    processing_time: float = Field(..., description="Processing time in seconds")
    analysis_timestamp: datetime = Field(default_factory=datetime.now)

class ResumeOptimizationResponse(BaseModel):
    success: bool = Field(default=True, description="Optimization success status")
    user_id: str = Field(..., description="User ID")
    
    # Optimization results
    optimized_sections: Dict[str, str] = Field(default={}, description="Optimized resume sections")
    added_keywords: List[str] = Field(default=[], description="Keywords that should be added")
    improved_phrases: List[Dict[str, str]] = Field(default=[], description="Original -> improved phrases")
    formatting_suggestions: List[str] = Field(default=[], description="Formatting improvements")
    
    # Scoring
    original_score: float = Field(..., ge=0.0, le=100.0, description="Original resume score")
    optimized_score: float = Field(..., ge=0.0, le=100.0, description="Optimized resume score")
    improvement_percentage: float = Field(..., description="Percentage improvement")
    
    # Detailed feedback
    missing_skills: List[str] = Field(default=[], description="Skills missing from resume")
    skill_match_improvement: Dict[str, float] = Field(default={}, description="Skill match improvements")
    recommendations: List[str] = Field(default=[], description="Specific recommendations")
    
    processing_time: float = Field(..., description="Processing time in seconds")

# ============ INTERVIEW RESPONSES ============

class InterviewQuestion(BaseModel):
    question_id: str = Field(..., description="Unique question identifier")
    question: str = Field(..., description="Interview question")
    category: str = Field(..., description="Question category")
    difficulty: QuestionDifficulty = Field(..., description="Question difficulty")
    expected_answer_points: List[str] = Field(default=[], description="Key points for good answer")
    followup_questions: List[str] = Field(default=[], description="Potential follow-up questions")
    tips: List[str] = Field(default=[], description="Tips for answering")

class InterviewQuestionsResponse(BaseModel):
    success: bool = Field(default=True, description="Generation success status")
    user_id: str = Field(..., description="User ID")
    job_title: str = Field(..., description="Job title")
    company_name: Optional[str] = Field(None, description="Company name")
    
    # Questions by category
    questions: List[InterviewQuestion] = Field(..., description="Generated interview questions")
    questions_by_category: Dict[str, List[InterviewQuestion]] = Field(default={}, description="Questions grouped by category")
    
    # Preparation insights
    key_topics: List[str] = Field(default=[], description="Key topics to prepare")
    skill_focus_areas: List[str] = Field(default=[], description="Skills to highlight")
    company_research_points: List[str] = Field(default=[], description="Company research suggestions")
    preparation_tips: List[str] = Field(default=[], description="General preparation tips")
    
    processing_time: float = Field(..., description="Processing time in seconds")
    generated_at: datetime = Field(default_factory=datetime.now)

class MockInterviewSession(BaseModel):
    session_id: str = Field(..., description="Session identifier")
    user_id: str = Field(..., description="User ID")
    job_title: str = Field(..., description="Job title")
    status: str = Field(..., description="Session status: active, paused, completed")
    current_question_index: int = Field(default=0, description="Current question index")
    total_questions: int = Field(..., description="Total questions in session")
    
    # Timing
    start_time: datetime = Field(..., description="Session start time")
    estimated_duration: int = Field(..., description="Estimated duration in minutes")
    time_elapsed: Optional[int] = Field(None, description="Time elapsed in seconds")
    
    # Progress
    questions_answered: int = Field(default=0, description="Questions answered")
    current_question: Optional[InterviewQuestion] = Field(None, description="Current question")

class AnswerEvaluation(BaseModel):
    question_id: str = Field(..., description="Question ID")
    question: str = Field(..., description="Original question")
    user_answer: str = Field(..., description="User's answer")
    
    # Scoring
    overall_score: float = Field(..., ge=0.0, le=10.0, description="Overall answer score")
    content_score: float = Field(..., ge=0.0, le=10.0, description="Content quality score")
    structure_score: float = Field(..., ge=0.0, le=10.0, description="Answer structure score")
    relevance_score: float = Field(..., ge=0.0, le=10.0, description="Relevance to question score")
    
    # Detailed feedback
    strengths: List[str] = Field(default=[], description="Answer strengths")
    improvements: List[str] = Field(default=[], description="Areas for improvement")
    missing_points: List[str] = Field(default=[], description="Key points missed")
    suggested_improvement: Optional[str] = Field(None, description="Improved version of answer")
    
    # Timing
    time_taken: Optional[int] = Field(None, description="Time taken to answer in seconds")
    recommended_time: Optional[int] = Field(None, description="Recommended answer time")

class MockInterviewResponse(BaseModel):
    success: bool = Field(default=True, description="Response success status")
    session: MockInterviewSession = Field(..., description="Session details")
    current_evaluation: Optional[AnswerEvaluation] = Field(None, description="Current answer evaluation")
    
    # Session progress
    progress_percentage: float = Field(..., ge=0.0, le=100.0, description="Session progress")
    next_question: Optional[InterviewQuestion] = Field(None, description="Next question")
    
    # Real-time feedback
    session_insights: List[str] = Field(default=[], description="Real-time session insights")
    performance_trends: Dict[str, float] = Field(default={}, description="Performance trends")

class InterviewSessionSummary(BaseModel):
    session_id: str = Field(..., description="Session ID")
    user_id: str = Field(..., description="User ID")
    completed_at: datetime = Field(..., description="Completion timestamp")
    
    # Overall performance
    overall_score: float = Field(..., ge=0.0, le=10.0, description="Overall interview score")
    total_questions: int = Field(..., description="Total questions")
    questions_answered: int = Field(..., description="Questions answered")
    total_duration: int = Field(..., description="Total duration in minutes")
    
    # Category performance
    category_scores: Dict[str, float] = Field(default={}, description="Scores by question category")
    skill_assessment: Dict[str, float] = Field(default={}, description="Skill-based assessment")
    
    # Detailed evaluations
    question_evaluations: List[AnswerEvaluation] = Field(default=[], description="All answer evaluations")
    
    # Comprehensive feedback
    overall_strengths: List[str] = Field(default=[], description="Overall strengths")
    areas_for_improvement: List[str] = Field(default=[], description="Areas for improvement")
    interview_tips: List[str] = Field(default=[], description="Interview tips")
    next_steps: List[str] = Field(default=[], description="Recommended next steps")
    
    # Performance metrics
    average_response_time: float = Field(..., description="Average response time")
    confidence_score: Optional[float] = Field(None, description="Confidence assessment")
    communication_score: Optional[float] = Field(None, description="Communication effectiveness")

# ============ UTILITY RESPONSES ============

class EmbeddingResponse(BaseModel):
    success: bool = Field(default=True, description="Embedding success status")
    embeddings: List[List[float]] = Field(..., description="Generated embeddings")
    model_used: str = Field(..., description="Embedding model used")
    processing_time: float = Field(..., description="Processing time in seconds")

class FileProcessingResponse(BaseModel):
    success: bool = Field(default=True, description="Processing success status")
    file_path: str = Field(..., description="Processed file path")
    extracted_text: str = Field(..., description="Extracted text content")
    metadata: Dict[str, Any] = Field(default={}, description="File metadata")
    processing_time: float = Field(..., description="Processing time in seconds")

class HealthCheckResponse(BaseModel):
    status: str = Field(..., description="Service status")
    service: str = Field(..., description="Service name")
    timestamp: datetime = Field(default_factory=datetime.now)
    version: str = Field(default="1.0.0", description="Service version")
    dependencies: Dict[str, str] = Field(default={}, description="Dependency status")

# ============ ERROR RESPONSES ============

class ErrorResponse(BaseModel):
    success: bool = Field(default=False, description="Request success status")
    error_code: str = Field(..., description="Error code")
    message: str = Field(..., description="Error message")
    details: Optional[Dict[str, Any]] = Field(None, description="Error details")
    timestamp: datetime = Field(default_factory=datetime.now)
    request_id: Optional[str] = Field(None, description="Request identifier")