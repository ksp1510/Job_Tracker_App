"""
Router package for the AI microservice.

This package exposes individual FastAPI routers that provide job search,
resume analysis/optimisation and interview assistance functionality.  The
routers are mounted in ``app/main.py`` under the `/api/v1` prefix.
"""

from .job_search import router as job_search_router  # noqa: F401
from .resume import router as resume_router  # noqa: F401
from .interview import router as interview_router  # noqa: F401
from .health import router as health_router  # noqa: F401

__all__ = [
    "job_search_router",
    "resume_router",
    "interview_router",
    "health_router",
]
