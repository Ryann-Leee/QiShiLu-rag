package com.enterprise.rag.controller;

import com.enterprise.rag.dto.DocumentRequest;
import com.enterprise.rag.entity.KnowledgeDocument;
import com.enterprise.rag.milvus.MilvusService;
import com.enterprise.rag.repository.KnowledgeDocumentRepository;
import com.enterprise.rag.service.LLMService;
import com.enterprise.rag.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Knowledge base controller
 * 
 * Handles document upload and management for RAG
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    
    private final KnowledgeDocumentRepository documentRepository;
    private final MilvusService milvusService;
    private final LLMService llmService;
    
    /**
     * Upload a document to the knowledge base
     */
    @PostMapping
    public ResponseEntity<KnowledgeDocument> uploadDocument(@Valid @RequestBody DocumentRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Uploading document for tenant: {}", tenantId);
        
        // Create document entity
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .title(request.getTitle())
                .content(request.getContent())
                .source(request.getSource())
                .docType(request.getDocType())
                .build();
        
        // Generate embedding
        float[] embedding = llmService.generateEmbedding(request.getContent());
        
        // Store in Milvus
        String embeddingId = milvusService.insertDocument(request.getContent(), embedding, tenantId, document.getId());
        document.setEmbeddingId(embeddingId);
        
        // Save to database
        document = documentRepository.save(document);
        
        return ResponseEntity.ok(document);
    }
    
    /**
     * Get all documents for a tenant
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeDocument>> getDocuments() {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<KnowledgeDocument> documents = documentRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get documents with pagination
     */
    @GetMapping("/page")
    public ResponseEntity<Page<KnowledgeDocument>> getDocumentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Page<KnowledgeDocument> documents = documentRepository.findByTenantId(tenantId, PageRequest.of(page, size));
        return ResponseEntity.ok(documents);
    }
}
