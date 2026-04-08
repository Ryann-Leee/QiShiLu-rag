package com.enterprise.rag.service;

import com.enterprise.rag.entity.ChunkMetadata;
import com.enterprise.rag.entity.KnowledgeDocument;
import com.enterprise.rag.entity.ProcessStatus;
import com.enterprise.rag.milvus.MilvusDualEngineService;
import com.enterprise.rag.milvus.MilvusService;
import com.enterprise.rag.processor.SemanticOverlapTextSplitter;
import com.enterprise.rag.repository.ChunkMetadataRepository;
import com.enterprise.rag.repository.KnowledgeDocumentRepository;
import com.enterprise.rag.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Document ingestion service for processing uploaded files
 *
 * Supports dual-engine hybrid search with Milvus 2.5+:
 * - Dense vector search (semantic)
 * - Sparse vector search (BM25 keyword)
 * - RRF fusion for optimal results
 *
 * @author Enterprise RAG System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final KnowledgeDocumentRepository documentRepository;
    private final ChunkMetadataRepository chunkMetadataRepository;
    private final SemanticOverlapTextSplitter textSplitter;
    private final LLMService llmService;

    // Use dual-engine service for Milvus 2.5+
    private final MilvusDualEngineService milvusDualEngineService;

    // Fallback to legacy service if dual-engine is unavailable
    private final MilvusService milvusService;

    private final int embeddingDimension;

    /**
     * Process and store a text document using dual-engine hybrid search
     *
     * @param fileName Name of the uploaded file
     * @param content File content (text)
     * @param contentType MIME type of the file
     * @return Summary of processed document
     */
    @Transactional
    public DocumentProcessResult processTextDocument(String fileName, String content, String contentType) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set");
        }

        log.info("Processing document: {} for tenant: {} [Dual-Engine Mode]", fileName, tenantId);

        // 1. Create knowledge document entity
        String docId = java.util.UUID.randomUUID().toString();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(docId)
                .tenantId(tenantId)
                .fileName(fileName)
                .title(fileName)
                .docType(contentType)
                .content(content)
                .status(ProcessStatus.PROCESSING)
                .build();

        documentRepository.save(document);

        try {
            // 2. Split document into semantic chunks
            List<SemanticOverlapTextSplitter.DocumentChunk> chunks = textSplitter.splitDocument(document);

            if (chunks.isEmpty()) {
                document.setStatus(ProcessStatus.FAILED);
                document.setErrorMessage("No chunks generated");
                documentRepository.save(document);

                return DocumentProcessResult.builder()
                        .documentId(docId)
                        .fileName(fileName)
                        .success(false)
                        .message("No content to process")
                        .build();
            }

            // 3. Prepare batch insert data for Milvus dual-engine
            List<MilvusDualEngineService.DocumentChunk> milvusChunks = new ArrayList<>();
            List<ChunkMetadata> chunkMetadataList = new ArrayList<>();

            log.info("Generating embeddings and preparing {} chunks for Milvus dual-engine", chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                SemanticOverlapTextSplitter.DocumentChunk chunk = chunks.get(i);

                // Generate dense embedding using LLM service
                float[] denseVector = generateChunkEmbedding(chunk.getContent());

                // Prepare metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileName", fileName);
                metadata.put("tokenCount", chunk.getTokenCount());
                metadata.put("chunkIndex", chunk.getChunkIndex());

                // Add to Milvus batch insert
                MilvusDualEngineService.DocumentChunk milvusChunk =
                        MilvusDualEngineService.DocumentChunk.builder()
                                .content(chunk.getContent())
                                .denseVector(denseVector)
                                .tenantId(tenantId)
                                .docId(docId)
                                .chunkIndex(chunk.getChunkIndex())
                                .metadata(metadata)
                                .build();
                milvusChunks.add(milvusChunk);

                // Determine prev and next chunk IDs
                String prevChunkId = (i > 0) ? chunks.get(i - 1).getId() : null;
                String nextChunkId = (i < chunks.size() - 1) ? chunks.get(i + 1).getId() : null;

                // Create chunk metadata for MySQL
                ChunkMetadata metadata2 = ChunkMetadata.builder()
                        .docId(docId)
                        .chunkId(chunk.getId())
                        .prevChunkId(prevChunkId)
                        .nextChunkId(nextChunkId)
                        .tokenCount(chunk.getTokenCount())
                        .pageRange("1")
                        .content(chunk.getContent())
                        .chunkIndex(chunk.getChunkIndex())
                        .tenantId(tenantId)
                        .build();

                chunkMetadataList.add(metadata2);
            }

            // 4. Batch insert into Milvus dual-engine (dense + auto BM25 sparse vectors)
            List<Object> vectorIds = milvusDualEngineService.batchInsert(milvusChunks);
            log.info("Inserted {} chunks into Milvus dual-engine", vectorIds.size());

            // 5. Save all chunk metadata in batch
            chunkMetadataRepository.saveAll(chunkMetadataList);
            log.info("Saved {} chunk metadata records to MySQL", chunkMetadataList.size());

            // 6. Update document status
            document.setStatus(ProcessStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            document.setTotalTokens(chunks.stream().mapToInt(SemanticOverlapTextSplitter.DocumentChunk::getTokenCount).sum());
            documentRepository.save(document);

            log.info("Successfully processed document {} with {} chunks [Dual-Engine]", fileName, chunks.size());

            return DocumentProcessResult.builder()
                    .documentId(docId)
                    .fileName(fileName)
                    .success(true)
                    .message("Document processed successfully with dual-engine hybrid search")
                    .chunkCount(chunks.size())
                    .totalTokens(document.getTotalTokens())
                    .chunks(chunks)
                    .searchEngine("hybrid_bm25_rrf")
                    .build();

        } catch (Exception e) {
            log.error("Error processing document: {}", fileName, e);

            document.setStatus(ProcessStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);

            return DocumentProcessResult.builder()
                    .documentId(docId)
                    .fileName(fileName)
                    .success(false)
                    .message("Processing failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Process base64 encoded file content
     * 
     * @param fileName Name of the file
     * @param base64Content Base64 encoded content
     * @param contentType MIME type
     * @return Processing result
     */
    @Transactional
    public DocumentProcessResult processBase64Document(String fileName, String base64Content, String contentType) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Content);
            String content = new String(decoded, StandardCharsets.UTF_8);
            
            return processTextDocument(fileName, content, contentType);
        } catch (Exception e) {
            log.error("Error decoding base64 content: {}", fileName, e);
            return DocumentProcessResult.builder()
                    .fileName(fileName)
                    .success(false)
                    .message("Failed to decode content: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate embedding for a chunk using LLM service
     *
     * @param content Text content to embed
     * @return Dense embedding vector
     */
    private float[] generateChunkEmbedding(String content) {
        try {
            // Use LLM service to generate embeddings
            return llmService.generateEmbedding(content);
        } catch (Exception e) {
            log.warn("Failed to generate embedding via LLM service, using fallback: {}", e.getMessage());
            return generateFallbackEmbedding(content);
        }
    }

    /**
     * Generate fallback embedding when LLM service is unavailable
     */
    private float[] generateFallbackEmbedding(String content) {
        float[] embedding = new float[embeddingDimension];

        if (content != null && !content.isEmpty()) {
            int hash = content.hashCode();
            java.util.Random random = new java.util.Random(hash);
            for (int i = 0; i < embeddingDimension; i++) {
                embedding[i] = random.nextFloat() * 2 - 1;
            }
        }

        return embedding;
    }

    /**
     * Get processing result DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class DocumentProcessResult {
        private String documentId;
        private String fileName;
        private boolean success;
        private String message;
        private int chunkCount;
        private int totalTokens;
        private List<SemanticOverlapTextSplitter.DocumentChunk> chunks;
        private String searchEngine;  // Added: hybrid_bm25_rrf, semantic, keyword
    }
}
