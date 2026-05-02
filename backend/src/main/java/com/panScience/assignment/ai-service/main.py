from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from fastapi import UploadFile, File
import faiss
import numpy as np

import whisper


app = FastAPI()

whisper_model = whisper.load_model("base")
# Load model
model = SentenceTransformer('all-MiniLM-L6-v2')

# FAISS setup
dimension = 384
index = faiss.IndexFlatL2(dimension)

# In-memory storage
texts = []
stored_set = set()  # for duplicate prevention


class TextRequest(BaseModel):
    text: str


# ----------- Helper: Chunking -----------
import re

def chunk_text(text):
    sentences = re.split(r'[.!?]', text)
    return [s.strip() for s in sentences if s.strip()]

# ----------- Store API -----------
@app.post("/store")
def store(req: TextRequest):
    chunks = chunk_text(req.text)

    new_chunks = []

    for chunk in chunks:
        if chunk not in stored_set:
            new_chunks.append(chunk)
            stored_set.add(chunk)

    if not new_chunks:
        return {"status": "all chunks already exist"}

    embeddings = model.encode(new_chunks)

    index.add(np.array(embeddings))

    texts.extend(new_chunks)

    return {
        "status": "stored",
        "chunks_added": len(new_chunks),
        "total_chunks": len(texts)
    }


# ----------- Search API -----------
@app.post("/search")
def search(req: TextRequest):
    if len(texts) == 0:
        return {"results": []}

    query_embedding = model.encode([req.text])

    k = min(3, len(texts))

    D, I = index.search(np.array(query_embedding), k)

    results = []
    seen = set()

    for idx in I[0]:
        if idx < len(texts) and texts[idx] not in seen:
            results.append(texts[idx])
            seen.add(texts[idx])

    return {"results": results}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):

    # save temp file
    temp_path = f"temp_{file.filename}"

    with open(temp_path, "wb") as f:
        f.write(await file.read())

    # transcribe
    result = whisper_model.transcribe(temp_path)

    segments = result["segments"]

    # format output
    transcript = []
    for seg in segments:
        transcript.append({
            "text": seg["text"],
            "start": seg["start"],
            "end": seg["end"]
        })

    return {"transcript": transcript}