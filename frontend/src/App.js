import { useState } from "react";
import Upload from "./components/Upload";
import Chat from "./components/Chat";
import Player from "./components/Player";
import AuthForm from "./components/AuthForm";
import api from "./api";
import axios from "axios";
import "./App.css";

// Use numeric loopback to avoid potential IPv6/localhost resolution issues
const API_BASE = "http://127.0.0.1:8080";

function App() {
  const [fileId, setFileId] = useState(null);
  const [videoUrl, setVideoUrl] = useState("");
  const [timestamp, setTimestamp] = useState(null);
  const [chatHistory, setChatHistory] = useState([]);
  const [summary, setSummary] = useState("");
  const [summaryLoading, setSummaryLoading] = useState(false);

  const fetchSummary = async () => {
    if (!fileId) return;
    try {
  setSummaryLoading(true);
  const res = await api.get(`/api/summary/${fileId}`);
  console.log("fetchSummary response:", res);

      // Support both JSON { summary: "..." } and plain text responses
      const returned = res?.data?.summary ?? (typeof res.data === "string" ? res.data : null);

      if (!returned) {
        // if nothing matched, show the whole payload for debugging
        setSummary(JSON.stringify(res.data));
      } else {
        setSummary(returned);
      }
    } catch (err) {
      console.error("Failed to fetch summary", err);
      const message = err?.response?.data || err.message || "Unknown error";
      alert("Failed to fetch summary: " + message);
    } finally {
      setSummaryLoading(false);
    }
  };

  const [token, setToken] = useState(localStorage.getItem("auth_token") || null);

  const handleLogout = () => {
    localStorage.removeItem("auth_token");
    setToken(null);
  };

  return (
    <div className="container">
      <h1>AI Multimedia Q&A</h1>

      <div style={{ fontSize: 12, color: '#666', marginBottom: 8 }}>
        API: {window.location ? `${window.location.protocol}//${window.location.hostname}:8080` : 'http://127.0.0.1:8080'}
      </div>

      {/* Auth */}
      {!token ? (
        <AuthForm onAuth={(t) => setToken(t)} />
      ) : (
        <div className="card">
          <p>Logged in</p>
          <button onClick={handleLogout}>Logout</button>
        </div>
      )}


      {/* Main app - only visible when authenticated */}
      {token ? (
        <>
          <Upload setFileId={setFileId} setVideoUrl={setVideoUrl} />

          {/* File Info */}
          {fileId && (
            <div className="card">
              <h3>Uploaded File</h3>
              <p><b>ID:</b> {fileId}</p>
              <p><b>Name:</b> {videoUrl.split("/").pop()}</p>
            </div>
          )}

          {/* Player */}
          {videoUrl && (
            <Player videoUrl={videoUrl} timestamp={timestamp} />
          )}

          {/* Summary */}
          {fileId && (
            <div className="card">
              <button onClick={fetchSummary} disabled={summaryLoading}>
                {summaryLoading ? "Generating..." : "Generate Summary"}
              </button>

              {summary && (
                <>
                  <h2>Summary</h2>
                  <p>{summary}</p>
                </>
              )}
            </div>
          )}

          {/* Chat */}
          {fileId && (
            <Chat
              fileId={fileId}
              setTimestamp={setTimestamp}
              setChatHistory={setChatHistory}
            />
          )}
        </>
      ) : (
        <div style={{ marginTop: 20 }}>
          <p>Please login or signup to access the app.</p>
        </div>
      )}

      {/* Chat History */}
      <div>
        {chatHistory.map((chat, i) => (
          <div key={i} className="chat-item">
            <p><b>Q:</b> {chat.question}</p>
            <p><b>A:</b> {chat.answer}</p>
            {chat.time && <p className="timestamp">⏱ {chat.time}s</p>}
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;