package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMatter_IdOrderByCreatedAtDesc(Long matterId);
}
