"""
API router for job search and matching.

This router defines endpoints for searching for job postings based on a query
and optionally ranking them using the candidate's resume.  It delegates
business logic to the ``JobSearchService`` and uses ``ResumeService``
internally when resume matching is enabled.
"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, HTTPException
import httpx

from app.config.settings import settings
from app.models.requests import JobSearchRequest
from app.models.responses import JobSearchResponse
from app.services.job_search_service import JobSearchService
from app.services.resume_service import ResumeService
from app.services.vector_service import VectorService
from app.services.llm_service import LLMService


# Instantiate shared services once per process.  They are shared across
# requests and routers.  If you need per‑request state, use FastAPI Depends.
vector_service = VectorService(settings.EMBEDDING_MODEL)
llm_service = LLMService()
resume_service = ResumeService(vector_service, llm_service)
job_search_service = JobSearchService(vector_service)

router = APIRouter()


@router.post("/jobs/search", response_model=JobSearchResponse)
async def job_search(request: JobSearchRequest) -> JobSearchResponse:
    """
    Search for jobs matching the provided criteria.

    If ``use_resume_matching`` is true and a resume is provided via
    ``resume_text`` or ``resume_file_id``, the results will be ranked by
    cosine similarity between the job descriptions and the candidate's resume.

    :param request: ``JobSearchRequest`` containing search parameters.
    :return: ``JobSearchResponse`` with ranked job postings.
    """
    resume_text: Optional[str] = None
    if request.use_resume_matching:
        # Determine the resume text for similarity ranking
        if request.resume_file_id:
            # Fetch the presigned URL from the Spring Boot backend
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
            # Download and parse the resume
            resume_text = await resume_service.fetch_resume_text(None, resume_url)
        elif request.resume_text:
            resume_text = request.resume_text
        # If resume_text is still None, we will proceed without ranking
    response = await job_search_service.search_jobs(
        request.query,
        location=request.location,
        remote_only=request.remote_only,
        use_resume_matching=request.use_resume_matching,
        resume_text=resume_text,
        limit=request.limit,
    )
    return response
