package com.panScience.assignment.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.panScience.assignment.entity.DocumentChunk;
import com.panScience.assignment.entity.FileEntity;
import com.panScience.assignment.repository.DocumentChunkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final DocumentChunkRepository chunkRepository;

    public void processPdf(FileEntity file) {
        try (PDDocument document = PDDocument.load(new File(file.getPath()))) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            List<String> chunks = splitIntoChunks(text, 800); // ~800 chars

            for (String chunkText : chunks) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setFileId(file.getId());
                chunk.setContent(chunkText);

                chunkRepository.save(chunk);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> splitIntoChunks(String text, int size) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }

        return chunks;
    }
}