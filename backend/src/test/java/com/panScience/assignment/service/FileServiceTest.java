package com.panScience.assignment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import com.panScience.assignment.entity.FileEntity;
import com.panScience.assignment.repository.FileRepository;

class FileServiceTest {

    @Mock
    FileRepository fileRepository;

    @Mock
    PdfService pdfService;

    @Mock
    TranscriptionService transcriptionService;

    FileService fileService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        fileService = new FileService(fileRepository, pdfService, transcriptionService);
        // set uploadDir via reflection since @Value won't be processed in test
        try {
            java.lang.reflect.Field f = FileService.class.getDeclaredField("uploadDir");
            f.setAccessible(true);
            f.set(fileService, "uploads_test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void savesPdfAndProcesses() throws IOException {
        MockMultipartFile mp = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                new ByteArrayInputStream("pdfdata".getBytes()));

        FileEntity saved = new FileEntity();
        saved.setId(10L);
        saved.setName("doc.pdf");
        saved.setUploadedAt(LocalDateTime.now());

        when(fileRepository.save(any(FileEntity.class))).thenReturn(saved);

        FileEntity out = fileService.saveFile(mp);
        assertEquals(10L, out.getId());
        verify(pdfService).processPdf(saved);
    }

    @Test
    void savesAudioAndTranscribes() throws IOException {
        MockMultipartFile mp = new MockMultipartFile("file", "sound.mp3", "audio/mpeg",
                new ByteArrayInputStream("audiodata".getBytes()));

        FileEntity saved = new FileEntity();
        saved.setId(11L);
        saved.setName("sound.mp3");
        saved.setUploadedAt(LocalDateTime.now());

        when(fileRepository.save(any(FileEntity.class))).thenReturn(saved);

        FileEntity out = fileService.saveFile(mp);
        assertEquals(11L, out.getId());
        verify(transcriptionService).transcribeFile(saved);
    }
}
