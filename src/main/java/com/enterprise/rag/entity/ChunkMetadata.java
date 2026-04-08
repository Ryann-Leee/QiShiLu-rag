package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chunk Metadata Entity
 * 
 * Stores metadata for document chunks in MySQL database
 * 
 * @author Enterprise RAG System
 */
@Data
@Entity
@Table(name = "chunk_metadata", indexes = {
    @Index(name = "idx_doc_id", columnList = "doc_id"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Document ID (corresponding to the original document)
     */
    @Column(name = "doc_id", nullable = false)
    private String docId;

    /**
     * Chunk unique ID
     */
    @Column(name = "chunk_id", nullable = false, unique = true)
    private String chunkId;

    /**
     * Previous chunk ID
     */
    @Column(name = "prev_chunk_id")
    private String prevChunkId;

    /**
     * Next chunk ID
     */
    @Column(name = "next_chunk_id")
    private String nextChunkId;

    /**
     * Token count
     */
    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    /**
     * Source page range (e.g., "1-3", "5")
     */
    @Column(name = "page_range", length = 50)
    private String pageRange;

    /**
     * Chunk content (optional, for quick retrieval)
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Chunk index in the document (0-based)
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * Tenant ID for multi-tenancy
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Timestamps
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
        updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.Instant.now();
    }
}
