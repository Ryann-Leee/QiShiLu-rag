package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Knowledge document entity
 *
 * Stores documents for RAG retrieval
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
    @Index(name = "idx_doc_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_doc_source", columnList = "source"),
    @Index(name = "idx_doc_type", columnList = "docType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36)
    private String tenantId;
    
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 255)
    private String source;
    
    @Column(length = 50)
    private String docType;

    /**
     * Original file name
     */
    @Column(length = 255)
    private String fileName;

    /**
     * Process status
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProcessStatus status = ProcessStatus.PENDING;

    /**
     * Error message if processing failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of chunks
     */
    private Integer chunkCount;

    /**
     * Total tokens in all chunks
     */
    private Integer totalTokens;

    /**
     * Milvus vector ID for the document embedding
     */
    @Column(length = 255)
    private String embeddingId;
    
    /**
     * Chunk index if document is split into multiple chunks
     */
    private Integer chunkIndex;
    
    /**
     * Total chunks for this document
     */
    private Integer totalChunks;
    
    /**
     * Document metadata
     */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String metadata = "{}";
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
