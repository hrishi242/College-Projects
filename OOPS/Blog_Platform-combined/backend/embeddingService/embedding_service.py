from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np
from typing import List
import logging
import os

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# Load models at startup
MODEL_CACHE_DIR = os.getenv("MODEL_CACHE_DIR", "./model_cache")
os.makedirs(MODEL_CACHE_DIR, exist_ok=True)

# Model initialization
try:
    logger.info("Loading title embedding model...")
    title_model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2', cache_folder=MODEL_CACHE_DIR)
    logger.info("Title embedding model loaded successfully")
    
    logger.info("Loading content embedding model...")
    content_model = SentenceTransformer('sentence-transformers/all-roberta-large-v1', cache_folder=MODEL_CACHE_DIR)
    logger.info("Content embedding model loaded successfully")
    
    models_loaded = True
except Exception as e:
    logger.error(f"Failed to load models: {str(e)}")
    models_loaded = False

class EmbeddingRequest(BaseModel):
    text: str

class EmbeddingResponse(BaseModel):
    embedding: List[float]
    model: str
    success: bool
    message: str = ""

@app.get("/health")
def health_check():
    return {
        "status": "OK",
        "models_loaded": models_loaded
    }

@app.post("/embed/title")
def embed_title(request: EmbeddingRequest):
    if not models_loaded:
        raise HTTPException(status_code=503, detail="Service unavailable - models not loaded")
    
    try:
        embedding = title_model.encode(request.text)
        return EmbeddingResponse(
            embedding=embedding.tolist(),
            model="all-MiniLM-L6-v2",
            success=True
        )
    except Exception as e:
        logger.error(f"Error generating title embedding: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error generating embedding: {str(e)}")

@app.post("/embed/content")
def embed_content(request: EmbeddingRequest):
    if not models_loaded:
        raise HTTPException(status_code=503, detail="Service unavailable - models not loaded")
    
    try:
        embedding = content_model.encode(request.text)
        return EmbeddingResponse(
            embedding=embedding.tolist(),
            model="all-roberta-large-v1",
            success=True
        )
    except Exception as e:
        logger.error(f"Error generating content embedding: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error generating embedding: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
