package com.panScience.assignment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.panScience.assignment.entity.DocumentChunk;
import com.panScience.assignment.entity.Summary;
import com.panScience.assignment.entity.Transcript;
import com.panScience.assignment.repository.DocumentChunkRepository;
import com.panScience.assignment.repository.SummaryRepository;
import com.panScience.assignment.repository.TranscriptRepository;

class SummaryServiceTest {

    @Mock
    DocumentChunkRepository chunkRepo;

    @Mock
    TranscriptRepository transcriptRepo;

    @Mock
    SummaryRepository summaryRepo;

    @Mock
    GeminiService geminiService;

    SummaryService summaryService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        summaryService = new SummaryService(chunkRepo, transcriptRepo, summaryRepo, geminiService);
    }

    @Test
    void returnsCachedIfExists() {
        Summary s = new Summary();
        s.setFileId(1L);
        s.setSummaryText("cached");
        when(summaryRepo.findByFileId(1L)).thenReturn(Optional.of(s));

        String out = summaryService.summarize(1L);
        assertEquals("cached", out);
        verifyNoInteractions(geminiService);
    }

    @Test
    void summarizesCombinedContent() {
        DocumentChunk c = new DocumentChunk();
        c.setContent("hello world");
        when(chunkRepo.findByFileId(2L)).thenReturn(List.of(c));

        Transcript t = new Transcript();
        t.setText("audio text");
        when(transcriptRepo.findByFileId(2L)).thenReturn(List.of(t));

        when(geminiService.generateAnswer(anyString(), anyList())).thenReturn("summary-out");
        when(summaryRepo.save(any())).thenAnswer(i -> { Summary ss = i.getArgument(0); ss.setId(5L); return ss; });

        String out = summaryService.summarize(2L);
        assertEquals("summary-out", out);
        verify(summaryRepo).save(any(Summary.class));
    }

    @Test
    void returnsMessageWhenNoContent() {
        when(chunkRepo.findByFileId(3L)).thenReturn(List.of());
        when(transcriptRepo.findByFileId(3L)).thenReturn(List.of());

        String out = summaryService.summarize(3L);
        assertEquals("No content found", out);
        verifyNoInteractions(geminiService);
    }
}
