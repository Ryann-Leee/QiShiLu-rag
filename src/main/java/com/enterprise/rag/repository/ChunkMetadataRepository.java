package com.enterprise.rag.repository;

import com.enterprise.rag.entity.ChunkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChunkMetadata entity
 * 
 * @author Enterprise RAG System
 */
@Repository
public interface ChunkMetadataRepository extends JpaRepository<ChunkMetadata, String> {

    /**
     * Find all chunks by document ID
     */
    List<ChunkMetadata> findByDocIdOrderByChunkIndex(String docId);

    /**
     * Find a chunk by its ID
     */
    Optional<ChunkMetadata> findByChunkId(String chunkId);

    /**
     * Find chunks by document ID and tenant ID
     */
    List<ChunkMetadata> findByDocIdAndTenantIdOrderByChunkIndex(String docId, String tenantId);

    /**
     * Count chunks by document ID
     */
    long countByDocId(String docId);

    /**
     * Delete all chunks by document ID
     */
    void deleteByDocId(String docId);

    /**
     * Find the first chunk of a document
     */
    Optional<ChunkMetadata> findFirstByDocIdOrderByChunkIndexAsc(String docId);

    /**
     * Find the last chunk of a document
     */
    Optional<ChunkMetadata> findFirstByDocIdOrderByChunkIndexDesc(String docId);

    /**
     * Find chunks by tenant ID
     */
    List<ChunkMetadata> findByTenantId(String tenantId);
}
