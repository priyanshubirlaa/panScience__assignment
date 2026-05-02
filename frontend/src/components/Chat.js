import api from "../api";
import { useState } from "react";

export default function Chat({ fileId, setTimestamp, setChatHistory }) {
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  const ask = async () => {
    if (!question || !fileId) return;

    setLoading(true);
    // Try SSE streaming first. EventSource cannot set Authorization header, so pass token as query param.
    const token = localStorage.getItem("auth_token");

    const params = new URLSearchParams({ fileId, question });
    if (token) params.set("access_token", token);

    let usedStreaming = false;

    try {
      const es = new EventSource(`${api.defaults.baseURL}/api/chat/stream?${params.toString()}`);

      let partialAnswer = "";
      usedStreaming = true;

      es.addEventListener("start", (e) => {
        // initialize partial
        partialAnswer = "";
      });

      es.addEventListener("partial", (e) => {
        partialAnswer = e.data;
        // show interim in chat history as a temporary last item
        setChatHistory((prev) => {
          const withoutLastTemp = prev.filter((c) => !(c._temp && c.question === question));
          return [
            ...withoutLastTemp,
            { question, answer: partialAnswer, time: null, _temp: true },
          ];
        });
      });

      // meta contains timing info (startTime/endTime)
      es.addEventListener("meta", (e) => {
        try {
          const meta = JSON.parse(e.data);
          // accept 0 as valid value, so check for null/undefined
          if (meta.startTime !== undefined && meta.startTime !== null) {
            const t = Number(meta.startTime);
            setTimestamp(t);
            // finalize cached entry with metadata
            setChatHistory((prev) => prev.map((c) => c.question === question ? { ...c, time: t } : c));
          }
        } catch (err) {
          // ignore parse errors
        } finally {
          // we've received metadata, we can stop the stream and clear loading
          try { es.close(); } catch (err) {}
          setLoading(false);
        }
      });

      es.addEventListener("done", (e) => {
        const final = e.data;
        // replace temp entry with final (metadata will set time later)
        setChatHistory((prev) => {
          const withoutTemp = prev.filter((c) => !(c._temp && c.question === question));
          return [
            ...withoutTemp,
            { question, answer: final, time: null },
          ];
        });

        setQuestion("");
        // do NOT close here: wait for meta event which arrives after 'done'
      });

      es.addEventListener("error", (e) => {
        console.error("SSE error", e);
        es.close();
        // fall back to POST below
      });

      // fallback timeout if server doesn't stream
      setTimeout(() => {
        if (!partialAnswer) {
          try { es.close(); } catch (e) {}
        }
      }, 10000);

    } catch (err) {
      console.warn("SSE failed, falling back to JSON POST", err);
      usedStreaming = false;
    }

    if (!usedStreaming) {
      try {
        const res = await api.post(`/api/chat/ask`, { fileId, question });

        setTimestamp(res.data.startTime);

        setChatHistory((prev) => [
          ...prev,
          {
            question,
            answer: res.data.answer,
            time: res.data.startTime,
          },
        ]);

        setQuestion("");
      } catch (err) {
        console.error("Error fetching answer:", err);
        const status = err?.response?.status;
        const data = err?.response?.data;
        let msg = "Error fetching answer";
        if (status) msg += ` (status ${status})`;
        if (data) msg += `: ${JSON.stringify(data)}`;
        alert(msg);
      }

      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>Ask Question</h2>

      <div className="chat-box">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask something..."
        />

        <button onClick={ask}>
          {loading ? "Thinking..." : "Ask"}
        </button>
      </div>
    </div>
  );
}