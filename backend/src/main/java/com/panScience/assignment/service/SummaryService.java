package com.panScience.assignment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.panScience.assignment.entity.DocumentChunk;
import com.panScience.assignment.entity.Summary;
import com.panScience.assignment.entity.Transcript;
import com.panScience.assignment.repository.DocumentChunkRepository;
import com.panScience.assignment.repository.SummaryRepository;
import com.panScience.assignment.repository.TranscriptRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final DocumentChunkRepository chunkRepository;
    private final TranscriptRepository transcriptRepository;
    private final SummaryRepository summaryRepository;
    private final GeminiService geminiService;

    public String summarize(Long fileId) {

        System.out.println("=== SUMMARY START ===");

        // 1. Check if already exists
        Optional<Summary> existing = summaryRepository.findByFileId(fileId);
        if (existing.isPresent()) {
            System.out.println("Returning cached summary");
            return existing.get().getSummaryText();
        }

        // 2. Collect ALL content (PDF + AUDIO/VIDEO)
        List<String> contentList = new ArrayList<>();

        // 📄 PDF chunks
        List<DocumentChunk> chunks = chunkRepository.findByFileId(fileId);
        for (DocumentChunk c : chunks) {
            contentList.add(c.getContent());
        }

        // 🎧 Audio/Video transcripts
        List<Transcript> transcripts = transcriptRepository.findByFileId(fileId);
        for (Transcript t : transcripts) {
            contentList.add(t.getText());
        }

        System.out.println("Chunks found: " + chunks.size());
        System.out.println("Transcripts found: " + transcripts.size());

        // 3. Handle empty content
        if (contentList.isEmpty()) {
            System.out.println("No content found for fileId: " + fileId);
            return "No content found";
        }

        // 4. Combine text
        String content = String.join(" ", contentList);

        // ⚠️ Limit size (important for LLM)
        if (content.length() > 6000) {
            content = content.substring(0, 6000);
        }

        // 5. Create prompt
        String prompt = """
        Summarize the following content in simple and clear language:

        %s
        """.formatted(content);

        System.out.println("Sending to Gemini...");

        // 6. Call Gemini
        String summaryText = geminiService.generateAnswer(prompt, List.of());

        // 7. Save summary
        Summary summary = new Summary();
        summary.setFileId(fileId);
        summary.setSummaryText(summaryText);

        summaryRepository.save(summary);

        System.out.println("=== SUMMARY COMPLETE ===");

        return summaryText;
    }
}