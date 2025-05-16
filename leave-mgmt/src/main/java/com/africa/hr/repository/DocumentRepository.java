package com.africa.hr.repository;

import com.africa.hr.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Additional query methods if needed
}