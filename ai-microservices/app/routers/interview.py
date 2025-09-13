"""
API router for interview assistance.

Defines endpoints for generating interview questions and conducting mock
interview sessions.  Supports both simple question generation and
interactive sessions with answer evaluation.  Uses ``InterviewService``
for business logic and ``ResumeService`` to fetch resume text when
necessary.
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException
import httpx

from app.config.settings import settings
from app.models.requests import (
    InterviewQuestionsRequest,
    MockInterviewRequest,
    MockAnswerRequest,
)
from app.models.responses import (
    InterviewQuestionsResponse,
    MockInterviewSessionResponse,
)
from app.services.vector_service import VectorService
from app.services.llm_service import LLMService
from app.services.resume_service import ResumeService
from app.services.interview_service import InterviewService


# Shared services
vector_service = VectorService(settings.EMBEDDING_MODEL)
llm_service = LLMService()
resume_service = ResumeService(vector_service, llm_service)
interview_service = InterviewService()

router = APIRouter()


async def _get_resume_text_from_request(
    resume_text: str | None,
    resume_file_id: str | None,
) -> str:
    """Helper to retrieve resume text from either direct text or file ID."""
    if resume_text:
        return resume_text
    if resume_file_id:
        presigned_endpoint = f"{settings.SPRING_BOOT_URL}/files/presigned/{resume_file_id}"
        try:
            async with httpx.AsyncClient() as client:
                presigned_resp = await client.get(presigned_endpoint, timeout=settings.REQUEST_TIMEOUT)
                presigned_resp.raise_for_status()
                resume_url = presigned_resp.text.strip().strip('"')
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"Failed to retrieve resume URL from backend: {exc}",
            ) from exc
        return await resume_service.fetch_resume_text(None, resume_url)
    return ""


@router.post("/interview/questions", response_model=InterviewQuestionsResponse)
async def generate_interview_questions(
    request: InterviewQuestionsRequest,
) -> InterviewQuestionsResponse:
    """
    Generate a list of interview questions tailored to the candidate and job.

    The resume may be provided directly as ``resume_text`` or indirectly via
    ``resume_file_id``.  A job description is required.
    """
    resume_text = await _get_resume_text_from_request(request.resume_text, request.resume_file_id)
    response = await interview_service.generate_questions(
        resume_text,
        request.job_description,
        job_title=request.job_title,
        company_name=request.company_name,
        interview_type=request.interview_type.value if hasattr(request.interview_type, "value") else str(request.interview_type),
        num_questions=request.num_questions,
    )
    return response


@router.post("/interview/mock/start", response_model=MockInterviewSessionResponse)
async def start_mock_interview(
    request: MockInterviewRequest,
) -> MockInterviewSessionResponse:
    """
    Start a mock interview session and return the first question along with a session ID.

    The resume may be provided directly or via a file ID.  The job description
    and title are used to generate questions.
    """
    resume_text = await _get_resume_text_from_request(request.resume_text, request.resume_file_id)
    response = await interview_service.start_mock_interview(
        resume_text,
        request.job_description,
        job_title=request.job_title,
        company_name=request.company_name,
        interview_type=request.interview_type.value if hasattr(request.interview_type, "value") else str(request.interview_type),
        num_questions=10,
    )
    return response


@router.post("/interview/mock/answer", response_model=MockInterviewSessionResponse)
async def answer_mock_interview_question(
    request: MockAnswerRequest,
) -> MockInterviewSessionResponse:
    """
    Submit an answer for the current question in a mock interview session.

    The session ID must reference a valid session created via ``/interview/mock/start``.
    """
    response = await interview_service.answer_question(
        request.session_id,
        request.question_id,
        request.answer,
    )
    return response
