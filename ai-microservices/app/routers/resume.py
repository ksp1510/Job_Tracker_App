"""
API router for resume analysis and optimisation.

Exposes endpoints for analysing a resume (extracting skills, experience,
education, certifications and computing an ATS score) and for generating
optimisation recommendations relative to a job description.  Supports
accepting resume text directly or referencing a file stored in the
Spring Boot backend via its MongoDB ID.
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException
import httpx

from app.config.settings import settings
from app.models.requests import ResumeAnalysisRequest, ResumeOptimizationRequest
from app.models.responses import ResumeAnalysisResponse, ResumeOptimizationResponse
from app.services.vector_service import VectorService
from app.services.llm_service import LLMService
from app.services.resume_service import ResumeService


# Instantiate shared services
vector_service = VectorService(settings.EMBEDDING_MODEL)
llm_service = LLMService()
resume_service = ResumeService(vector_service, llm_service)

router = APIRouter()


@router.post("/resume/analyze", response_model=ResumeAnalysisResponse)
async def analyse_resume(request: ResumeAnalysisRequest) -> ResumeAnalysisResponse:
    """
    Analyse a resume and optionally compare it to a job description.

    The resume can be provided directly via ``resume_text`` or indirectly via
    ``resume_file_id`` which refers to a file stored in the Spring Boot
    backend.  If ``resume_file_id`` is supplied, the microservice will
    retrieve a presigned download URL from the backend and parse the file.

    :param request: ``ResumeAnalysisRequest`` containing the resume and job description.
    :return: ``ResumeAnalysisResponse`` with extracted information.
    """
    # Retrieve resume text
    resume_text: str = request.resume_text or ""
    if request.resume_file_id:
        presigned_endpoint = f"{settings.SPRING_BOOT_URL}/files/presigned/{request.resume_file_id}"
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
        resume_text = await resume_service.fetch_resume_text(None, resume_url)
    response = await resume_service.analyse(
        resume_text=resume_text,
        resume_url=None,
        job_description=request.job_description,
    )
    return response


@router.post("/resume/optimize", response_model=ResumeOptimizationResponse)
async def optimise_resume(request: ResumeOptimizationRequest) -> ResumeOptimizationResponse:
    """
    Generate optimisation suggestions for a resume relative to a job description.

    This endpoint currently requires ``resume_text`` to be supplied in the
    request body.  In future it could be extended to accept a
    ``resume_file_id`` similar to the analysis endpoint.

    :param request: ``ResumeOptimizationRequest`` containing the resume and target job description.
    :return: ``ResumeOptimizationResponse`` with optimisation suggestions.
    """
    # Only resume_text is supported for optimisation.  If resume_file_id is needed, fetch first.
    response = await resume_service.optimise(
        resume_text=request.resume_text,
        resume_url=None,
        job_description=request.job_description,
    )
    return response
