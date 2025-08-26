from fastapi import APIRouter
import openai, os

router = APIRouter()
openai.api_key = os.getenv("OPENAI_API_KEY")

@router.post("/questions")
def generate_questions(resume: str, jd: str):
    prompt = f"Generate 10 interview questions for candidate resume:\n{resume}\n\nJob Description:\n{jd}"
    resp = openai.ChatCompletion.create(
        model="gpt-4o-mini",
        messages=[{"role":"user","content":prompt}]
    )
    return {"questions": resp.choices[0].message["content"]}
