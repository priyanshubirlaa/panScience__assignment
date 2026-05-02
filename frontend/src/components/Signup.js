import { useState } from "react";
import api from "../api";

export default function Signup({ onSignup }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const submit = async () => {
    try {
  const res = await api.post(`/api/auth/register`, { username, password });
      const token = res.data.token;
      localStorage.setItem("auth_token", token);
      onSignup(token);
    } catch (err) {
  console.error("Signup error:", err);
  // show more useful message for common cases
  const status = err?.response?.status;
  const data = err?.response?.data;
  let msg = "Signup failed";
  if (status) msg += ` (status ${status})`;
  if (data) msg += `: ${JSON.stringify(data)}`;
  msg += `\n${err.message}`;
  alert(msg);
    }
  };

  return (
    <div className="card">
      <h2>Signup</h2>
      <input placeholder="username" value={username} onChange={(e) => setUsername(e.target.value)} />
      <input placeholder="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button onClick={submit}>Signup</button>
    </div>
  );
}
