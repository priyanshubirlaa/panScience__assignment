import { useRef, useEffect } from "react";

export default function Player({ videoUrl, timestamp }) {
  const videoRef = useRef(null);

  useEffect(() => {
    if (videoRef.current && videoUrl) {
      videoRef.current.load();
    }
  }, [videoUrl]);

  const playFromTime = () => {
    if (videoRef.current && timestamp != null) {
      videoRef.current.currentTime = timestamp;
      videoRef.current.play();
    }
  };

  // Auto-seek and play when timestamp updates (useful when server returns a timestamp)
  useEffect(() => {
    if (timestamp == null || !videoRef.current) return;

    const el = videoRef.current;

    const tryPlay = () => {
      try {
        // clamp to duration if available
        if (!isNaN(el.duration) && el.duration > 0) {
          el.currentTime = Math.min(timestamp, el.duration - 0.1);
        } else {
          el.currentTime = timestamp;
        }
        const p = el.play();
        if (p && typeof p.then === "function") {
          p.catch(() => {
            // autoplay blocked by browser; user can press play manually
          });
        }
      } catch (e) {
        // ignore errors
      }
    };

    if (el.readyState >= 1) {
      tryPlay();
    } else {
      const onLoaded = () => {
        tryPlay();
        el.removeEventListener("loadedmetadata", onLoaded);
      };
      el.addEventListener("loadedmetadata", onLoaded);
      // also try a fallback after a short delay
      const t = setTimeout(() => tryPlay(), 500);
      return () => clearTimeout(t);
    }
  }, [timestamp]);

  return (
    <div className="card">
      <h2>Player</h2>

      {/* Use audio for wav */}
      <audio ref={videoRef} controls src={videoUrl} />

      {timestamp != null && (
        <>
          <p>🎯 Answer at: {timestamp}s</p>
          <button onClick={playFromTime}>
            ▶ Play from {timestamp}s
          </button>
        </>
      )}
    </div>
  );
}