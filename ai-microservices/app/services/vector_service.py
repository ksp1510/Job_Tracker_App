import asyncio
import logging
import numpy as np
from typing import List, Dict, Any, Optional, Tuple
import chromadb
from chromadb.config import Settings
from sentence_transformers import SentenceTransformer
import torch
from pathlib import Path

from config.settings import settings

logger = logging.getLogger(__name__)

class VectorService:
    def __init__(self):
        self.embedding_model = None
        self.chroma_client = None
        self.collections = {}
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        logger.info(f"Vector service will use device: {self.device}")

    async def initialize(self):
        """Initialize the vector service"""
        try:
            # Initialize embedding model
            logger.info(f"Loading embedding model: {settings.EMBEDDING_MODEL}")
            self.embedding_model = SentenceTransformer(settings.EMBEDDING_MODEL)
            
            if self.device == "cuda":
                self.embedding_model = self.embedding_model.cuda()
            
            # Initialize ChromaDB
            logger.info("Initializing ChromaDB")
            db_path = Path(settings.VECTOR_DB_PATH)
            db_path.mkdir(parents=True, exist_ok=True)
            
            self.chroma_client = chromadb.PersistentClient(
                path=str(db_path),
                settings=Settings(
                    anonymized_telemetry=False,
                    allow_reset=True
                )
            )
            
            # Create collections
            await self._create_collections()
            
            logger.info("Vector service initialized successfully")
            
        except Exception as e:
            logger.error(f"Vector service initialization failed: {str(e)}")
            raise

    async def _create_collections(self):
        """Create ChromaDB collections for different data types"""
        try:
            collection_configs = [
                ("resumes", "Store resume embeddings and metadata"),
                ("jobs", "Store job description embeddings and metadata"),
                ("skills", "Store skill embeddings and metadata"),
                ("interview_questions", "Store interview question embeddings")
            ]
            
            for name, description in collection_configs:
                try:
                    collection = self.chroma_client.create_collection(
                        name=name,
                        metadata={"description": description}
                    )
                    self.collections[name] = collection
                    logger.info(f"Created collection: {name}")
                except Exception as e:
                    if "already exists" in str(e).lower():
                        collection = self.chroma_client.get_collection(name=name)
                        self.collections[name] = collection
                        logger.info(f"Retrieved existing collection: {name}")
                    else:
                        logger.error(f"Failed to create collection {name}: {str(e)}")
                        
        except Exception as e:
            logger.error(f"Collection creation failed: {str(e)}")
            raise

    async def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        """Generate embeddings for a list of texts"""
        try:
            if not texts:
                return []
            
            # Clean and preprocess texts
            cleaned_texts = [self._preprocess_text(text) for text in texts]
            
            # Generate embeddings
            embeddings = self.embedding_model.encode(
                cleaned_texts,
                convert_to_tensor=False,
                show_progress_bar=False,
                batch_size=32
            )
            
            # Convert to list format
            if isinstance(embeddings, np.ndarray):
                embeddings = embeddings.tolist()
            
            return embeddings
            
        except Exception as e:
            logger.error(f"Embedding generation failed: {str(e)}")
            raise

    async def calculate_similarity(self, embedding1: List[float], embedding2: List[float]) -> float:
        """Calculate cosine similarity between two embeddings"""
        try:
            # Convert to numpy arrays
            vec1 = np.array(embedding1)
            vec2 = np.array(embedding2)
            
            # Calculate cosine similarity
            dot_product = np.dot(vec1, vec2)
            norm1 = np.linalg.norm(vec1)
            norm2 = np.linalg.norm(vec2)
            
            if norm1 == 0 or norm2 == 0:
                return 0.0
            
            similarity = dot_product / (norm1 * norm2)
            
            # Ensure similarity is between 0 and 1
            similarity = max(0.0, min(1.0, similarity))
            
            return float(similarity)
            
        except Exception as e:
            logger.error(f"Similarity calculation failed: {str(e)}")
            return 0.0

    async def store_resume(self, user_id: str, resume_id: str, resume_text: str, metadata: Optional[Dict] = None) -> bool:
        """Store resume embedding in vector database"""
        try:
            # Generate embedding
            embeddings = await self.get_embeddings([resume_text])
            
            if not embeddings:
                raise ValueError("Failed to generate embedding")
            
            # Prepare metadata
            doc_metadata = {
                "user_id": user_id,
                "resume_id": resume_id,
                "text_length": len(resume_text),
                "created_at": str(np.datetime64('now'))
            }
            
            if metadata:
                doc_metadata.update(metadata)
            
            # Store in ChromaDB
            collection = self.collections.get("resumes")
            if collection:
                collection.add(
                    embeddings=embeddings,
                    documents=[resume_text[:1000]],  # Store first 1000 chars
                    metadatas=[doc_metadata],
                    ids=[f"{user_id}_{resume_id}"]
                )
                
                logger.info(f"Stored resume embedding for user {user_id}")
                return True
            else:
                logger.error("Resumes collection not available")
                return False
                
        except Exception as e:
            logger.error(f"Resume storage failed: {str(e)}")
            return False

    async def store_job(self, job_id: str, job_text: str, metadata: Optional[Dict] = None) -> bool:
        """Store job description embedding in vector database"""
        try:
            # Generate embedding
            embeddings = await self.get_embeddings([job_text])
            
            if not embeddings:
                raise ValueError("Failed to generate embedding")
            
            # Prepare metadata
            doc_metadata = {
                "job_id": job_id,
                "text_length": len(job_text),
                "created_at": str(np.datetime64('now'))
            }
            
            if metadata:
                doc_metadata.update(metadata)
            
            # Store in ChromaDB
            collection = self.collections.get("jobs")
            if collection:
                collection.add(
                    embeddings=embeddings,
                    documents=[job_text[:1000]],  # Store first 1000 chars
                    metadatas=[doc_metadata],
                    ids=[job_id]
                )
                
                logger.info(f"Stored job embedding for job {job_id}")
                return True
            else:
                logger.error("Jobs collection not available")
                return False
                
        except Exception as e:
            logger.error(f"Job storage failed: {str(e)}")
            return False

    async def find_similar_resumes(self, query_text: str, limit: int = 10, user_id: Optional[str] = None) -> List[Dict]:
        """Find resumes similar to query text"""
        try:
            # Generate query embedding
            query_embeddings = await self.get_embeddings([query_text])
            
            if not query_embeddings:
                return []
            
            collection = self.collections.get("resumes")
            if not collection:
                return []
            
            # Build where clause for user filtering
            where_clause = None
            if user_id:
                where_clause = {"user_id": {"$eq": user_id}}
            
            # Query similar documents
            results = collection.query(
                query_embeddings=query_embeddings,
                n_results=limit,
                where=where_clause,
                include=['metadatas', 'documents', 'distances']
            )
            
            # Format results
            similar_resumes = []
            if results['ids'] and results['ids'][0]:
                for i, doc_id in enumerate(results['ids'][0]):
                    similar_resumes.append({
                        'id': doc_id,
                        'metadata': results['metadatas'][0][i] if results['metadatas'] else {},
                        'document': results['documents'][0][i] if results['documents'] else '',
                        'similarity': 1 - results['distances'][0][i] if results['distances'] else 0.0
                    })
            
            return similar_resumes
            
        except Exception as e:
            logger.error(f"Similar resume search failed: {str(e)}")
            return []

    async def find_similar_jobs(self, query_text: str, limit: int = 10, filters: Optional[Dict] = None) -> List[Dict]:
        """Find jobs similar to query text"""
        try:
            # Generate query embedding
            query_embeddings = await self.get_embeddings([query_text])
            
            if not query_embeddings:
                return []
            
            collection = self.collections.get("jobs")
            if not collection:
                return []
            
            # Query similar documents
            results = collection.query(
                query_embeddings=query_embeddings,
                n_results=limit,
                where=filters,
                include=['metadatas', 'documents', 'distances']
            )
            
            # Format results
            similar_jobs = []
            if results['ids'] and results['ids'][0]:
                for i, doc_id in enumerate(results['ids'][0]):
                    similar_jobs.append({
                        'id': doc_id,
                        'metadata': results['metadatas'][0][i] if results['metadatas'] else {},
                        'document': results['documents'][0][i] if results['documents'] else '',
                        'similarity': 1 - results['distances'][0][i] if results['distances'] else 0.0
                    })
            
            return similar_jobs
            
        except Exception as e:
            logger.error(f"Similar job search failed: {str(e)}")
            return []

    async def match_resume_to_jobs(self, resume_text: str, job_texts: List[str]) -> List[Tuple[int, float]]:
        """Match a resume to multiple job descriptions and return similarity scores"""
        try:
            # Generate embeddings
            resume_embeddings = await self.get_embeddings([resume_text])
            job_embeddings = await self.get_embeddings(job_texts)
            
            if not resume_embeddings or not job_embeddings:
                return []
            
            resume_embedding = resume_embeddings[0]
            matches = []
            
            # Calculate similarity with each job
            for i, job_embedding in enumerate(job_embeddings):
                similarity = await self.calculate_similarity(resume_embedding, job_embedding)
                matches.append((i, similarity))
            
            # Sort by similarity (descending)
            matches.sort(key=lambda x: x[1], reverse=True)
            
            return matches
            
        except Exception as e:
            logger.error(f"Resume-job matching failed: {str(e)}")
            return []

    async def cluster_similar_content(self, texts: List[str], num_clusters: int = 5) -> Dict[str, List[int]]:
        """Cluster texts based on similarity"""
        try:
            if len(texts) < num_clusters:
                # If fewer texts than clusters, each text is its own cluster
                return {f"cluster_{i}": [i] for i in range(len(texts))}
            
            # Generate embeddings
            embeddings = await self.get_embeddings(texts)
            
            if not embeddings:
                return {}
            
            # Simple clustering using cosine similarity
            # For more advanced clustering, use sklearn or similar
            embeddings_array = np.array(embeddings)
            
            # Calculate similarity matrix
            similarity_matrix = np.zeros((len(embeddings), len(embeddings)))
            for i in range(len(embeddings)):
                for j in range(len(embeddings)):
                    similarity_matrix[i][j] = await self.calculate_similarity(embeddings[i], embeddings[j])
            
            # Simple greedy clustering
            clusters = {}
            assigned = set()
            cluster_id = 0
            
            for i in range(len(texts)):
                if i in assigned:
                    continue
                
                cluster_name = f"cluster_{cluster_id}"
                clusters[cluster_name] = [i]
                assigned.add(i)
                
                # Find similar texts
                for j in range(i + 1, len(texts)):
                    if j not in assigned and similarity_matrix[i][j] > 0.7:
                        clusters[cluster_name].append(j)
                        assigned.add(j)
                
                cluster_id += 1
                
                if cluster_id >= num_clusters:
                    break
            
            # Add remaining texts to their own clusters
            for i in range(len(texts)):
                if i not in assigned:
                    cluster_name = f"cluster_{cluster_id}"
                    clusters[cluster_name] = [i]
                    cluster_id += 1
            
            return clusters
            
        except Exception as e:
            logger.error(f"Content clustering failed: {str(e)}")
            return {}

    async def get_collection_stats(self) -> Dict[str, Any]:
        """Get statistics about stored collections"""
        stats = {}
        
        try:
            for name, collection in self.collections.items():
                try:
                    count = collection.count()
                    stats[name] = {
                        "document_count": count,
                        "status": "active"
                    }
                except Exception as e:
                    stats[name] = {
                        "document_count": 0,
                        "status": f"error: {str(e)}"
                    }
                    
        except Exception as e:
            logger.error(f"Failed to get collection stats: {str(e)}")
            
        return stats

    async def delete_user_data(self, user_id: str) -> bool:
        """Delete all data for a specific user"""
        try:
            success = True
            
            for collection_name in ["resumes"]:
                collection = self.collections.get(collection_name)
                if collection:
                    try:
                        # Get documents for the user
                        results = collection.get(
                            where={"user_id": {"$eq": user_id}},
                            include=['metadatas']
                        )
                        
                        if results['ids']:
                            collection.delete(ids=results['ids'])
                            logger.info(f"Deleted {len(results['ids'])} documents from {collection_name} for user {user_id}")
                        
                    except Exception as e:
                        logger.error(f"Failed to delete from {collection_name}: {str(e)}")
                        success = False
            
            return success
            
        except Exception as e:
            logger.error(f"User data deletion failed: {str(e)}")
            return False

    async def backup_collection(self, collection_name: str, backup_path: str) -> bool:
        """Backup a collection to file"""
        try:
            collection = self.collections.get(collection_name)
            if not collection:
                logger.error(f"Collection {collection_name} not found")
                return False
            
            # Get all documents from collection
            results = collection.get(include=['embeddings', 'metadatas', 'documents'])
            
            if not results['ids']:
                logger.info(f"No documents to backup in {collection_name}")
                return True
            
            # Save to file (would implement actual file saving)
            backup_data = {
                'collection_name': collection_name,
                'documents': results,
                'backup_timestamp': str(np.datetime64('now'))
            }
            
            logger.info(f"Backup prepared for {collection_name} with {len(results['ids'])} documents")
            
            return True
            
        except Exception as e:
            logger.error(f"Collection backup failed: {str(e)}")
            return False

    def _preprocess_text(self, text: str) -> str:
        """Preprocess text before embedding"""
        if not text:
            return ""
        
        # Basic preprocessing
        text = text.strip()
        
        # Remove excessive whitespace
        text = " ".join(text.split())
        
        # Truncate if too long (model limitations)
        max_length = 512  # Common transformer limit
        if len(text) > max_length:
            text = text[:max_length]
        
        return text

    async def cleanup(self):
        """Cleanup resources"""
        try:
            logger.info("Cleaning up vector service resources")
            
            # Clear model from GPU memory if using CUDA
            if self.device == "cuda" and self.embedding_model:
                del self.embedding_model
                torch.cuda.empty_cache()
            
            self.embedding_model = None
            self.collections = {}
            
            logger.info("Vector service cleanup completed")
            
        except Exception as e:
            logger.error(f"Vector service cleanup failed: {str(e)}")
            