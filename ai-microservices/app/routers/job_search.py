from fastapi import APIRouter
import httpx, os

router = APIRouter()
THEIRSTACK_API = os.getenv("THEIRSTACK_API_KEY")
JSEARCH_API = os.getenv("JSEARCH_API_KEY")

@router.get("/")
def search_jobs(query: str, location: str = "remote"):
    url = f"https://api.jsearch.p.rapidapi.com/search?query={query}&location={location}"
    headers = {"X-RapidAPI-Key": JSEARCH_API}
    r = httpx.get(url, headers=headers)
    return r.json()
