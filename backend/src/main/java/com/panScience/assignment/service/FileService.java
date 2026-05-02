package com.panScience.assignment.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.panScience.assignment.entity.FileEntity;
import com.panScience.assignment.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final PdfService pdfService;
    private final TranscriptionService transcriptionService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileEntity saveFile(MultipartFile file) throws IOException {

        System.out.println("=== FILE UPLOAD START ===");

        // 1. Create directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        System.out.println("Upload directory: " + uploadPath);

        // 2. Clean filename
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        System.out.println("File name: " + fileName);

        // 3. Resolve path
        Path targetLocation = uploadPath.resolve(fileName);
        System.out.println("Saving to: " + targetLocation);

        // 4. Save file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File saved successfully");

        // 5. Save metadata
        FileEntity entity = new FileEntity();
        entity.setName(fileName);
        entity.setType(file.getContentType());
        entity.setPath(targetLocation.toString());
        entity.setUploadedAt(LocalDateTime.now());

        FileEntity saved = fileRepository.save(entity);

        System.out.println("Saved in DB with ID: " + saved.getId());
        System.out.println("Detected Content-Type: " + file.getContentType());

        // 6. Decide processing type (ROBUST)
        String type = file.getContentType();

        if (type != null && (type.contains("audio") || type.contains("video"))) {
            System.out.println(">>> AUDIO/VIDEO detected. Calling transcription...");
            transcriptionService.transcribeFile(saved);

        } else if (type != null && type.contains("pdf")) {
            System.out.println(">>> PDF detected. Calling PDF processing...");
            pdfService.processPdf(saved);

        } else {
            System.out.println(">>> Unknown file type. No processing triggered.");
        }

        System.out.println("=== FILE UPLOAD END ===");

        return saved;
    }
}