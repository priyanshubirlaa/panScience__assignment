package com.panScience.assignment.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.panScience.assignment.entity.FileEntity;
import com.panScience.assignment.entity.Transcript;
import com.panScience.assignment.repository.TranscriptRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final TranscriptRepository transcriptRepository;
    private final EmbeddingService embeddingService;
    private final RestTemplate restTemplate = new RestTemplate();

    public void transcribeFile(FileEntity file) {

        System.out.println("=== TRANSCRIPTION START ===");
        System.out.println("File path: " + file.getPath());

        try {
            // 1. Prepare file for request
            FileSystemResource resource = new FileSystemResource(file.getPath());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            System.out.println("Sending file to Python service...");

            // 2. Call Python API
            Map response = restTemplate.postForObject(
                    "http://localhost:8000/transcribe",
                    request,
                    Map.class
            );

            System.out.println("PYTHON RESPONSE: " + response);

            // 3. Extract transcript segments
            List<Map<String, Object>> segments =
                    (List<Map<String, Object>>) response.get("transcript");

            if (segments == null || segments.isEmpty()) {
                System.out.println("No transcript data received!");
                return;
            }

            // 4. Save each segment
            for (Map<String, Object> seg : segments) {

                Transcript t = new Transcript();
                t.setFileId(file.getId());
                t.setText((String) seg.get("text"));

                // ✅ FIXED CASTING (IMPORTANT)
                t.setStartTime(((Number) seg.get("start")).doubleValue());
                t.setEndTime(((Number) seg.get("end")).doubleValue());

                transcriptRepository.save(t);
                embeddingService.storeChunk(t.getText());

                System.out.println("Saved transcript: " + t.getText());
            }

            System.out.println("=== TRANSCRIPTION COMPLETE ===");

        } catch (Exception e) {
            System.out.println("❌ ERROR IN TRANSCRIPTION");
            e.printStackTrace();
        }
    }

    public Transcript findBestMatch(Long fileId, String answer) {

    List<Transcript> transcripts = transcriptRepository.findByFileId(fileId);

    if (transcripts.isEmpty()) return null;

    String lowerAnswer = answer.toLowerCase();

    Transcript bestMatch = null;
    int bestScore = 0;

    for (Transcript t : transcripts) {

        String text = t.getText().toLowerCase();

        int score = 0;

        // simple keyword overlap scoring
        for (String word : lowerAnswer.split(" ")) {
            if (text.contains(word)) {
                score++;
            }
        }

        if (score > bestScore) {
            bestScore = score;
            bestMatch = t;
        }
    }

    return bestMatch;
}
}