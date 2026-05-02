package com.panScience.assignment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.panScience.assignment.entity.Summary;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findByFileId(Long fileId);
}