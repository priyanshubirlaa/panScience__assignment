package com.panScience.assignment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.panScience.assignment.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByFileId(Long fileId);

}