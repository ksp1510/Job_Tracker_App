"""
Job search and matching service.

This module integrates with external job search APIs (TheirStack and JSearch)
to fetch job postings and ranks them using semantic similarity against a
candidate's resume.  During development or when API keys are unavailable,
sample data from ``MockDataConfig`` is used instead.  The service also
supports basic filtering on location and remote status.
"""

from __future__ import annotations

import logging
from typing import List, Optional, Dict, Any

import httpx

from app.config.settings import settings, APIConfig, MockDataConfig
from app.models.responses import JobResult, JobSearchResponse
from .vector_service import VectorService

logger = logging.getLogger(__name__)


class JobSearchService:
    """Service responsible for fetching and ranking job postings."""

    def __init__(self, vector_service: VectorService) -> None:
        self.vector_service = vector_service

    async def _fetch_from_jsearch(
        self,
        query: str,
        location: Optional[str] = None,
        remote_only: bool = False,
        limit: int = 20,
    ) -> List[Dict[str, Any]]:
        """
        Retrieve job postings from the JSearch API.

        :param query: Job search query string.
        :param location: Preferred job location (optional).
        :param remote_only: Whether to restrict to remote positions.
        :param limit: Number of jobs to fetch.
        :return: List of raw job posting dictionaries.
        """
        headers = APIConfig.get_job_search_headers("jsearch")
        params = {
            "query": query,
            "num_pages": 1,
        }
        if location:
            params["location"] = location
        if remote_only:
            params["remote_only"] = "true"
        url = f"{APIConfig.JSEARCH_BASE_URL}/search"
        try:
            async with httpx.AsyncClient() as client:
                resp = await client.get(url, params=params, headers=headers, timeout=settings.REQUEST_TIMEOUT)
                resp.raise_for_status()
                data = resp.json()
        except Exception as exc:
            logger.error(f"JSearch API request failed: {exc}")
            return []
        jobs = data.get("data", []) if isinstance(data, dict) else []
        # Limit results
        return jobs[:limit]

    async def _fetch_from_theirstack(
        self,
        query: str,
        location: Optional[str] = None,
        remote_only: bool = False,
        limit: int = 20,
    ) -> List[Dict[str, Any]]:
        """
        Retrieve job postings from the TheirStack API.
        Currently unimplemented; returns an empty list.
        """
        # Future extension: implement call to TheirStack API when API key is configured.
        return []

    async def search_jobs(
        self,
        query: str,
        *,
        location: Optional[str] = None,
        remote_only: bool = False,
        use_resume_matching: bool = True,
        resume_text: Optional[str] = None,
        limit: int = 20,
    ) -> JobSearchResponse:
        """
        Search for jobs and optionally rank them based on resume similarity.

        :param query: Job search query string.
        :param location: Preferred job location (optional).
        :param remote_only: Whether to restrict to remote positions.
        :param use_resume_matching: Whether to rank results based on similarity to the resume.
        :param resume_text: Candidate's resume text for ranking purposes (optional).
        :param limit: Maximum number of jobs to return.
        :return: ``JobSearchResponse`` containing sorted job results.
        """
        # Fetch jobs either from mock data or external APIs
        if settings.USE_MOCK_DATA:
            jobs: List[Dict[str, Any]] = MockDataConfig.SAMPLE_JOBS.copy()
        else:
            jobs = []
            # Try JSearch first
            jobs += await self._fetch_from_jsearch(query, location, remote_only, limit)
            # Fallback to TheirStack if JSearch returned none
            if not jobs:
                jobs += await self._fetch_from_theirstack(query, location, remote_only, limit)
        # Convert raw jobs into a common structure
        job_results: List[Dict[str, Any]] = []
        for job in jobs:
            job_results.append(
                {
                    "id": job.get("id") or job.get("job_id") or job.get("_id") or "",
                    "title": job.get("title") or job.get("job_title") or "",
                    "company": job.get("company") or job.get("employer_name") or "",
                    "location": job.get("location") or job.get("job_location") or "",
                    "salary_min": job.get("salary_min") or job.get("min_salary") or None,
                    "salary_max": job.get("salary_max") or job.get("max_salary") or None,
                    "description": job.get("description") or job.get("job_description") or job.get("snippet") or "",
                    "skills": job.get("skills") or [],
                    "employment_type": job.get("employment_type") or job.get("job_employment_type"),
                    "remote": job.get("remote"),
                    "posted_date": job.get("posted_date") or job.get("job_posted_at_datetime_utc") or job.get("posted"),
                    "url": job.get("url") or job.get("job_url") or None,
                    "company_logo_url": job.get("company_logo_url") or job.get("employer_logo") or None,
                }
            )
        # If resume matching is requested and resume text provided, rank by similarity
        if use_resume_matching and resume_text:
            ranked = self.vector_service.rank_items(resume_text, job_results, text_key="description")
            # Build JobResult objects with similarity_score
            results = []
            for item, score in ranked[:limit]:
                item["similarity_score"] = round(score, 3)
                results.append(JobResult(**item))
        else:
            # No ranking; convert directly
            results = [JobResult(**job) for job in job_results[:limit]]
        return JobSearchResponse(results=results, total_results=len(results))
