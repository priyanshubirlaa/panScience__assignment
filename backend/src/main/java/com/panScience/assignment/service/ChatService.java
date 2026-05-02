package com.panScience.assignment.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.panScience.assignment.entity.Transcript;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final EmbeddingService embeddingService;
    private final GeminiService geminiService;
    private final TranscriptionService transcriptionService;

   public Map<String, Object> askQuestion(Long fileId, String question) {

    List<String> chunks = embeddingService.search(question);

    String answer = geminiService.generateAnswer(question, chunks);

    Transcript match = transcriptionService.findBestMatch(fileId, answer);

    Map<String, Object> response = new HashMap<>();
    response.put("answer", answer);

    if (match != null) {
        response.put("startTime", match.getStartTime());
        response.put("endTime", match.getEndTime());
    } else {
        response.put("startTime", null);
        response.put("endTime", null);
    }

    return response;
}

    // Streaming version that pushes incremental answer tokens to the provided SseEmitter
    public void askQuestionStream(Long fileId, String question, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {

        try {
            List<String> chunks = embeddingService.search(question);

            // Start stream: send a 'start' event
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("start").data("stream-started"));

            // We'll simulate streaming by calling geminiService.generateAnswer and emitting progressively.
            // If Gemini API supported streaming we'd hook into it; here we split the final answer into pieces.
            String answer = geminiService.generateAnswer(question, chunks);

            // naive split into sentences/parts for streaming
            String[] parts = answer.split("(?<=[\\.!?])\\s+");
            StringBuilder accumulated = new StringBuilder();

            for (String p : parts) {
                accumulated.append(p).append(" ");
                // send partial
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("partial").data(accumulated.toString()));
                // small pause to simulate streaming
                try { Thread.sleep(150); } catch (InterruptedException e) { /* ignore */ }
            }

            // final part
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("done").data(answer));

            // find transcript match
            Transcript match = transcriptionService.findBestMatch(fileId, answer);

            Map<String, Object> meta = new java.util.HashMap<>();
            if (match != null) {
                meta.put("startTime", match.getStartTime());
                meta.put("endTime", match.getEndTime());
            }

            // send meta as JSON string to make parsing reliable on the client
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(meta);
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("meta").data(json));
            } catch (Exception ex) {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("meta").data(meta.toString()));
            }

        } catch (Exception e) {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("error").data(e.getMessage()));
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}