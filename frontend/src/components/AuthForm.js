import { useState } from "react";
import api from "../api";

export default function AuthForm({ onAuth }) {
  const [mode, setMode] = useState("login"); // 'login' or 'signup'
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e?.preventDefault();
    setError(null);
    if (!username || !password) {
      setError("Please enter username and password");
      return;
    }

    setLoading(true);
    try {
      const path = mode === "login" ? "/api/auth/login" : "/api/auth/register";
      const res = await api.post(path, { username, password });
      const token = res?.data?.token;
      if (!token) throw new Error("No token returned from server");
      localStorage.setItem("auth_token", token);
      onAuth(token);
    } catch (err) {
      console.error("Auth error:", err);
      // Handle common network/connection errors
      if (err.message && err.message.toLowerCase().includes("network")) {
        setError("Network error: cannot reach backend. Is the backend running at http://127.0.0.1:8080 ?");
      } else if (err?.response) {
        // server returned a response
        const status = err.response.status;
        const data = err.response.data;
        setError(`Server error${status ? ` (status ${status})` : ""}: ${JSON.stringify(data)}`);
      } else {
        setError(err.message || "Authentication failed");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card auth-card">
      <h2>{mode === "login" ? "Login" : "Sign up"}</h2>

      <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        <input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />

        <input
          placeholder="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete={mode === "login" ? "current-password" : "new-password"}
        />

        <button type="submit" disabled={loading}>
          {loading ? (mode === "login" ? "Signing in..." : "Creating account...") : (mode === "login" ? "Login" : "Signup")}
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      <div style={{ marginTop: 8 }}>
        {mode === "login" ? (
          <span>
            Don't have an account? <button className="link-button" onClick={() => { setMode("signup"); setError(null); }}>Sign up</button>
          </span>
        ) : (
          <span>
            Already signed up? <button className="link-button" onClick={() => { setMode("login"); setError(null); }}>Login here</button>
          </span>
        )}
      </div>
    </div>
  );
}
