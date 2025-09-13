# AI Microservices

This directory contains a FastAPI‑based microservice that powers the AI assistant layer for the Job Tracker application.

The service provides endpoints for job search and ranking, resume analysis and optimization, and interview assistance (question generation and mock interview sessions).

## Running locally

Install the dependencies and start the server:

```bash
cd ai-microservices
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

The service reads configuration from environment variables or a `.env` file.  See `app/config/settings.py` for supported options.  At a minimum, configure your API keys for job search providers (TheirStack and JSearch) when `USE_MOCK_DATA` is set to `False`.  The language model is self‑hosted, so no external LLM API keys are required.

## Docker

To build and run using Docker:

```bash
cd ai-microservices
docker build -t ai-microservices .
docker run -p 8001:8001 ai-microservices
```

## Endpoints

The service exposes the following API endpoints (all under `/api/v1`):

* `/jobs/search` – search for jobs using TheirStack and JSearch or mock data, optionally ranking by resume similarity.
* `/resume/analyze` – extract skills, experiences and other information from a resume.
* `/resume/optimize` – suggest improvements to a resume based on a target job description.
* `/interview/questions` – generate a set of interview questions for a given resume and job.
* `/interview/mock/start` – start a mock interview session and obtain a session ID and first question.
* `/interview/mock/answer` – submit an answer during a mock interview session and receive feedback along with the next question.
* `/health` – basic health check endpoint that returns `{"status": "ok"}`.

See `app/main.py` for router registration and `app/models/requests.py` and `app/models/responses.py` for full request and response models.