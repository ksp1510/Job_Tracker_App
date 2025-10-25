from fastapi import FastAPI, HTTPException, UploadFile, File, Depends, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import uvicorn
import logging
import os
from contextlib import asynccontextmanager

# Import our modules
from services.job_search_service import JobSearchService
from services.resume_service import ResumeService
from services.interview_service import InterviewService
from services.vector_service import VectorService
from config.settings import settings
from models.requests import (
    JobSearchRequest, 
    ResumeAnalysisRequest, 
    InterviewQuestionsRequest,
    MockInterviewRequest,
    JobMatchRequest
)
from models.responses import (
    JobSearchResponse,
    ResumeAnalysisResponse,
    InterviewQuestionsResponse,
    MockInterviewResponse,
    JobMatchResponse
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global services
job_search_service = None
resume_service = None
interview_service = None
vector_service = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    global job_search_service, resume_service, interview_service, vector_service
    
    logger.info("Starting AI Microservice...")
    
    # Initialize services
    vector_service = VectorService()
    await vector_service.initialize()
    
    job_search_service = JobSearchService(vector_service)
    resume_service = ResumeService(vector_service)
    interview_service = InterviewService()
    
    logger.info("AI Microservice started successfully")
    
    yield
    
    # Shutdown
    logger.info("Shutting down AI Microservice...")
    if vector_service:
        await vector_service.cleanup()

app = FastAPI(
    title="Job Tracker AI Assistant",
    description="AI-powered job search, resume optimization, and interview assistance",
    version="1.0.0",
    lifespan=lifespan
)

# SECURITY FIX: Configure CORS from environment variable
allowed_origins = os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080").split(",")

app.add_middleware(
    CORSMiddleware,  # Fixed typo: was CORsMiddleware
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Health check endpoint
@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "ai-microservice"}

# ============ JOB SEARCH ENDPOINTS ============

@app.post("/api/v1/jobs/search", response_model=JobSearchResponse)
async def search_jobs(request: JobSearchRequest):
    """Search jobs using TheirStack and JSearch APIs with AI ranking"""
    try:
        result = await job_search_service.search_jobs(request)
        return result
    except Exception as e:
        logger.error(f"Job search failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Job search failed: {str(e)}")

@app.post("/api/v1/jobs/match", response_model=JobMatchResponse)
async def match_jobs_to_resume(request: JobMatchRequest):
    """Match jobs to user's resume using semantic similarity"""
    try:
        result = await job_search_service.match_jobs_to_resume(request)
        return result
    except Exception as e:
        logger.error(f"Job matching failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Job matching failed: {str(e)}")

# ============ RESUME ANALYSIS ENDPOINTS ============

@app.post("/api/v1/resume/analyze", response_model=ResumeAnalysisResponse)
async def analyze_resume(request: ResumeAnalysisRequest):
    """Analyze resume and extract keywords, skills, experience"""
    try:
        result = await resume_service.analyze_resume(request)
        return result
    except Exception as e:
        logger.error(f"Resume analysis failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Resume analysis failed: {str(e)}")

@app.post("/api/v1/resume/optimize")
async def optimize_resume_for_job(request: ResumeAnalysisRequest):
    """Optimize resume based on job description"""
    try:
        result = await resume_service.optimize_resume_for_job(request)
        return result
    except Exception as e:
        logger.error(f"Resume optimization failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Resume optimization failed: {str(e)}")

@app.post("/api/v1/resume/upload")
async def upload_resume_for_analysis(
    file: UploadFile = File(...),
    user_id: str = None
):
    """Upload and analyze resume file"""
    # SECURITY: File size limit - 10MB max
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB in bytes

    try:
        # Read file content to check size
        contents = await file.read()
        if len(contents) > MAX_FILE_SIZE:
            raise HTTPException(
                status_code=413,
                detail=f"File too large. Maximum size is 10MB"
            )

        # Reset file pointer for resume service
        await file.seek(0)

        result = await resume_service.process_uploaded_resume(file, user_id)
        return result
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Resume upload failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Resume upload failed: {str(e)}")

# ============ INTERVIEW ASSISTANCE ENDPOINTS ============

@app.post("/api/v1/interview/questions", response_model=InterviewQuestionsResponse)
async def generate_interview_questions(request: InterviewQuestionsRequest):
    """Generate interview questions based on resume and job description"""
    try:
        result = await interview_service.generate_questions(request)
        return result
    except Exception as e:
        logger.error(f"Interview question generation failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Question generation failed: {str(e)}")

@app.post("/api/v1/interview/mock-start")
async def start_mock_interview(request: MockInterviewRequest):
    """Start a mock interview session"""
    try:
        result = await interview_service.start_mock_interview(request)
        return result
    except Exception as e:
        logger.error(f"Mock interview start failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Mock interview failed: {str(e)}")

@app.post("/api/v1/interview/mock-answer")
async def process_mock_answer(
    session_id: str,
    question_id: str,
    answer: str
):
    """Process user's answer in mock interview"""
    try:
        result = await interview_service.process_answer(session_id, question_id, answer)
        return result
    except Exception as e:
        logger.error(f"Mock answer processing failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Answer processing failed: {str(e)}")

@app.get("/api/v1/interview/mock-session/{session_id}")
async def get_mock_interview_session(session_id: str):
    """Get mock interview session details"""
    try:
        result = await interview_service.get_session(session_id)
        return result
    except Exception as e:
        logger.error(f"Session retrieval failed: {str(e)}")
        raise HTTPException(status_code=404, detail="Session not found")

# ============ UTILITY ENDPOINTS ============

@app.get("/api/v1/models/status")
async def get_model_status():
    """Get status of all AI models"""
    return {
        "embedding_model": "sentence-transformers/all-MiniLM-L6-v2",
        "llm_model": settings.LLM_MODEL,
        "vector_db": "ChromaDB",
        "job_apis": ["TheirStack", "JSearch"],
        "status": "operational"
    }

@app.post("/api/v1/embeddings/generate")
async def generate_embeddings(texts: List[str]):
    """Generate embeddings for given texts"""
    try:
        embeddings = await vector_service.get_embeddings(texts)
        return {"embeddings": embeddings}
    except Exception as e:
        logger.error(f"Embedding generation failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Embedding generation failed: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
        log_level="info"
    )