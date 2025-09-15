import asyncio
import aiohttp
import logging
from typing import List, Dict, Any, Optional
import time
from datetime import datetime

from config.settings import settings, APIConfig, MockDataConfig
from models.requests import JobSearchRequest, JobMatchRequest
from models.responses import JobSearchResponse, JobResult, JobMatchResponse, MatchQuality
from services.vector_service import VectorService
from services.resume_service import ResumeService

logger = logging.getLogger(__name__)

class JobSearchService:
    def __init__(self, vector_service: VectorService):
        self.vector_service = vector_service
        self.resume_service = ResumeService(vector_service)
        
    async def search_jobs(self, request: JobSearchRequest) -> JobSearchResponse:
        """Main job search function that combines multiple sources and AI ranking"""
        start_time = time.time()
        
        try:
            # Get jobs from multiple sources
            all_jobs = []
            
            if settings.USE_MOCK_DATA:
                logger.info("Using mock data for job search")
                jobs = await self._get_mock_jobs(request)
                all_jobs.extend(jobs)
            else:
                # Fetch from real APIs
                logger.info("Fetching from real job APIs")
                theirstack_jobs = await self._search_theirstack(request)
                jsearch_jobs = await self._search_jsearch(request)
                
                all_jobs.extend(theirstack_jobs)
                all_jobs.extend(jsearch_jobs)
            
            # Remove duplicates based on job title and company
            unique_jobs = self._deduplicate_jobs(all_jobs)
            
            # AI-powered ranking if resume is provided
            if request.use_resume_matching and (request.resume_file_id or hasattr(request, 'resume_text')):
                ranked_jobs = await self._rank_jobs_with_ai(unique_jobs, request)
            else:
                ranked_jobs = unique_jobs
            
            # Limit results
            limited_jobs = ranked_jobs[:request.limit]
            
            # Generate AI insights
            ai_insights = await self._generate_search_insights(limited_jobs, request)
            
            processing_time = time.time() - start_time
            
            return JobSearchResponse(
                success=True,
                total_jobs=len(limited_jobs),
                jobs=limited_jobs,
                search_metadata={
                    "query": request.query,
                    "location": request.location,
                    "sources_used": ["mock"] if settings.USE_MOCK_DATA else ["theirstack", "jsearch"],
                    "ai_ranking_applied": request.use_resume_matching,
                    "timestamp": datetime.now().isoformat()
                },
                ai_insights=ai_insights,
                processing_time=processing_time
            )
            
        except Exception as e:
            logger.error(f"Job search failed: {str(e)}")
            raise

    async def match_jobs_to_resume(self, request: JobMatchRequest) -> JobMatchResponse:
        """Match job descriptions to user's resume using semantic similarity"""
        start_time = time.time()
        
        try:
            # Get resume text
            resume_text = await self._get_resume_text(request.user_id, request.resume_text, request.resume_file_id)
            
            if not resume_text:
                raise ValueError("Could not retrieve resume text")
            
            # Convert job descriptions to JobResult objects
            jobs = []
            for i, job_desc in enumerate(request.job_descriptions):
                job = JobResult(
                    job_id=job_desc.get('id', f"job_{i}"),
                    title=job_desc.get('title', 'Unknown Title'),
                    company=job_desc.get('company', 'Unknown Company'),
                    location=job_desc.get('location', 'Unknown Location'),
                    description=job_desc.get('description', ''),
                    skills=job_desc.get('skills', []),
                    salary_min=job_desc.get('salary_min'),
                    salary_max=job_desc.get('salary_max'),
                    employment_type=job_desc.get('employment_type'),
                    remote=job_desc.get('remote', False),
                    posted_date=job_desc.get('posted_date'),
                    job_url=job_desc.get('job_url')
                )
                jobs.append(job)
            
            # Calculate matches
            matched_jobs = await self._calculate_job_matches(jobs, resume_text, request.match_threshold)
            
            # Generate match summary
            match_summary = self._generate_match_summary(matched_jobs)
            
            # Generate recommendations
            recommendations = await self._generate_match_recommendations(matched_jobs, resume_text)
            
            processing_time = time.time() - start_time
            
            return JobMatchResponse(
                success=True,
                user_id=request.user_id,
                total_jobs=len(jobs),
                matched_jobs=matched_jobs,
                match_summary=match_summary,
                recommendations=recommendations,
                processing_time=processing_time
            )
            
        except Exception as e:
            logger.error(f"Job matching failed: {str(e)}")
            raise

    async def _get_mock_jobs(self, request: JobSearchRequest) -> List[JobResult]:
        """Get mock jobs for testing"""
        mock_jobs = MockDataConfig.SAMPLE_JOBS
        
        # Filter based on request criteria
        filtered_jobs = []
        
        for job_data in mock_jobs:
            # Simple filtering logic
            if request.query.lower() in job_data['title'].lower() or \
               request.query.lower() in job_data['description'].lower():
                
                # Check location filter
                if request.location and request.location.lower() not in job_data['location'].lower():
                    continue
                
                # Check salary filter
                if request.salary_min and job_data.get('salary_max', 0) < request.salary_min:
                    continue
                
                if request.salary_max and job_data.get('salary_min', 999999) > request.salary_max:
                    continue
                
                # Check remote filter
                if request.remote_only and not job_data.get('remote', False):
                    continue
                
                job_result = JobResult(
                    job_id=job_data['id'],
                    title=job_data['title'],
                    company=job_data['company'],
                    location=job_data['location'],
                    salary_min=job_data.get('salary_min'),
                    salary_max=job_data.get('salary_max'),
                    description=job_data['description'],
                    skills=job_data.get('skills', []),
                    employment_type=job_data.get('employment_type'),
                    remote=job_data.get('remote', False),
                    posted_date=job_data.get('posted_date'),
                    requirements=self._extract_requirements(job_data['description'])
                )
                
                filtered_jobs.append(job_result)
        
        return filtered_jobs

    async def _search_theirstack(self, request: JobSearchRequest) -> List[JobResult]:
        """Search jobs using TheirStack API"""
        if not settings.THEIRSTACK_API_KEY:
            logger.warning("TheirStack API key not configured")
            return []
        
        try:
            headers = APIConfig.get_job_search_headers("theirstack")
            
            params = {
                "q": request.query,
                "location": request.location,
                "limit": min(request.limit, 50)  # API limit
            }
            
            if request.salary_min:
                params["salary_min"] = request.salary_min
            
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{APIConfig.THEIRSTACK_BASE_URL}/jobs/search",
                    headers=headers,
                    params=params,
                    timeout=aiohttp.ClientTimeout(total=30)
                ) as response:
                    
                    if response.status == 200:
                        data = await response.json()
                        return self._parse_theirstack_jobs(data)
                    else:
                        logger.error(f"TheirStack API error: {response.status}")
                        return []
                        
        except Exception as e:
            logger.error(f"TheirStack search failed: {str(e)}")
            return []

    async def _search_jsearch(self, request: JobSearchRequest) -> List[JobResult]:
        """Search jobs using JSearch API"""
        if not settings.JSEARCH_API_KEY:
            logger.warning("JSearch API key not configured")
            return []
        
        try:
            headers = APIConfig.get_job_search_headers("jsearch")
            
            params = {
                "query": f"{request.query} {request.location or ''}".strip(),
                "num_pages": 1,
                "page": 1
            }
            
            if request.remote_only:
                params["remote_jobs_only"] = "true"
            
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"{APIConfig.JSEARCH_BASE_URL}/search",
                    headers=headers,
                    params=params,
                    timeout=aiohttp.ClientTimeout(total=30)
                ) as response:
                    
                    if response.status == 200:
                        data = await response.json()
                        return self._parse_jsearch_jobs(data)
                    else:
                        logger.error(f"JSearch API error: {response.status}")
                        return []
                        
        except Exception as e:
            logger.error(f"JSearch search failed: {str(e)}")
            return []

    def _parse_theirstack_jobs(self, data: Dict[str, Any]) -> List[JobResult]:
        """Parse TheirStack API response"""
        jobs = []
        
        for job_data in data.get('jobs', []):
            try:
                job = JobResult(
                    job_id=job_data.get('id'),
                    title=job_data.get('title'),
                    company=job_data.get('company', {}).get('name'),
                    location=job_data.get('location'),
                    salary_min=job_data.get('salary', {}).get('min'),
                    salary_max=job_data.get('salary', {}).get('max'),
                    description=job_data.get('description'),
                    skills=job_data.get('skills', []),
                    employment_type=job_data.get('employment_type'),
                    remote=job_data.get('remote', False),
                    posted_date=job_data.get('posted_at'),
                    job_url=job_data.get('url'),
                    requirements=self._extract_requirements(job_data.get('description', ''))
                )
                jobs.append(job)
            except Exception as e:
                logger.warning(f"Failed to parse TheirStack job: {str(e)}")
                continue
        
        return jobs

    def _parse_jsearch_jobs(self, data: Dict[str, Any]) -> List[JobResult]:
        """Parse JSearch API response"""
        jobs = []
        
        for job_data in data.get('data', []):
            try:
                job = JobResult(
                    job_id=job_data.get('job_id'),
                    title=job_data.get('job_title'),
                    company=job_data.get('employer_name'),
                    location=f"{job_data.get('job_city', '')}, {job_data.get('job_state', '')}".strip(', '),
                    salary_min=job_data.get('job_min_salary'),
                    salary_max=job_data.get('job_max_salary'),
                    description=job_data.get('job_description'),
                    employment_type=job_data.get('job_employment_type'),
                    remote=job_data.get('job_is_remote', False),
                    posted_date=job_data.get('job_posted_at_datetime_utc'),
                    job_url=job_data.get('job_apply_link'),
                    company_logo=job_data.get('employer_logo'),
                    requirements=self._extract_requirements(job_data.get('job_description', ''))
                )
                jobs.append(job)
            except Exception as e:
                logger.warning(f"Failed to parse JSearch job: {str(e)}")
                continue
        
        return jobs

    def _deduplicate_jobs(self, jobs: List[JobResult]) -> List[JobResult]:
        """Remove duplicate jobs based on title and company"""
        seen = set()
        unique_jobs = []
        
        for job in jobs:
            key = (job.title.lower().strip(), job.company.lower().strip())
            if key not in seen:
                seen.add(key)
                unique_jobs.append(job)
        
        return unique_jobs

    async def _rank_jobs_with_ai(self, jobs: List[JobResult], request: JobSearchRequest) -> List[JobResult]:
        """Rank jobs using AI based on resume match"""
        try:
            # Get resume text
            resume_text = await self._get_resume_text(request.user_id, None, request.resume_file_id)
            
            if not resume_text:
                logger.warning("No resume text available for ranking")
                return jobs
            
            # Calculate similarity scores for all jobs
            ranked_jobs = await self._calculate_job_matches(jobs, resume_text, threshold=0.0)  # No threshold for ranking
            
            # Sort by match score (descending)
            ranked_jobs.sort(key=lambda x: x.match_score or 0, reverse=True)
            
            return ranked_jobs
            
        except Exception as e:
            logger.error(f"AI ranking failed: {str(e)}")
            return jobs

    async def _calculate_job_matches(self, jobs: List[JobResult], resume_text: str, threshold: float = 0.7) -> List[JobResult]:
        """Calculate semantic similarity between jobs and resume"""
        try:
            # Prepare texts for embedding
            job_texts = []
            for job in jobs:
                job_text = f"{job.title} {job.description} {' '.join(job.skills)} {' '.join(job.requirements)}"
                job_texts.append(job_text)
            
            # Get embeddings
            resume_embedding = await self.vector_service.get_embeddings([resume_text])
            job_embeddings = await self.vector_service.get_embeddings(job_texts)
            
            # Calculate similarities
            matched_jobs = []
            
            for i, job in enumerate(jobs):
                try:
                    similarity = await self.vector_service.calculate_similarity(
                        resume_embedding[0], job_embeddings[i]
                    )
                    
                    if similarity >= threshold:
                        # Set match information
                        job.match_score = similarity
                        job.match_quality = self._get_match_quality(similarity)
                        job.matching_keywords = self._find_matching_keywords(resume_text, job.description)
                        job.missing_skills = self._find_missing_skills(resume_text, job.skills)
                        
                        matched_jobs.append(job)
                        
                except Exception as e:
                    logger.warning(f"Failed to calculate similarity for job {job.job_id}: {str(e)}")
                    continue
            
            return matched_jobs
            
        except Exception as e:
            logger.error(f"Job matching calculation failed: {str(e)}")
            return jobs

    def _get_match_quality(self, score: float) -> MatchQuality:
        """Convert similarity score to match quality"""
        if score >= 0.9:
            return MatchQuality.EXCELLENT
        elif score >= 0.75:
            return MatchQuality.GOOD
        elif score >= 0.6:
            return MatchQuality.FAIR
        else:
            return MatchQuality.POOR

    def _find_matching_keywords(self, resume_text: str, job_description: str) -> List[str]:
        """Find keywords that appear in both resume and job description"""
        # Simple keyword matching (can be enhanced with NLP)
        resume_words = set(word.lower() for word in resume_text.split())
        job_words = set(word.lower() for word in job_description.split())
        
        common_words = resume_words.intersection(job_words)
        
        # Filter out common stop words
        stop_words = {'the', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by', 'is', 'are', 'was', 'were', 'be', 'been', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should', 'may', 'might', 'can', 'must', 'shall'}
        
        meaningful_words = [word for word in common_words if len(word) > 2 and word not in stop_words]
        
        return meaningful_words[:10]  # Return top 10

    def _find_missing_skills(self, resume_text: str, job_skills: List[str]) -> List[str]:
        """Find skills mentioned in job but missing from resume"""
        resume_lower = resume_text.lower()
        missing_skills = []
        
        for skill in job_skills:
            if skill.lower() not in resume_lower:
                missing_skills.append(skill)
        
        return missing_skills

    def _extract_requirements(self, description: str) -> List[str]:
        """Extract requirements from job description"""
        # Simple extraction (can be enhanced with NLP)
        lines = description.split('\n')
        requirements = []
        
        for line in lines:
            line = line.strip()
            if any(keyword in line.lower() for keyword in ['require', 'must have', 'need', 'should have', 'experience with']):
                if len(line) > 10 and len(line) < 200:  # Reasonable requirement length
                    requirements.append(line)
        
        return requirements[:5]  # Return top 5

    async def _get_resume_text(self, user_id: str, resume_text: Optional[str], resume_file_id: Optional[str]) -> Optional[str]:
        """Get resume text from either provided text or file ID"""
        if resume_text:
            return resume_text
        
        if resume_file_id:
            # This would integrate with your Spring Boot backend to get resume content
            # For now, return None - implement based on your file storage system
            logger.warning(f"Resume file retrieval not implemented for file_id: {resume_file_id}")
            return None
        
        return None

    def _generate_match_summary(self, matched_jobs: List[JobResult]) -> Dict[str, Any]:
        """Generate summary of job matches"""
        if not matched_jobs:
            return {"message": "No jobs matched the criteria"}
        
        total_jobs = len(matched_jobs)
        avg_match_score = sum(job.match_score or 0 for job in matched_jobs) / total_jobs
        
        quality_counts = {}
        for job in matched_jobs:
            quality = job.match_quality.value if job.match_quality else "unknown"
            quality_counts[quality] = quality_counts.get(quality, 0) + 1
        
        top_companies = {}
        for job in matched_jobs:
            top_companies[job.company] = top_companies.get(job.company, 0) + 1
        
        return {
            "total_matches": total_jobs,
            "average_match_score": round(avg_match_score, 2),
            "quality_distribution": quality_counts,
            "top_companies": dict(sorted(top_companies.items(), key=lambda x: x[1], reverse=True)[:5]),
            "salary_range": {
                "min": min((job.salary_min or 0 for job in matched_jobs), default=0),
                "max": max((job.salary_max or 0 for job in matched_jobs), default=0)
            }
        }

    async def _generate_match_recommendations(self, matched_jobs: List[JobResult], resume_text: str) -> List[str]:
        """Generate AI recommendations based on job matches"""
        recommendations = []
        
        if not matched_jobs:
            recommendations.append("No jobs matched your criteria. Consider broadening your search terms or location.")
            return recommendations
        
        # Analyze missing skills across all jobs
        all_missing_skills = []
        for job in matched_jobs:
            all_missing_skills.extend(job.missing_skills or [])
        
        # Count frequency of missing skills
        skill_counts = {}
        for skill in all_missing_skills:
            skill_counts[skill] = skill_counts.get(skill, 0) + 1
        
        # Get top missing skills
        top_missing = sorted(skill_counts.items(), key=lambda x: x[1], reverse=True)[:3]
        
        if top_missing:
            skills_text = ", ".join([skill for skill, _ in top_missing])
            recommendations.append(f"Consider adding these in-demand skills to your resume: {skills_text}")
        
        # Analyze match quality distribution
        excellent_jobs = [job for job in matched_jobs if job.match_quality == MatchQuality.EXCELLENT]
        good_jobs = [job for job in matched_jobs if job.match_quality == MatchQuality.GOOD]
        
        if len(excellent_jobs) > 0:
            recommendations.append(f"You have {len(excellent_jobs)} excellent matches. Focus on these high-quality opportunities first.")
        
        if len(good_jobs) > len(excellent_jobs):
            recommendations.append("Many jobs are good matches but could be excellent with minor resume improvements.")
        
        # Location analysis
        remote_jobs = [job for job in matched_jobs if job.remote]
        if len(remote_jobs) > len(matched_jobs) * 0.3:
            recommendations.append("Many remote opportunities are available. Highlight your remote work capabilities.")
        
        # Salary analysis
        salaries = [job.salary_max or job.salary_min for job in matched_jobs if job.salary_max or job.salary_min]
        if salaries:
            avg_salary = sum(salaries) / len(salaries)
            recommendations.append(f"Average salary for matched jobs: ${avg_salary:,.0f}. Ensure your expectations align.")
        
        return recommendations

    async def _generate_search_insights(self, jobs: List[JobResult], request: JobSearchRequest) -> Dict[str, Any]:
        """Generate AI insights about the search results"""
        insights = {
            "market_analysis": {},
            "skill_trends": {},
            "recommendations": [],
            "search_optimization": {}
        }
        
        if not jobs:
            insights["recommendations"].append("No jobs found. Try broader search terms or different locations.")
            return insights
        
        # Market analysis
        companies = [job.company for job in jobs]
        company_counts = {}
        for company in companies:
            company_counts[company] = company_counts.get(company, 0) + 1
        
        insights["market_analysis"] = {
            "total_jobs": len(jobs),
            "unique_companies": len(set(companies)),
            "top_hiring_companies": dict(sorted(company_counts.items(), key=lambda x: x[1], reverse=True)[:5]),
            "remote_percentage": len([job for job in jobs if job.remote]) / len(jobs) * 100
        }
        
        # Skill analysis
        all_skills = []
        for job in jobs:
            all_skills.extend(job.skills or [])
        
        skill_counts = {}
        for skill in all_skills:
            skill_counts[skill] = skill_counts.get(skill, 0) + 1
        
        insights["skill_trends"] = {
            "most_demanded_skills": dict(sorted(skill_counts.items(), key=lambda x: x[1], reverse=True)[:10]),
            "total_unique_skills": len(set(all_skills))
        }
        
        # Salary insights
        salaries = []
        for job in jobs:
            if job.salary_min:
                salaries.append(job.salary_min)
            if job.salary_max:
                salaries.append(job.salary_max)
        
        if salaries:
            insights["market_analysis"]["salary_insights"] = {
                "average": sum(salaries) / len(salaries),
                "min": min(salaries),
                "max": max(salaries)
            }
        
        # Generate recommendations
        top_skills = list(skill_counts.keys())[:5]
        if top_skills:
            insights["recommendations"].append(f"Top in-demand skills: {', '.join(top_skills)}")
        
        if insights["market_analysis"]["remote_percentage"] > 50:
            insights["recommendations"].append("High percentage of remote jobs available in this search.")
        
        return insights