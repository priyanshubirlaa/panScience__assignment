package com.panScience.assignment.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateAnswer(String question, List<String> contextChunks) {

        String context = String.join("\n", contextChunks);

        String prompt = "Answer the question based only on the context below.\n\n"
                + "Context:\n" + context + "\n\n"
                + "Question: " + question;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map response = restTemplate.postForObject(url, body, Map.class);

        // extract response safely
        List candidates = (List) response.get("candidates");
        Map first = (Map) candidates.get(0);
        Map content = (Map) first.get("content");
        List parts = (List) content.get("parts");
        Map textPart = (Map) parts.get(0);

        return (String) textPart.get("text");
    }
}