FROM python:3.11-slim

# Set working directory
WORKDIR /app

# System deps (optional but useful for HF + PyTorch)
RUN apt-get update && \
    apt-get install -y --no-install-recommends git && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*


# Copy requirements first (for Docker cache)
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# (Optional) Install Hugging Face + torch explicitly if not in requirements.txt
RUN pip install --no-cache-dir "transformers>=4.40.0" "torch>=2.2.0"

# Copy app code
COPY . .

# Expose FastAPI port
EXPOSE 8001

# Default run command
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
