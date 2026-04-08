package com.enterprise.rag.milvus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Milvus service for vector database operations
 * 
 * Note: This is a simplified in-memory implementation.
 * For production, use the actual Milvus SDK with proper vector indexing.
 * 
 * Handles:
 * - Document and memory storage
 * - Vector similarity search (using simple cosine similarity)
 * - Multi-tenant isolation via metadata filtering
 */
@Slf4j
@Service
public class MilvusService {
    
    @Value("${milvus.host:localhost}")
    private String milvusHost;
    
    @Value("${milvus.port:19530}")
    private int milvusPort;
    
    @Value("${llm.embedding-dimension:1536}")
    private int embeddingDimension;
    
    // In-memory storage (for development/demo)
    private final Map<String, DocumentEntry> knowledgeStore = new ConcurrentHashMap<>();
    private final Map<String, MemoryEntry> memoryStore = new ConcurrentHashMap<>();
    
    private static final String KNOWLEDGE_COLLECTION = "knowledge_base";
    private static final String MEMORY_COLLECTION = "episodic_memories";
    
    @PostConstruct
    public void init() {
        log.info("Initialized Milvus service (in-memory mode)");
        log.info("Configuration: host={}, port={}, embedding-dimension={}", 
                milvusHost, milvusPort, embeddingDimension);
    }
    
    /**
     * Check if collection exists
     */
    public boolean hasCollection(String collectionName) {
        return KNOWLEDGE_COLLECTION.equals(collectionName) || 
               MEMORY_COLLECTION.equals(collectionName);
    }
    
    /**
     * Create partition for tenant isolation
     */
    public void createPartition(String collectionName, String partitionName) {
        log.debug("Created partition {} in {} (in-memory mode)", partitionName, collectionName);
    }
    
    /**
     * Insert document into knowledge base
     * 
     * @param content Document content
     * @param embedding Document embedding vector
     * @param tenantId Tenant ID for isolation
     * @param docId Document ID
     * @return Vector ID
     */
    public String insertDocument(String content, float[] embedding, String tenantId, String docId) {
        String vectorId = UUID.randomUUID().toString();
        
        DocumentEntry entry = new DocumentEntry();
        entry.id = vectorId;
        entry.tenantId = tenantId;
        entry.docId = docId;
        entry.content = content;
        entry.embedding = embedding != null ? embedding : generateEmbedding(content);
        entry.source = "upload";
        entry.createdAt = System.currentTimeMillis();
        
        knowledgeStore.put(vectorId, entry);
        log.debug("Inserted document {} for tenant {} (total: {})", docId, tenantId, knowledgeStore.size());
        
        return vectorId;
    }
    
    /**
     * Insert memory into episodic memory collection
     * 
     * @param content Memory content
     * @param embedding Memory embedding vector
     * @param userId User ID
     * @param tenantId Tenant ID for isolation
     * @param memoryId Memory ID
     * @return Vector ID
     */
    public String insertMemory(String content, float[] embedding, String userId, String tenantId, String memoryId) {
        String vectorId = UUID.randomUUID().toString();
        
        MemoryEntry entry = new MemoryEntry();
        entry.id = vectorId;
        entry.userId = userId;
        entry.tenantId = tenantId;
        entry.content = content;
        entry.embedding = embedding != null ? embedding : generateEmbedding(content);
        entry.memoryType = "CONTEXT";
        entry.importanceScore = 0.5f;
        entry.createdAt = System.currentTimeMillis();
        
        memoryStore.put(vectorId, entry);
        log.debug("Inserted memory {} for user {} in tenant {} (total: {})", 
                memoryId, userId, tenantId, memoryStore.size());
        
        return vectorId;
    }
    
    /**
     * Search knowledge base
     * 
     * @param query Query embedding
     * @param tenantId Tenant ID for isolation
     * @param topK Number of results
     * @return List of search results
     */
    public List<SearchResult> searchKnowledge(float[] query, String tenantId, int topK) {
        List<SearchResult> results = new ArrayList<>();
        
        // Filter by tenant and compute similarity
        List<DocumentEntry> entries = knowledgeStore.values().stream()
                .filter(e -> tenantId.equals(e.tenantId))
                .toList();
        
        // Compute similarities and sort
        List<ScoredEntry> scored = new ArrayList<>();
        for (DocumentEntry entry : entries) {
            float score = cosineSimilarity(query, entry.embedding);
            scored.add(new ScoredEntry(entry.id, score, entry));
        }
        
        scored.sort((a, b) -> Float.compare(b.score, a.score));
        
        // Return top K
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            ScoredEntry se = scored.get(i);
            DocumentEntry entry = (DocumentEntry) se.entry;
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("content", entry.content);
            fields.put("doc_id", entry.docId);
            fields.put("source", entry.source);
            
            results.add(SearchResult.builder()
                    .id(entry.id)
                    .score(se.score)
                    .fields(fields)
                    .build());
        }
        
        log.debug("Found {} results for tenant {}", results.size(), tenantId);
        return results;
    }
    
    /**
     * Search episodic memories
     * 
     * @param query Query text
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param topK Number of results
     * @return List of memory IDs
     */
    public List<String> searchMemories(String query, String userId, String tenantId, int topK) {
        float[] queryEmbedding = generateEmbedding(query);
        
        // Filter by user and tenant
        List<MemoryEntry> entries = memoryStore.values().stream()
                .filter(e -> userId.equals(e.userId) && tenantId.equals(e.tenantId))
                .toList();
        
        // Compute similarities and sort
        List<ScoredEntry> scored = new ArrayList<>();
        for (MemoryEntry entry : entries) {
            float score = cosineSimilarity(queryEmbedding, entry.embedding);
            scored.add(new ScoredEntry(entry.id, score, entry));
        }
        
        scored.sort((a, b) -> Float.compare(b.score, a.score));
        
        // Return top K IDs
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            ids.add(scored.get(i).id);
        }
        
        log.debug("Found {} memories for user {} in tenant {}", ids.size(), userId, tenantId);
        return ids;
    }
    
    /**
     * Generate placeholder embedding
     */
    private float[] generateEmbedding(String text) {
        float[] embedding = new float[embeddingDimension];
        
        if (text != null && !text.isEmpty()) {
            int hash = text.hashCode();
            Random random = new Random(hash);
            for (int i = 0; i < embeddingDimension; i++) {
                embedding[i] = random.nextFloat() * 2 - 1;
            }
        }
        
        return embedding;
    }
    
    /**
     * Compute cosine similarity between two vectors
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0.0f;
        }
        
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Check if service is connected
     */
    public boolean isConnected() {
        return true; // Always connected in memory mode
    }
    
    // Inner classes for storage
    private static class DocumentEntry {
        String id;
        String tenantId;
        String docId;
        String content;
        float[] embedding;
        String source;
        long createdAt;
    }
    
    private static class MemoryEntry {
        String id;
        String userId;
        String tenantId;
        String content;
        float[] embedding;
        String memoryType;
        float importanceScore;
        long createdAt;
    }
    
    private static class ScoredEntry {
        String id;
        float score;
        Object entry;
        
        ScoredEntry(String id, float score, Object entry) {
            this.id = id;
            this.score = score;
            this.entry = entry;
        }
    }
    
    /**
     * Search result DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private String id;
        private float score;
        private Map<String, Object> fields;
    }
}
