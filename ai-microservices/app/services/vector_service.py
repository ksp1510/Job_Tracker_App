"""
Vector service for the AI microservice.

This module wraps a ``SentenceTransformer`` model and provides helper
functions for generating sentence embeddings and computing cosine similarity.
It also includes a convenience method for ranking arbitrary items by
similarity to a query string based on a specified text attribute.
"""

from __future__ import annotations

from typing import List, Any, Dict, Iterable, Tuple
import numpy as np
from sentence_transformers import SentenceTransformer


class VectorService:
    """Service for encoding text into embeddings and comparing similarity."""

    def __init__(self, model_name: str = "sentence-transformers/all-MiniLM-L6-v2") -> None:
        """
        Initialise the vector service.

        :param model_name: Name of the HuggingFace sentence transformer model to load.
        """
        # Lazy load the model only once; this can take a few seconds.
        self.model = SentenceTransformer(model_name)

    def embed_text(self, text: str) -> List[float]:
        """Generate an embedding for a single string.

        :param text: The text to embed.
        :return: A list of floats representing the embedding.
        """
        if not text:
            return []
        embedding = self.model.encode([text], convert_to_numpy=True)[0]
        return embedding.tolist()

    def embed_texts(self, texts: Iterable[str]) -> List[List[float]]:
        """Generate embeddings for a list of strings.

        :param texts: An iterable of strings to embed.
        :return: A list of embeddings.
        """
        texts_list = list(texts)
        if not texts_list:
            return []
        embeddings = self.model.encode(texts_list, convert_to_numpy=True)
        return [vec.tolist() for vec in embeddings]

    @staticmethod
    def cosine_similarity(vec1: Iterable[float], vec2: Iterable[float]) -> float:
        """Compute the cosine similarity between two vectors.

        :param vec1: First embedding vector.
        :param vec2: Second embedding vector.
        :return: Cosine similarity in the range [0, 1].
        """
        v1 = np.array(list(vec1), dtype=float)
        v2 = np.array(list(vec2), dtype=float)
        if v1.size == 0 or v2.size == 0:
            return 0.0
        norm_v1 = np.linalg.norm(v1)
        norm_v2 = np.linalg.norm(v2)
        if norm_v1 == 0.0 or norm_v2 == 0.0:
            return 0.0
        sim = np.dot(v1, v2) / (norm_v1 * norm_v2)
        # normalise to [0,1] range (optional but intuitive)
        return float((sim + 1) / 2)

    def rank_items(
        self,
        query_text: str,
        items: List[Dict[str, Any]],
        *,
        text_key: str = "description",
    ) -> List[Tuple[Dict[str, Any], float]]:
        """
        Rank a list of items according to their similarity with a query string.

        :param query_text: The query text to compare against each item's text.
        :param items: A list of dictionaries representing items; each item
            should have a text field indicated by ``text_key``.
        :param text_key: The key in each item dictionary that contains the text
            to embed for similarity comparison.
        :return: A list of tuples (item, similarity) sorted by similarity
            descending.
        """
        if not query_text:
            # If there's no query text, return items with zero similarity.
            return [(item, 0.0) for item in items]
        query_embedding = self.model.encode([query_text], convert_to_numpy=True)[0]
        ranked: List[Tuple[Dict[str, Any], float]] = []
        for item in items:
            text = item.get(text_key, "") or ""
            item_embedding = self.model.encode([text], convert_to_numpy=True)[0]
            sim = self.cosine_similarity(query_embedding, item_embedding)
            ranked.append((item, sim))
        ranked.sort(key=lambda x: x[1], reverse=True)
        return ranked

    async def embed(self, texts: Iterable[str]) -> List[List[float]]:
        """
        Async wrapper to generate embeddings for compatibility with async services.
        """
        return self.embed_texts(texts)
