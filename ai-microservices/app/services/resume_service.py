import re
from typing import List, Dict
from app.models.responses import ResumeAnalysisResponse

class ResumeService:
    def __init__(self, vector_service, llm_service):
        self.vector_service = vector_service
        self.llm_service = llm_service

        # Institution normalization map (extend as needed)
        self.institution_map = {
            "lambton coll": "Lambton College",
            "parul inst of tech": "Parul Institute of Technology",
            "parul inst technology": "Parul Institute of Technology",
            "uoft": "University of Toronto",
            "u of t": "University of Toronto",
            "mit": "Massachusetts Institute of Technology",
            "iit": "Indian Institute of Technology",
            "nyu": "New York University",
        }

    async def analyse(self, resume_text: str, resume_url: str | None, job_description: str | None):
        # --- 1. Extract core features ---
        skills = self._extract_skills(resume_text)
        experience = self._extract_experience(resume_text)
        education = self._extract_education(resume_text)   # normalized degrees + institutions
        certifications = self._extract_certifications(resume_text)

        # --- 2. Compute semantic similarity ---
        semantic_score = 0
        if job_description:
            resume_emb = self.vector_service.embed_texts([resume_text])
            jd_emb = self.vector_service.embed_texts([job_description])
            if resume_emb and jd_emb:
                sim = self._cosine_similarity(resume_emb[0], jd_emb[0])
                semantic_score = sim * 100

        # --- 3. Compute keyword overlap ---
        keyword_score = 0
        if job_description:
            jd_tokens = set(re.findall(r"\b\w+\b", job_description.lower()))
            resume_tokens = set(re.findall(r"\b\w+\b", resume_text.lower()))
            overlap = jd_tokens & resume_tokens
            keyword_score = (len(overlap) / max(len(jd_tokens), 1)) * 100

        # --- 4. Weighted ATS score ---
        ats_score = round(0.7 * semantic_score + 0.3 * keyword_score, 2)

        # --- 5. Fit label ---
        if ats_score >= 80:
            fit_label = "Excellent Fit"
        elif ats_score >= 60:
            fit_label = "Moderate Fit"
        else:
            fit_label = "Needs Improvement"

        # --- 6. Skill gap detection ---
        skill_gap = []
        if job_description:
            jd_tokens = set(re.findall(r"\b\w+\b", job_description.lower()))
            skill_gap = list(jd_tokens - set(skills))

        return ResumeAnalysisResponse(
            ats_score=ats_score,
            fit_label=fit_label,
            skills=skills,
            experience=experience,
            education=education,
            certifications=certifications,
            skill_gap=skill_gap,
        )

    # ------------------- Regex extractors -------------------

    def _extract_skills(self, text: str) -> List[str]:
        tech_keywords = [
            "java", "spring boot", "python", "fastapi", "aws", "docker",
            "kubernetes", "git", "ci", "cd", "ci/cd", "postgresql", "mysql",
            "mongodb", "typescript", "angular", "react", "node"
        ]
        found = [kw for kw in tech_keywords if re.search(rf"\b{re.escape(kw)}\b", text.lower())]
        return list(set(found))

    def _extract_experience(self, text: str) -> List[str]:
        return re.findall(r"(?:Inc\.|Ltd\.|Corporation|Corp\.|LLC|Technologies|Solutions)[^\n]+", text)

    def _extract_education(self, text: str) -> List[Dict[str, str]]:
        """
        Extract structured education: degree, institution, duration.
        Normalize both degree and institution names.
        """
        education = []
        pattern = re.compile(
            r"(?P<degree>Bachelor|Master|B\.Tech|M\.Tech|Diploma|Post\-Graduate Diploma|Ph\.D)[^,.\n]*"
            r".*?(?P<institution>[A-Z][A-Za-z .&]+(?:College|University|Institute|School))"
            r".*?(?P<dates>(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)?\.? ?\d{4}(?: ?[–-] ?(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)?\.? ?\d{4})?)",
            re.DOTALL,
        )

        for match in pattern.finditer(text):
            degree_raw = match.group("degree").strip()
            degree = self._normalize_degree(degree_raw)

            institution_raw = match.group("institution").strip()
            institution = self._normalize_institution(institution_raw)

            education.append({
                "degree": degree,
                "institution": institution,
                "duration": match.group("dates").strip()
            })
        return education

    def _normalize_degree(self, degree: str) -> str:
        degree_map = {
            "b.tech": "Bachelor of Technology",
            "bachelor": "Bachelor’s Degree",
            "master": "Master’s Degree",
            "m.tech": "Master of Technology",
            "ph.d": "Doctor of Philosophy",
            "diploma": "Diploma",
            "post-graduate diploma": "Post-Graduate Diploma"
        }
        key = degree.lower().replace(".", "").strip()
        return degree_map.get(key, degree.title())

    def _normalize_institution(self, institution: str) -> str:
        """Normalize institution names using a dictionary of known variants."""
        key = institution.lower().replace(".", "").replace(",", "").strip()
        for short, full in self.institution_map.items():
            if short in key:
                return full
        return institution.title()

    def _extract_certifications(self, text: str) -> List[str]:
        return re.findall(r"(AWS Certified|Azure Certified|Google Cloud Certified)[^\n]*", text)

    def _cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        dot = sum(a * b for a, b in zip(vec1, vec2))
        norm1 = sum(a * a for a in vec1) ** 0.5
        norm2 = sum(b * b for b in vec2) ** 0.5
        return dot / (norm1 * norm2 + 1e-8)
