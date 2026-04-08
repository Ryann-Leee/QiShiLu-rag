package com.enterprise.rag.controller;

import com.enterprise.rag.entity.ChunkMetadata;
import com.enterprise.rag.repository.ChunkMetadataRepository;
import com.enterprise.rag.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Chunk Metadata Controller
 * 
 * Provides API endpoints for querying chunk metadata
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@RestController
@RequestMapping("/api/chunks")
@RequiredArgsConstructor
public class ChunkMetadataController {

    private final ChunkMetadataRepository chunkMetadataRepository;

    /**
     * Get all chunks for a document
     * 
     * GET /api/chunks/document/{docId}
     */
    @GetMapping("/document/{docId}")
    public ResponseEntity<?> getChunksByDocument(@PathVariable String docId) {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing tenant context")
            );
        }

        try {
            List<ChunkMetadata> chunks = chunkMetadataRepository
                    .findByDocIdAndTenantIdOrderByChunkIndex(docId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "docId", docId,
                    "count", chunks.size(),
                    "chunks", chunks
            ));
        } catch (Exception e) {
            log.error("Error fetching chunks for document: {}", docId, e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to fetch chunks: " + e.getMessage())
            );
        }
    }

    /**
     * Get a specific chunk by ID
     * 
     * GET /api/chunks/{chunkId}
     */
    @GetMapping("/{chunkId}")
    public ResponseEntity<?> getChunkById(@PathVariable String chunkId) {
        try {
            return chunkMetadataRepository.findByChunkId(chunkId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching chunk: {}", chunkId, e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to fetch chunk: " + e.getMessage())
            );
        }
    }

    /**
     * Get chunk statistics for a document
     * 
     * GET /api/chunks/document/{docId}/stats
     */
    @GetMapping("/document/{docId}/stats")
    public ResponseEntity<?> getChunkStatistics(@PathVariable String docId) {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing tenant context")
            );
        }

        try {
            List<ChunkMetadata> chunks = chunkMetadataRepository
                    .findByDocIdAndTenantIdOrderByChunkIndex(docId, tenantId);

            long totalChunks = chunks.size();
            int totalTokens = chunks.stream()
                    .mapToInt(ChunkMetadata::getTokenCount)
                    .sum();

            double avgTokens = totalChunks > 0 ? (double) totalTokens / totalChunks : 0;
            int maxTokens = chunks.stream()
                    .mapToInt(ChunkMetadata::getTokenCount)
                    .max()
                    .orElse(0);
            int minTokens = chunks.stream()
                    .mapToInt(ChunkMetadata::getTokenCount)
                    .min()
                    .orElse(0);

            return ResponseEntity.ok(Map.of(
                    "docId", docId,
                    "totalChunks", totalChunks,
                    "totalTokens", totalTokens,
                    "avgTokens", String.format("%.2f", avgTokens),
                    "maxTokens", maxTokens,
                    "minTokens", minTokens
            ));
        } catch (Exception e) {
            log.error("Error fetching chunk statistics: {}", docId, e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to fetch statistics: " + e.getMessage())
            );
        }
    }

    /**
     * Get all chunks for a tenant
     * 
     * GET /api/chunks/tenant
     */
    @GetMapping("/tenant")
    public ResponseEntity<?> getChunksByTenant() {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing tenant context")
            );
        }

        try {
            List<ChunkMetadata> chunks = chunkMetadataRepository.findByTenantId(tenantId);

            return ResponseEntity.ok(Map.of(
                    "tenantId", tenantId,
                    "count", chunks.size(),
                    "chunks", chunks
            ));
        } catch (Exception e) {
            log.error("Error fetching chunks for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to fetch chunks: " + e.getMessage())
            );
        }
    }
}
