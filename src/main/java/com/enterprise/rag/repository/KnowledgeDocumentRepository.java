package com.enterprise.rag.repository;

import com.enterprise.rag.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    
    List<KnowledgeDocument> findByTenantId(String tenantId);
    
    Page<KnowledgeDocument> findByTenantId(String tenantId, Pageable pageable);
    
    List<KnowledgeDocument> findByTenantIdAndDocType(String tenantId, String docType);
}
