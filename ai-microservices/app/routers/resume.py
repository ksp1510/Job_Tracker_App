from fastapi import APIRouter, UploadFile
import fitz, docx

router = APIRouter()

@router.post("/keywords")
async def extract_keywords(file: UploadFile):
    text = ""
    if file.filename.endswith(".pdf"):
        pdf = fitz.open(stream=await file.read(), filetype="pdf")
        for page in pdf: text += page.get_text()
    elif file.filename.endswith(".docx"):
        doc = docx.Document(file.file)
        text = "\n".join([p.text for p in doc.paragraphs])
    keywords = list(set(text.split()))[:30]
    return {"keywords": keywords}
