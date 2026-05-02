package com.panScience.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.panScience.assignment.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}