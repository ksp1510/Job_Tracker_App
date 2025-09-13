"""
Large language model service for the AI microservice.

This module provides an interface for generating text using a locally
hosted HuggingFace causal language model (e.g. GPT-like).  The service
is configured via environment variables defined in ``config/settings.py``
and does not make any calls to external APIs.  Downstream services may
build specialised prompts for resume optimisation, interview questions,
etc., and delegate the generation task to this service.
"""

import logging
from typing import Optional
from transformers import pipeline, AutoModelForCausalLM, AutoTokenizer
import torch

from app.config.settings import settings

logger = logging.getLogger(__name__)


class LLMService:
    """
    Wrapper around a HuggingFace causal language model.
    """

    def __init__(self) -> None:
        """
        Initialise and load HuggingFace causal language model.
        """
        model_name = settings.LLM_MODEL  # e.g., "microsoft/DialoGPT-medium"
        logger.info(f"Loading model: {model_name}")

        # Load tokenizer + model
        self.tokenizer = AutoTokenizer.from_pretrained(model_name, token=None)
        self.model = AutoModelForCausalLM.from_pretrained(model_name, token=None)

        # Always CPU for portability
        self.device = 0 if torch.cuda.is_available() else -1

        # Initialise pipeline
        self.generator = pipeline(
            "text-generation",
            model=self.model,
            tokenizer=self.tokenizer,
            device=self.device
        )

    def generate(
        self,
        prompt: str,
        *,
        temperature: float = 0.7,
        max_tokens: int = 256,
        stop: Optional[str] = None,
    ) -> str:
        """
        Generate continuation text given a prompt.
        """
        if not prompt:
            return ""

        outputs = self.generator(
            prompt,
            max_new_tokens=max_tokens,
            do_sample=True,
            temperature=temperature,
            top_p=0.9,
            num_return_sequences=1
        )

        generated = outputs[0]["generated_text"]

        # Return only continuation (after the prompt)
        if generated.startswith(prompt):
            return generated[len(prompt):].strip()
        return generated.strip()
