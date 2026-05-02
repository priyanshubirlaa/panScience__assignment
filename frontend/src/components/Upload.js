import api from "../api";
import { useState } from "react";

export default function Upload({ setFileId, setVideoUrl }) {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);

  const uploadFile = async () => {
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);

    try {
      // axios instance 'api' will attach Authorization header when token is present
      const res = await api.post(`/api/files/upload`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      setFileId(res.data.id);
  setVideoUrl(`${window.location.protocol}//${window.location.hostname}:8080/uploads/${res.data.name}`);
    } catch (err) {
      alert("Upload failed");
    }

    setLoading(false);
  };

  return (
    <div className="card">
      <h2>Upload File</h2>

      <input type="file" onChange={(e) => setFile(e.target.files[0])} />

      <button onClick={uploadFile}>
        {loading ? "Uploading..." : "Upload"}
      </button>
    </div>
  );
}