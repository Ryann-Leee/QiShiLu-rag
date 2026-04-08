package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Episodic memory entity
 * 
 * Stores important conversation fragments for long-term memory retrieval.
 * Unlike simple message history, episodic memories are extracted key information
 * that can be semantically searched.
 * 
 * Memory types:
 * - PREFERENCE: User preferences and interests
 * - DECISION: Important decisions made by user
 * - ENTITY: Key entities mentioned (people, projects, etc.)
 * - EMOTION: Emotional state changes
 * - FACT: Important facts about user
 */
@Entity
@Table(name = "episodic_memories", indexes = {
    @Index(name = "idx_episodic_user_id", columnList = "userId"),
    @Index(name = "idx_episodic_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_episodic_type", columnList = "memoryType"),
    @Index(name = "idx_episodic_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodicMemory {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @Column(nullable = false, length = 36)
    private String tenantId;
    
    @Column(length = 36)
    private String conversationId;
    
    /**
     * Memory content (the key information extracted)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * Original message that generated this memory
     */
    @Column(columnDefinition = "TEXT")
    private String originalMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MemoryType memoryType;
    
    /**
     * Importance score (0.0 - 1.0)
     * Higher score = more important = higher retrieval priority
     */
    @Builder.Default
    private Float importanceScore = 0.5f;
    
    /**
     * Number of times this memory has been accessed
     */
    @Builder.Default
    private Integer accessCount = 0;
    
    /**
     * Milvus vector ID for semantic search
     */
    @Column(length = 255)
    private String embeddingId;
    
    /**
     * Additional metadata (entities, keywords, etc.)
     */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String metadata = "{}";
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastAccessedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastAccessedAt == null) {
            lastAccessedAt = createdAt;
        }
    }
    
    /**
     * Memory type enumeration
     */
    public enum MemoryType {
        PREFERENCE,
        DECISION,
        ENTITY,
        EMOTION,
        FACT,
        CONTEXT
    }
    
    /**
     * Increment access count (called when memory is retrieved)
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate memory score based on importance and access frequency
     */
    public double calculateMemoryScore() {
        // Recency factor (decay over time)
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        double recencyFactor = Math.exp(-daysSinceCreation / 30.0); // 30-day half-life
        
        // Access frequency factor
        double accessFactor = Math.log(1 + accessCount) / Math.log(10); // Logarithmic scaling
        
        // Combined score
        return 0.4 * importanceScore + 0.3 * recencyFactor + 0.3 * accessFactor;
    }
}
