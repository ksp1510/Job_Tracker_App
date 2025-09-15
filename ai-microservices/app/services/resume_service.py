import asyncio
import aiohttp
import logging
import re
import time
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime
import io
from pathlib import Path

# File processing libraries
import PyPDF2
import docx
from fastapi import UploadFile

from config.settings import settings
from models.requests import ResumeAnalysisRequest, ResumeOptimizationRequest
from models.responses import (
    ResumeAnalysisResponse, 
    ResumeOptimizationResponse,
    SkillAnalysis, 
    ExperienceAnalysis,
    FileProcessingResponse
)
from services.vector_service import VectorService
from services.llm_service import LLMService

logger = logging.getLogger(__name__)

class ResumeService:
    def __init__(self, vector_service: VectorService):
        self.vector_service = vector_service
        self.llm_service = LLMService()
        
        # Common skill categories and patterns
        self.skill_patterns = {
            "programming_languages": [
                "python", "java", "javascript", "typescript", "c++", "c#", "go", "rust",
                "php", "ruby", "swift", "kotlin", "scala", "r", "matlab", "sql"
            ],
            "frameworks": [
                "react", "angular", "vue", "spring", "spring boot", "django", "flask",
                "express", "node.js", "laravel", "rails", ".net", "hibernate"
            ],
            "databases": [
                "mongodb", "postgresql", "mysql", "redis", "elasticsearch", "cassandra",
                "oracle", "sqlite", "dynamodb", "neo4j"
            ],
            "cloud_platforms": [
                "aws", "azure", "gcp", "google cloud", "docker", "kubernetes", "jenkins",
                "terraform", "ansible", "chef", "puppet"
            ],
            "tools": [
                "git", "jira", "confluence", "postman", "swagger", "maven", "gradle",
                "npm", "webpack", "babel", "jest", "junit", "selenium"
            ],
            "soft_skills": [
                "leadership", "communication", "teamwork", "problem solving", "analytical",
                "creative", "adaptable", "detail oriented", "time management", "critical thinking"
            ]
        }

    async def analyze_resume(self, request: ResumeAnalysisRequest) -> ResumeAnalysisResponse:
        """Comprehensive resume analysis"""
        start_time = time.time()
        
        try:
            # Get resume text
            resume_text = await self._get_resume_text(request)
            
            if not resume_text:
                raise ValueError("Could not extract text from resume")
            
            # Perform different types of analysis
            extracted_skills = await self._extract_skills(resume_text)
            experience_summary = await self._analyze_experience(resume_text)
            education = await self._extract_education(resume_text)
            certifications = await self._extract_certifications(resume_text)
            
            # Analytics
            total_experience = self._calculate_total_experience(experience_summary)
            career_level = self._assess_career_level(total_experience, experience_summary)
            keywords = await self._extract_keywords(resume_text)
            ats_score = await self._calculate_ats_score(resume_text)
            
            # Generate insights and suggestions
            strengths = await self._identify_strengths(resume_text, extracted_skills, experience_summary)
            skill_gaps = await self._identify_skill_gaps(extracted_skills, request.job_description)
            improvements = await self._generate_improvement_suggestions(resume_text, extracted_skills)
            
            processing_time = time.time() - start_time
            
            return ResumeAnalysisResponse(
                success=True,
                user_id=request.user_id,
                extracted_skills=extracted_skills,
                experience_summary=experience_summary,
                education=education,
                certifications=certifications,
                total_experience_years=total_experience,
                career_level=career_level,
                skill_gaps=skill_gaps,
                strengths=strengths,
                improvement_suggestions=improvements,
                keywords=keywords,
                ats_score=ats_score,
                processing_time=processing_time
            )
            
        except Exception as e:
            logger.error(f"Resume analysis failed: {str(e)}")
            raise

    async def optimize_resume_for_job(self, request: ResumeAnalysisRequest) -> ResumeOptimizationResponse:
        """Optimize resume based on job description"""
        start_time = time.time()
        
        try:
            if not request.job_description:
                raise ValueError("Job description is required for optimization")
            
            resume_text = await self._get_resume_text(request)
            
            # Analyze job description
            job_keywords = await self._extract_job_keywords(request.job_description)
            job_skills = await self._extract_job_skills(request.job_description)
            
            # Analyze current resume
            current_analysis = await self.analyze_resume(request)
            original_score = await self._score_resume_for_job(resume_text, request.job_description)
            
            # Generate optimizations
            optimized_sections = await self._optimize_resume_sections(resume_text, request.job_description)
            added_keywords = self._find_missing_keywords(resume_text, job_keywords)
            improved_phrases = await self._suggest_phrase_improvements(resume_text, request.job_description)
            formatting_suggestions = self._generate_formatting_suggestions(resume_text)
            
            # Calculate improvement
            optimized_text = self._apply_optimizations(resume_text, optimized_sections, improved_phrases)
            optimized_score = await self._score_resume_for_job(optimized_text, request.job_description)
            improvement = ((optimized_score - original_score) / original_score) * 100
            
            # Generate specific recommendations
            missing_skills = [skill for skill in job_skills if skill.lower() not in resume_text.lower()]
            recommendations = await self._generate_optimization_recommendations(
                resume_text, request.job_description, missing_skills
            )
            
            processing_time = time.time() - start_time
            
            return ResumeOptimizationResponse(
                success=True,
                user_id=request.user_id,
                optimized_sections=optimized_sections,
                added_keywords=added_keywords,
                improved_phrases=improved_phrases,
                formatting_suggestions=formatting_suggestions,
                original_score=original_score,
                optimized_score=optimized_score,
                improvement_percentage=improvement,
                missing_skills=missing_skills,
                recommendations=recommendations,
                processing_time=processing_time
            )
            
        except Exception as e:
            logger.error(f"Resume optimization failed: {str(e)}")
            raise

    async def process_uploaded_resume(self, file: UploadFile, user_id: Optional[str] = None) -> FileProcessingResponse:
        """Process uploaded resume file"""
        start_time = time.time()
        
        try:
            # Validate file
            if file.size > settings.MAX_FILE_SIZE_MB * 1024 * 1024:
                raise ValueError(f"File size exceeds {settings.MAX_FILE_SIZE_MB}MB limit")
            
            file_extension = Path(file.filename).suffix.lower()
            if file_extension not in settings.ALLOWED_FILE_TYPES:
                raise ValueError(f"File type {file_extension} not supported")
            
            # Read file content
            content = await file.read()
            
            # Extract text based on file type
            if file_extension == '.pdf':
                extracted_text = await self._extract_pdf_text(content)
            elif file_extension == '.docx':
                extracted_text = await self._extract_docx_text(content)
            elif file_extension == '.txt':
                extracted_text = content.decode('utf-8')
            else:
                raise ValueError(f"Unsupported file type: {file_extension}")
            
            # Generate metadata
            metadata = {
                "filename": file.filename,
                "file_size": file.size,
                "file_type": file_extension,
                "word_count": len(extracted_text.split()),
                "character_count": len(extracted_text),
                "processed_at": datetime.now().isoformat()
            }
            
            processing_time = time.time() - start_time
            
            return FileProcessingResponse(
                success=True,
                file_path=file.filename,
                extracted_text=extracted_text,
                metadata=metadata,
                processing_time=processing_time
            )
            
        except Exception as e:
            logger.error(f"File processing failed: {str(e)}")
            raise

    async def _get_resume_text(self, request: ResumeAnalysisRequest) -> Optional[str]:
        """Get resume text from request"""
        if request.resume_text:
            return request.resume_text
        
        if request.resume_file_id:
            # In a real implementation, this would fetch from your Spring Boot backend
            # For now, return None and handle accordingly
            logger.warning(f"Resume file retrieval not implemented for file_id: {request.resume_file_id}")
            return None
        
        return None

    async def _extract_skills(self, resume_text: str) -> List[SkillAnalysis]:
        """Extract skills from resume text"""
        skills = []
        text_lower = resume_text.lower()
        
        for category, skill_list in self.skill_patterns.items():
            for skill in skill_list:
                if skill.lower() in text_lower:
                    # Try to determine proficiency and experience
                    proficiency = self._estimate_skill_proficiency(resume_text, skill)
                    years_exp = self._estimate_skill_experience(resume_text, skill)
                    
                    skill_analysis = SkillAnalysis(
                        skill=skill,
                        proficiency_level=proficiency,
                        years_experience=years_exp,
                        skill_category=category.replace('_', ' ').title()
                    )
                    skills.append(skill_analysis)
        
        # Remove duplicates and sort by relevance
        unique_skills = []
        seen_skills = set()
        
        for skill in skills:
            if skill.skill.lower() not in seen_skills:
                seen_skills.add(skill.skill.lower())
                unique_skills.append(skill)
        
        return unique_skills

    async def _analyze_experience(self, resume_text: str) -> List[ExperienceAnalysis]:
        """Analyze work experience from resume"""
        experience_list = []
        
        # Simple pattern matching for common resume formats
        # This is a basic implementation - can be enhanced with NLP
        
        # Look for job titles and companies
        job_patterns = [
            r'([A-Z][a-z\s]+(?:Engineer|Developer|Manager|Analyst|Specialist|Coordinator|Director|Lead))\s*(?:at|@|\|)\s*([A-Z][A-Za-z\s&,\.]+)',
            r'([A-Z][A-Za-z\s&,\.]+)\s*(?:-|–)\s*([A-Z][a-z\s]+(?:Engineer|Developer|Manager|Analyst|Specialist|Coordinator|Director|Lead))'
        ]
        
        for pattern in job_patterns:
            matches = re.findall(pattern, resume_text)
            for match in matches:
                if len(match) == 2:
                    title, company = match
                    
                    # Extract achievements for this role (simple approach)
                    achievements = self._extract_achievements_for_role(resume_text, title, company)
                    skills = self._extract_skills_for_role(resume_text, title, company)
                    
                    exp_analysis = ExperienceAnalysis(
                        job_title=title.strip(),
                        company=company.strip(),
                        key_achievements=achievements,
                        relevant_skills=skills
                    )
                    experience_list.append(exp_analysis)
        
        return experience_list[:10]  # Limit to 10 most recent

    def _extract_achievements_for_role(self, resume_text: str, title: str, company: str) -> List[str]:
        """Extract achievements for a specific role"""
        # Simple bullet point extraction
        lines = resume_text.split('\n')
        achievements = []
        
        # Find lines that look like achievements
        achievement_indicators = ['•', '◦', '-', '★', 'achieved', 'improved', 'increased', 'reduced', 'led', 'managed', 'developed', 'created']
        
        for line in lines:
            line = line.strip()
            if any(indicator in line.lower() for indicator in achievement_indicators):
                if 10 < len(line) < 200:  # Reasonable achievement length
                    achievements.append(line)
        
        return achievements[:5]  # Top 5 achievements

    def _extract_skills_for_role(self, resume_text: str, title: str, company: str) -> List[str]:
        """Extract skills mentioned for a specific role"""
        # This is a simplified version - would be enhanced with context analysis
        skills_found = []
        
        for category, skill_list in self.skill_patterns.items():
            for skill in skill_list:
                if skill.lower() in resume_text.lower():
                    skills_found.append(skill)
        
        return skills_found[:8]  # Top 8 skills

    async def _extract_education(self, resume_text: str) -> List[Dict[str, str]]:
        """Extract education information"""
        education_list = []
        
        # Common degree patterns
        degree_patterns = [
            r'(Bachelor[\'s]*|Master[\'s]*|PhD|Ph\.D\.|MBA|BS|MS|BA|MA)\s+(?:of\s+|in\s+)?([A-Za-z\s,&]+)(?:\s+(?:from\s+)?([A-Za-z\s,&\.]+University|[A-Za-z\s,&\.]+College|[A-Za-z\s,&\.]+Institute))?',
            r'([A-Za-z\s,&\.]+University|[A-Za-z\s,&\.]+College|[A-Za-z\s,&\.]+Institute)\s*[-–]?\s*(Bachelor[\'s]*|Master[\'s]*|PhD|Ph\.D\.|MBA|BS|MS|BA|MA)\s+(?:of\s+|in\s+)?([A-Za-z\s,&]+)'
        ]
        
        for pattern in degree_patterns:
            matches = re.findall(pattern, resume_text, re.IGNORECASE)
            for match in matches:
                if len(match) >= 2:
                    education_dict = {}
                    if 'university' in pattern.lower() or 'college' in pattern.lower():
                        education_dict['institution'] = match[0] if match[0] else 'Unknown'
                        education_dict['degree'] = match[1] if len(match) > 1 else 'Unknown'
                        education_dict['field'] = match[2] if len(match) > 2 else 'Unknown'
                    else:
                        education_dict['degree'] = match[0] if match[0] else 'Unknown'
                        education_dict['field'] = match[1] if len(match) > 1 else 'Unknown'
                        education_dict['institution'] = match[2] if len(match) > 2 else 'Unknown'
                    
                    education_list.append(education_dict)
        
        return education_list[:5]  # Limit to 5 entries

    async def _extract_certifications(self, resume_text: str) -> List[str]:
        """Extract certifications from resume"""
        cert_patterns = [
            r'(?:Certified|Certification|Certificate)\s+([A-Za-z\s,\(\)\.&]+)',
            r'([A-Z]{2,})\s+(?:Certified|Certification|Certificate)',
            r'(AWS|Azure|Google|Oracle|Microsoft|Cisco|CompTIA|PMI|Scrum|Agile)\s+([A-Za-z\s,\(\)\.&]+)(?:Certified|Certification|Certificate)'
        ]
        
        certifications = []
        
        for pattern in cert_patterns:
            matches = re.findall(pattern, resume_text, re.IGNORECASE)
            for match in matches:
                if isinstance(match, tuple):
                    cert = ' '.join(match).strip()
                else:
                    cert = match.strip()
                
                if cert and len(cert) > 3:
                    certifications.append(cert)
        
        return list(set(certifications))[:10]  # Remove duplicates, limit to 10

    def _calculate_total_experience(self, experience_list: List[ExperienceAnalysis]) -> Optional[int]:
        """Calculate total years of experience"""
        # This is a simplified calculation
        # In practice, you'd parse dates and calculate overlaps
        return max(5, len(experience_list) * 2) if experience_list else 0

    def _assess_career_level(self, total_experience: int, experience_list: List[ExperienceAnalysis]) -> str:
        """Assess career level based on experience"""
        if not total_experience:
            return "Entry Level"
        
        # Look for senior titles
        senior_titles = ['senior', 'lead', 'principal', 'staff', 'manager', 'director', 'vp', 'chief']
        has_senior_titles = any(
            any(title_word in exp.job_title.lower() for title_word in senior_titles)
            for exp in experience_list
        )
        
        if total_experience >= 10 or has_senior_titles:
            return "Senior Level"
        elif total_experience >= 5:
            return "Mid Level"
        elif total_experience >= 2:
            return "Junior Level"
        else:
            return "Entry Level"

    async def _extract_keywords(self, resume_text: str) -> List[str]:
        """Extract important keywords from resume"""
        # Use TF-IDF or similar for keyword extraction
        # For now, simple approach
        words = resume_text.lower().split()
        
        # Filter meaningful words
        meaningful_words = []
        stop_words = {'the', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by'}
        
        for word in words:
            word = re.sub(r'[^a-zA-Z]', '', word)
            if len(word) > 3 and word not in stop_words:
                meaningful_words.append(word)
        
        # Count frequency
        word_count = {}
        for word in meaningful_words:
            word_count[word] = word_count.get(word, 0) + 1
        
        # Return top keywords
        top_keywords = sorted(word_count.items(), key=lambda x: x[1], reverse=True)
        return [word for word, count in top_keywords[:20]]

    async def _calculate_ats_score(self, resume_text: str) -> float:
        """Calculate ATS compatibility score"""
        score = 0.0
        total_checks = 0
        
        # Check for various ATS-friendly elements
        checks = [
            # Has clear sections
            ('experience' in resume_text.lower() or 'work history' in resume_text.lower(), 10),
            ('education' in resume_text.lower(), 10),
            ('skills' in resume_text.lower(), 10),
            
            # Has measurable achievements
            (bool(re.search(r'\d+%|\$\d+|\d+\s+years?', resume_text)), 15),
            
            # Proper formatting
            (len(resume_text.split('\n')) > 10, 10),  # Multiple lines
            (bool(re.search(r'[•◦-]', resume_text)), 10),  # Bullet points
            
            # Contact information
            (bool(re.search(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', resume_text)), 15),
            (bool(re.search(r'\d{3}[-.]?\d{3}[-.]?\d{4}', resume_text)), 10),  # Phone
            
            # Professional keywords
            (len([skill for category in self.skill_patterns.values() for skill in category 
                  if skill.lower() in resume_text.lower()]) > 5, 20)
        ]
        
        for check_passed, points in checks:
            total_checks += points
            if check_passed:
                score += points
        
        return (score / total_checks) * 100 if total_checks > 0 else 0.0

    def _estimate_skill_proficiency(self, resume_text: str, skill: str) -> Optional[str]:
        """Estimate skill proficiency level"""
        text_lower = resume_text.lower()
        skill_lower = skill.lower()
        
        # Look for proficiency indicators
        if f"expert {skill_lower}" in text_lower or f"{skill_lower} expert" in text_lower:
            return "Expert"
        elif f"senior {skill_lower}" in text_lower or f"advanced {skill_lower}" in text_lower:
            return "Advanced"
        elif f"intermediate {skill_lower}" in text_lower:
            return "Intermediate"
        elif f"basic {skill_lower}" in text_lower or f"beginner {skill_lower}" in text_lower:
            return "Beginner"
        else:
            # Default based on context frequency
            skill_mentions = text_lower.count(skill_lower)
            if skill_mentions >= 5:
                return "Advanced"
            elif skill_mentions >= 3:
                return "Intermediate"
            else:
                return "Beginner"

    def _estimate_skill_experience(self, resume_text: str, skill: str) -> Optional[int]:
        """Estimate years of experience with a skill"""
        # Look for patterns like "5 years of Python" or "Python (3 years)"
        patterns = [
            rf'(\d+)\s+years?\s+(?:of\s+)?{re.escape(skill)}',
            rf'{re.escape(skill)}\s+\((\d+)\s+years?\)',
            rf'(\d+)\+?\s+years?\s+.*{re.escape(skill)}'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, resume_text, re.IGNORECASE)
            if match:
                return int(match.group(1))
        
        return None

    async def _identify_strengths(self, resume_text: str, skills: List[SkillAnalysis], experience: List[ExperienceAnalysis]) -> List[str]:
        """Identify resume strengths"""
        strengths = []
        
        # Diverse skill set
        skill_categories = set(skill.skill_category for skill in skills if skill.skill_category)
        if len(skill_categories) >= 4:
            strengths.append("Diverse technical skill set spanning multiple domains")
        
        # Experience breadth
        if len(experience) >= 3:
            strengths.append("Extensive professional experience across multiple roles")
        
        # Leadership indicators
        leadership_keywords = ['led', 'managed', 'directed', 'supervised', 'mentored', 'coordinated']
        if any(keyword in resume_text.lower() for keyword in leadership_keywords):
            strengths.append("Demonstrated leadership and management capabilities")
        
        # Quantified achievements
        if re.search(r'\d+%|\$\d+|increased.*\d+|reduced.*\d+|improved.*\d+', resume_text):
            strengths.append("Quantified achievements showing measurable impact")
        
        # Education
        if 'master' in resume_text.lower() or 'phd' in resume_text.lower() or 'mba' in resume_text.lower():
            strengths.append("Advanced educational qualifications")
        
        return strengths

    async def _identify_skill_gaps(self, current_skills: List[SkillAnalysis], job_description: Optional[str]) -> List[str]:
        """Identify potential skill gaps"""
        if not job_description:
            return []
        
        # Extract skills from job description
        job_skills = await self._extract_job_skills(job_description)
        current_skill_names = [skill.skill.lower() for skill in current_skills]
        
        gaps = []
        for job_skill in job_skills:
            if job_skill.lower() not in current_skill_names:
                gaps.append(job_skill)
        
        return gaps[:10]  # Return top 10 gaps

    async def _generate_improvement_suggestions(self, resume_text: str, skills: List[SkillAnalysis]) -> List[str]:
        """Generate resume improvement suggestions"""
        suggestions = []
        
        # Check for contact information
        if not re.search(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', resume_text):
            suggestions.append("Add a professional email address")
        
        # Check for quantified achievements
        if not re.search(r'\d+%|\$\d+|increased.*\d+|reduced.*\d+|improved.*\d+', resume_text):
            suggestions.append("Add quantified achievements with specific numbers and percentages")
        
        # Check for action verbs
        action_verbs = ['achieved', 'improved', 'developed', 'created', 'led', 'managed', 'implemented']
        if not any(verb in resume_text.lower() for verb in action_verbs):
            suggestions.append("Use more action verbs to describe your accomplishments")
        
        # Check resume length
        word_count = len(resume_text.split())
        if word_count < 300:
            suggestions.append("Expand your resume with more detailed descriptions of your experience")
        elif word_count > 800:
            suggestions.append("Consider condensing your resume to focus on most relevant experiences")
        
        # Check for professional summary
        if not any(keyword in resume_text.lower() for keyword in ['summary', 'profile', 'objective']):
            suggestions.append("Add a professional summary at the beginning of your resume")
        
        # Check skill diversity
        if len(skills) < 8:
            suggestions.append("Consider adding more relevant technical and soft skills")
        
        return suggestions

    async def _extract_job_keywords(self, job_description: str) -> List[str]:
        """Extract important keywords from job description"""
        # Simple keyword extraction - can be enhanced with NLP
        words = job_description.lower().split()
        
        # Filter meaningful words
        meaningful_words = []
        stop_words = {'the', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by', 'is', 'are', 'will', 'be'}
        
        for word in words:
            word = re.sub(r'[^a-zA-Z]', '', word)
            if len(word) > 3 and word not in stop_words:
                meaningful_words.append(word)
        
        # Count frequency
        word_count = {}
        for word in meaningful_words:
            word_count[word] = word_count.get(word, 0) + 1
        
        # Return top keywords
        top_keywords = sorted(word_count.items(), key=lambda x: x[1], reverse=True)
        return [word for word, count in top_keywords[:30]]

    async def _extract_job_skills(self, job_description: str) -> List[str]:
        """Extract skills from job description"""
        skills_found = []
        text_lower = job_description.lower()
        
        for category, skill_list in self.skill_patterns.items():
            for skill in skill_list:
                if skill.lower() in text_lower:
                    skills_found.append(skill)
        
        # Also look for common job requirement patterns
        requirement_patterns = [
            r'(?:experience with|knowledge of|proficient in|expertise in|familiar with)\s+([A-Za-z\s,/]+)',
            r'(?:required|must have|should have):\s*([A-Za-z\s,/]+)',
            r'([A-Za-z]+)\s+(?:experience|skills?|knowledge)'
        ]
        
        for pattern in requirement_patterns:
            matches = re.findall(pattern, job_description, re.IGNORECASE)
            for match in matches:
                if isinstance(match, str) and len(match.strip()) > 2:
                    # Split on common delimiters
                    additional_skills = re.split(r'[,/&\n]', match)
                    for skill in additional_skills:
                        skill = skill.strip()
                        if len(skill) > 2 and skill.lower() not in [s.lower() for s in skills_found]:
                            skills_found.append(skill)
        
        return skills_found[:20]  # Return top 20 skills

    async def _optimize_resume_sections(self, resume_text: str, job_description: str) -> Dict[str, str]:
        """Optimize different sections of the resume"""
        optimized_sections = {}
        
        # This would use LLM to optimize sections
        # For now, return basic optimizations
        
        job_keywords = await self._extract_job_keywords(job_description)
        
        # Professional Summary optimization
        if 'summary' in resume_text.lower() or 'profile' in resume_text.lower():
            optimized_sections['professional_summary'] = await self._optimize_summary(resume_text, job_keywords)
        
        # Skills section optimization
        if 'skills' in resume_text.lower():
            optimized_sections['skills'] = await self._optimize_skills_section(resume_text, job_description)
        
        # Experience optimization suggestions
        optimized_sections['experience_tips'] = await self._generate_experience_optimization_tips(resume_text, job_description)
        
        return optimized_sections

    async def _optimize_summary(self, resume_text: str, job_keywords: List[str]) -> str:
        """Optimize professional summary"""
        # Extract current summary
        summary_pattern = r'(?:SUMMARY|PROFILE|OBJECTIVE|ABOUT)(.*?)(?:\n\s*\n|EXPERIENCE|SKILLS|EDUCATION)'
        match = re.search(summary_pattern, resume_text.upper(), re.DOTALL)
        
        if match:
            current_summary = match.group(1).strip()
        else:
            current_summary = "Professional summary not found"
        
        # Suggest improvements
        suggestions = f"Consider incorporating these keywords: {', '.join(job_keywords[:5])}"
        return f"Current: {current_summary}\n\nSuggestion: {suggestions}"

    async def _optimize_skills_section(self, resume_text: str, job_description: str) -> str:
        """Optimize skills section"""
        job_skills = await self._extract_job_skills(job_description)
        current_skills_analysis = await self._extract_skills(resume_text)
        current_skills = [skill.skill for skill in current_skills_analysis]
        
        missing_skills = [skill for skill in job_skills if skill not in current_skills]
        
        return f"Consider adding these skills if applicable: {', '.join(missing_skills[:8])}"

    async def _generate_experience_optimization_tips(self, resume_text: str, job_description: str) -> str:
        """Generate tips for optimizing experience section"""
        tips = []
        
        # Check for quantified achievements
        if not re.search(r'\d+%|\$\d+|\d+\s+(?:percent|million|thousand)', resume_text):
            tips.append("Add quantified achievements (e.g., 'Improved performance by 25%')")
        
        # Check for action verbs
        action_verbs = ['achieved', 'improved', 'developed', 'created', 'led', 'managed', 'implemented', 'optimized']
        used_verbs = [verb for verb in action_verbs if verb in resume_text.lower()]
        
        if len(used_verbs) < 3:
            tips.append("Use more diverse action verbs to start bullet points")
        
        # Check for relevance to job
        job_keywords = await self._extract_job_keywords(job_description)
        resume_keywords = await self._extract_keywords(resume_text)
        
        common_keywords = set(job_keywords).intersection(set(resume_keywords))
        if len(common_keywords) < 5:
            tips.append("Highlight experiences more relevant to the target role")
        
        return '; '.join(tips)

    def _find_missing_keywords(self, resume_text: str, job_keywords: List[str]) -> List[str]:
        """Find keywords from job description missing in resume"""
        resume_lower = resume_text.lower()
        missing_keywords = []
        
        for keyword in job_keywords:
            if keyword.lower() not in resume_lower:
                missing_keywords.append(keyword)
        
        return missing_keywords[:15]  # Return top 15

    async def _suggest_phrase_improvements(self, resume_text: str, job_description: str) -> List[Dict[str, str]]:
        """Suggest improved phrases"""
        improvements = []
        
        # Common weak phrases and their improvements
        phrase_improvements = {
            "responsible for": "managed",
            "worked on": "developed",
            "helped with": "contributed to",
            "participated in": "collaborated on",
            "familiar with": "experienced in",
            "knowledge of": "proficient in"
        }
        
        for weak_phrase, strong_phrase in phrase_improvements.items():
            if weak_phrase in resume_text.lower():
                improvements.append({
                    "original": weak_phrase,
                    "improved": strong_phrase,
                    "reason": "More action-oriented and impactful"
                })
        
        return improvements[:5]  # Return top 5 improvements

    def _generate_formatting_suggestions(self, resume_text: str) -> List[str]:
        """Generate formatting suggestions"""
        suggestions = []
        
        # Check for consistent bullet points
        bullet_types = re.findall(r'^[\s]*([•◦\-\*])', resume_text, re.MULTILINE)
        if len(set(bullet_types)) > 1:
            suggestions.append("Use consistent bullet point style throughout")
        
        # Check for proper sections
        sections = ['experience', 'education', 'skills']
        missing_sections = [section for section in sections if section not in resume_text.lower()]
        if missing_sections:
            suggestions.append(f"Consider adding these sections: {', '.join(missing_sections)}")
        
        # Check for excessive formatting
        if resume_text.count('*') > 10 or resume_text.count('_') > 10:
            suggestions.append("Avoid excessive bold/italic formatting for better ATS compatibility")
        
        # Check line length
        lines = resume_text.split('\n')
        long_lines = [line for line in lines if len(line) > 100]
        if len(long_lines) > len(lines) * 0.3:
            suggestions.append("Consider breaking up long lines for better readability")
        
        return suggestions

    def _apply_optimizations(self, resume_text: str, optimized_sections: Dict[str, str], improved_phrases: List[Dict[str, str]]) -> str:
        """Apply optimizations to resume text"""
        optimized_text = resume_text
        
        # Apply phrase improvements
        for improvement in improved_phrases:
            optimized_text = optimized_text.replace(improvement['original'], improvement['improved'])
        
        return optimized_text

    async def _score_resume_for_job(self, resume_text: str, job_description: str) -> float:
        """Score resume against job description"""
        try:
            # Get embeddings for both texts
            resume_embedding = await self.vector_service.get_embeddings([resume_text])
            job_embedding = await self.vector_service.get_embeddings([job_description])
            
            # Calculate similarity
            similarity = await self.vector_service.calculate_similarity(
                resume_embedding[0], job_embedding[0]
            )
            
            # Convert to percentage score
            return similarity * 100
            
        except Exception as e:
            logger.error(f"Resume scoring failed: {str(e)}")
            return 50.0  # Default score

    async def _generate_optimization_recommendations(self, resume_text: str, job_description: str, missing_skills: List[str]) -> List[str]:
        """Generate specific optimization recommendations"""
        recommendations = []
        
        if missing_skills:
            recommendations.append(f"Add these relevant skills if you have them: {', '.join(missing_skills[:5])}")
        
        # Check for industry keywords
        job_keywords = await self._extract_job_keywords(job_description)
        resume_keywords = await self._extract_keywords(resume_text)
        
        missing_keywords = [kw for kw in job_keywords[:10] if kw not in resume_keywords]
        if missing_keywords:
            recommendations.append(f"Incorporate these keywords: {', '.join(missing_keywords[:5])}")
        
        # Check for quantified achievements
        if not re.search(r'\d+%|\$\d+', resume_text):
            recommendations.append("Add quantified achievements with specific metrics")
        
        # Check for relevant experience
        if 'years' not in resume_text.lower():
            recommendations.append("Highlight years of experience more clearly")
        
        return recommendations

    async def _extract_pdf_text(self, pdf_content: bytes) -> str:
        """Extract text from PDF content"""
        try:
            pdf_file = io.BytesIO(pdf_content)
            pdf_reader = PyPDF2.PdfReader(pdf_file)
            
            text = ""
            for page in pdf_reader.pages:
                text += page.extract_text() + "\n"
            
            return text.strip()
            
        except Exception as e:
            logger.error(f"PDF text extraction failed: {str(e)}")
            raise ValueError("Could not extract text from PDF")

    async def _extract_docx_text(self, docx_content: bytes) -> str:
        """Extract text from DOCX content"""
        try:
            docx_file = io.BytesIO(docx_content)
            doc = docx.Document(docx_file)
            
            text = ""
            for paragraph in doc.paragraphs:
                text += paragraph.text + "\n"
            
            return text.strip()
            
        except Exception as e:
            logger.error(f"DOCX text extraction failed: {str(e)}")
            raise ValueError("Could not extract text from DOCX")