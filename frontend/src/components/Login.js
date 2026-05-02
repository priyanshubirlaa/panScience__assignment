import { useState } from "react";
import api from "../api";

export default function Login({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const submit = async () => {
    try {
  const res = await api.post(`/api/auth/login`, { username, password });
      const token = res.data.token;
      localStorage.setItem("auth_token", token);
      onLogin(token);
    } catch (err) {
  console.error("Login error:", err);
  const status = err?.response?.status;
  const data = err?.response?.data;
  let msg = "Login failed";
  if (status) msg += ` (status ${status})`;
  if (data) msg += `: ${JSON.stringify(data)}`;
  msg += `\n${err.message}`;
  alert(msg);
    }
  };

  return (
    <div className="card">
      <h2>Login</h2>
      <input placeholder="username" value={username} onChange={(e) => setUsername(e.target.value)} />
      <input placeholder="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button onClick={submit}>Login</button>
    </div>
  );
}
