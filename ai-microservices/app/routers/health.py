"""
Health check router.

Provides a simple endpoint that returns a status indicating the service is
healthy.  This can be used by load balancers, Kubernetes readiness probes
or external monitoring services.
"""

from fastapi import APIRouter
from app.models.responses import HealthCheckResponse

router = APIRouter()


@router.get("/health", response_model=HealthCheckResponse)
async def health_check() -> HealthCheckResponse:
    """Return a simple status string indicating the service is alive."""
    return HealthCheckResponse(status="ok")
