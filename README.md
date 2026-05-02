# AI Multimedia Q&A — PanScience

This repository is an AI-powered multimedia Q&A web app with a Spring Boot backend, a React frontend, and optional local Python-based AI helpers. This README covers setup, environment variables, running & testing, API documentation (including SSE streaming), and Python packages required for local AI services.

## Checklist (what I'll cover)
- Repo overview and required system packages
- Backend (Java/Spring) setup, env vars, run/build/test
- Frontend (React) setup, run/build/test
- Python AI helper environment and packages to install
- API documentation for all endpoints (auth, files, summary, chat + SSE)
- Notes on rate-limiting, SSE token handling, and troubleshooting

---

## Prerequisites
- Git
- Java 17+ (or the version your build requires) and JDK installed (used by `./mvnw`)
- Maven wrapper is included — `./mvnw` will work without system Maven
- Node.js and npm (Node 16+ recommended)
- Python 3.8+ (3.10/3.11 recommended) for optional local AI helpers
- System FFmpeg (required by audio processing):

For Debian/Ubuntu:
```bash
sudo apt update
sudo apt install -y ffmpeg libsndfile1
```

On macOS (Homebrew):
```bash
brew install ffmpeg
```

Make sure you have enough disk/memory if you plan to run local ML models (torch, sentence-transformers, etc.).

---

## Environment / Configuration
The backend reads some properties from `src/main/resources/application.properties`, but it's easiest to override with environment variables.
Key values you should set (examples):

- `SPRING_DATASOURCE_URL` (or set property `spring.datasource.url`) — e.g. `jdbc:mysql://localhost:3306/ai_app`
- `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` if using a DB that requires it
- `JWT_SECRET` (or property `jwt.secret`) — change from the default `change_me_replace` to a strong secret
- `GEMINI_API_KEY` (or property `gemini.api-key`) — your Google Gemini/API key for the cloud LLM

You can export env vars before starting the backend, for example:

```bash
export GEMINI_API_KEY="YOUR_GEMINI_KEY"
export JWT_SECRET="a_very_secure_secret_here"
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/ai_app"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="password"
```

Notes:
- The app allows CORS from `http://localhost:3000` (frontend dev server).
- EventSource (SSE) cannot set custom headers, so the backend supports an `access_token` query param for streaming endpoints (see SSE docs below).
- Rate limiting for `/api/auth/register` and `/api/auth/login` is enforced (1 minute window, max 10 attempts per IP). If you see HTTP 429, wait a minute.

---

## Backend (Spring Boot)
Project is located in `backend/`.

Build & run:

```bash
cd backend
# build jar (skip tests for faster iteration)
./mvnw -DskipTests package

# run from the built jar
java -jar target/assignment-0.0.1-SNAPSHOT.jar

# or run via the maven spring-boot plugin during development
./mvnw spring-boot:run
```

Run unit tests:

```bash
./mvnw test
```

Notes:
- If you change the Java version, ensure your local JDK matches the project's target.
- The backend exposes API on `http://localhost:8080` by default.

---

## Frontend (React)
Located in `frontend/`.

Install and start dev server:

```bash
cd frontend
npm install
npm start
```

Build for production:

```bash
npm run build
```

Run tests:

```bash
npm test
```

The UI expects the backend at `http://localhost:8080` (config in `src/api.js`).

---

## Python AI helper environment (optional/local)
This project contains references to Python AI helper scripts/services (for local transcription, embeddings, etc.). If you plan to run them locally, create a virtual environment and install the packages below.

Create and activate venv (recommended location: `backend/venv`):

```bash
cd backend
python3 -m venv venv
source venv/bin/activate
python -m pip install --upgrade pip
```

Install common packages used for AI helpers:

```bash
pip install fastapi uvicorn sentence-transformers faiss-cpu torch openai-whisper ffmpeg-python
```

Notes and alternatives:
- `torch` installation may vary by platform and GPU support. For CPU-only systems you can `pip install torch --index-url https://download.pytorch.org/whl/cpu`. Refer to PyTorch docs for the right wheel for your platform.
- The `openai-whisper` (aka `whisper`) package depends on `ffmpeg` being installed on the system.
- `faiss-cpu` can be large; if not available for your platform use `pip install faiss-cpu` or fallback to other vector stores.

Run a local FastAPI helper (example):

```bash
# example command (adjust module name/path to your ai-service script)
uvicorn ai_service:app --reload --host 127.0.0.1 --port 8001
```

Adjust any backend configuration to call the local AI helper instead of remote APIs if needed.

---

## API Documentation
All backend endpoints are prefixed with `/api`.

1) Authentication

- POST /api/auth/register
  - Description: Register a new user.
  - Request JSON: { "username": "alice", "password": "secret" }
  - Responses:
    - 200: { "token": "JWT_TOKEN" }
    - 400: { "error": "..." }
    - 429: Too many requests (rate-limited)

- POST /api/auth/login
  - Description: Login an existing user.
  - Request JSON: { "username": "alice", "password": "secret" }
  - Responses:
    - 200: { "token": "JWT_TOKEN" }
    - 401: { "error": "..." }
    - 429: Too many requests (rate-limited)

Rate limiting: register & login endpoints are limited to 10 attempts per IP per minute — excessive attempts return HTTP 429.

2) File upload

- POST /api/files/upload
  - Content-Type: multipart/form-data
  - Form field: `file` (the uploaded audio/video/pdf)
  - Response: JSON describing saved file entity (id, filename, path, etc.) on success.

3) Summary

- GET /api/summary/{fileId}
  - Description: returns a text summary generated for the uploaded file.
  - Response: { "summary": "..." }

4) Chat (single-shot JSON)

- POST /api/chat/ask
  - Request JSON: { "fileId": 123, "question": "What happened at 2:10?" }
  - Authentication: Bearer token required in `Authorization: Bearer <token>` header
  - Response JSON: { "answer": "...", "startTime": 12.3, "endTime": 15.8 }
    - `startTime` and `endTime` are times (seconds) in the transcript matched to the answer; they may be null.

5) Chat streaming (Server-Sent Events / SSE)

- GET /api/chat/stream?fileId=<id>&question=<url-encoded>&access_token=<JWT_TOKEN>
  - Description: Stream the AI answer as it is produced. Use EventSource on the client.
  - Note: Browsers' EventSource cannot set custom headers, so pass the JWT as `access_token` query parameter.
  - Events emitted by the server (SSE event names):
    - `start` — indicates stream started
    - `partial` — partial/accumulated answer text (data contains partial string)
    - `done` — final complete answer text
    - `meta` — JSON string containing metadata, e.g. `{ "startTime": 12.3, "endTime": 15.8 }`
    - `error` — error message

Client-side example (browser):

```javascript
// example using EventSource
const token = localStorage.getItem('auth_token');
const params = new URLSearchParams({ fileId: '123', question: 'What happened?', access_token: token });
const es = new EventSource(`http://localhost:8080/api/chat/stream?${params.toString()}`);

es.addEventListener('partial', (e) => {
  // e.data has partial answer text
});

es.addEventListener('meta', (e) => {
  const meta = JSON.parse(e.data);
  // meta.startTime -> seek audio player
});

es.addEventListener('done', (e) => {
  // final answer in e.data
  es.close();
});
```

Important: Because EventSource sends a long-lived GET request, the backend accepts the token via `access_token` query param. This is intentional and limited to the streaming endpoint.

---

## How timestamp -> audio playback works (UI behavior)
- When the chat system returns `startTime`/`endTime` in JSON or the streaming `meta` event contains `startTime`, the frontend calls `setTimestamp(startTime)`.
- The `Player` component listens for timestamp changes, seeks the audio element to that time (seconds) and starts playback.

---

## Testing & Troubleshooting
- If you see HTTP 429 when calling `/api/auth/*`, you hit the rate limiter (10 attempts / minute per IP). Wait or reset the IP window.
- If SSE doesn't receive `meta`, ensure streams are processed and the server is sending `meta` after `done`. The client must not close the EventSource before `meta` arrives.
- If `startTime` is present but the audio doesn't play from that time:
  - Ensure the `Player` component receives the timestamp prop and seeks the HTMLAudioElement: `audio.currentTime = timestamp`.
  - Ensure the audio file is accessible from `/uploads/...` and that `WebConfig` allows static resource serving.
- Database issues: ensure `spring.datasource.*` is correctly configured.

---

## Additional notes
- This README gives a full developer-focused setup. For production deployment you should:
  - Use HTTPS
  - Use a robust token management and refresh flow
  - Replace in-memory rate-limiting with a distributed limiter (Redis) if horizontal scaling
  - Remove `access_token` query passing if you adopt a proxy that can set headers for EventSource or use websockets / client that supports headers

---

## Contributing
PRs welcome. Run existing tests locally before submitting. Keep changes small and focused.

---

## License
(Insert your project's license here)

