"""
FastAPI application entrypoint for the AI microservice.

This module sets up the FastAPI app, configures middleware (e.g. CORS)
and mounts the routers that implement job search, resume analysis and
interview assistance functionality.  The application is designed to
integrate with a Spring Boot backend and an Angular frontend.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.services.interview_service import InterviewService
from app.models.requests import InterviewQuestionsRequest
from app.config.settings import settings
from app.models.responses import InterviewQuestionsResponse
from app.routers import (
    health_router,
    job_search_router,
    resume_router,
    interview_router,
)

from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained(settings.LLM_MODEL, token=settings.HF_TOKEN)
tokenizer = AutoTokenizer.from_pretrained(settings.LLM_MODEL, token=settings.HF_TOKEN)

app = FastAPI(
    title="AI Assistant Microservice",
    description="Provides AI features such as job search, resume analysis and interview coaching.",
    version="0.1.0",
)

# Configure CORS to allow requests from the Angular frontend and Spring Boot backend
origins = [
    "http://localhost:4200",  # Angular dev server
    "http://localhost:8080",  # Spring Boot backend
    "*",  # In production you should restrict this to specific domains
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount routers under /api/v1 namespace
app.include_router(health_router, prefix="/api/v1")
app.include_router(job_search_router, prefix="/api/v1")
app.include_router(resume_router, prefix="/api/v1")
app.include_router(interview_router, prefix="/api/v1")

interview_service = InterviewService()

print(">>> HF_TOKEN loaded:", settings.HF_TOKEN)

@app.post("/api/v1/interview/questions", response_model=InterviewQuestionsResponse)
async def generate_interview_questions(request: InterviewQuestionsRequest):
    result = await interview_service.generate_questions(request)
    return result

@app.get("/", tags=["root"])
async def read_root() -> dict:
    """Root endpoint returns a simple greeting."""
    return {"message": "AI Assistant Microservice is up and running"}
