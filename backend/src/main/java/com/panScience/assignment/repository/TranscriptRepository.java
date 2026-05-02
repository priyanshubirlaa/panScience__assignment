package com.panScience.assignment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.panScience.assignment.entity.Transcript;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {
    List<Transcript> findByFileId(Long fileId);
}