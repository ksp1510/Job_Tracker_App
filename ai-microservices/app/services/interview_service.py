# app/services/interview_service.py
import time, re, uuid
from app.config.settings import settings
from app.models.requests import InterviewQuestionsRequest
from app.models.responses import InterviewQuestionsResponse, InterviewQuestion, QuestionDifficulty
from app.services.llm_service import LLMService

class InterviewService:
    def __init__(self):
        self.llm = LLMService()  # initialize the local LLM service

    async def generate_questions(self, request: InterviewQuestionsRequest) -> InterviewQuestionsResponse:
        """Generate interview questions based on resume and job description using local LLM."""
        start_time = time.time()
        # Obtain resume text (already provided in request or retrieved via file ID)
        resume_text = request.resume_text
        if not resume_text and request.resume_file_id:
            # If resume text not directly provided, you could fetch it using the file ID.
            # For now, we'll assume resume_text is provided (similar to validate in request model).
            resume_text = ""  
        # Construct the prompt for the LLM
        num_q = request.num_questions or settings.MAX_INTERVIEW_QUESTIONS
        interview_type = request.interview_type.value if request.interview_type else "general"
        difficulty = request.difficulty_level or "medium"
        prompt = (
            f"You are a hiring manager preparing a **{difficulty}** difficulty, **{interview_type}** interview.\n"
            f"Generate {num_q} interview questions for the candidate based on their resume and the job description.\n\n"
            f"**Resume:** {resume_text}\n\n"
            f"**Job Description:** {request.job_description}\n\n"
            f"List each question clearly."
        )
        # Use the local model to generate questions
        raw_output = self.llm.generate_text(prompt, max_new_tokens=500)
        # Split the output into individual questions (assuming they are separated by lines or numbers)
        lines = [line.strip() for line in raw_output.splitlines() if line.strip()]
        questions_list = []
        for line in lines:
            # Remove any leading numbering or bullet points
            q_text = re.sub(r'^\d+[\).\s]*', '', line)
            if q_text.endswith("?"): 
                question_text = q_text
            else:
                question_text = q_text + "?"  # ensure it ends with a question mark
            # Determine category (for simplicity, use interview_type for all questions)
            category = interview_type
            # Create a unique ID for the question
            q_id = str(uuid.uuid4())
            # Set difficulty enum (uses the request's difficulty_level)
            try:
                difficulty_enum = QuestionDifficulty(request.difficulty_level.lower())
            except:
                difficulty_enum = QuestionDifficulty.MEDIUM
            questions_list.append(InterviewQuestion(
                question_id=q_id,
                question=question_text,
                category=category,
                difficulty=difficulty_enum,
                expected_answer_points=[],  # Could be generated or left empty
                followup_questions=[],
                tips=[]
            ))
        processing_time = time.time() - start_time
        return InterviewQuestionsResponse(
            success=True,
            user_id=request.user_id,
            job_title=request.job_title,
            company_name=request.company_name,
            questions=questions_list,
            questions_by_category={},  # could group by category if needed
            key_topics=[],
            skill_focus_areas=[],
            company_research_points=[],
            preparation_tips=[],
            processing_time=processing_time
        )
