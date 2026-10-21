package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocument_IdOrderByVersionNumberDesc(Long documentId);
}
