package com.panScience.assignment.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void storeChunk(String text) {
        Map<String, String> body = Map.of("text", text);
        restTemplate.postForObject("http://localhost:8000/store", body, String.class);
    }

    
    public List<String> search(String query) {

    Map<String, String> body = Map.of("text", query);

    Map response = restTemplate.postForObject(
            "http://localhost:8000/search",
            body,
            Map.class
    );

    // 👇 FIX: it is List<String>, NOT List<Map>
    List<String> results = (List<String>) response.get("results");

    if (results == null) return List.of();

    return results;
}
}