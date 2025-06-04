FROM python:3.9-slim
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    python3-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY embedding_service.py .

# Environment variables
ENV MODEL_CACHE_DIR=/app/model_cache
ENV PORT=8000

# Create cache directory
RUN mkdir -p ${MODEL_CACHE_DIR}

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT}/health || exit 1

EXPOSE 8000

CMD ["uvicorn", "embedding_service:app", "--host", "0.0.0.0", "--port", "8000"]
