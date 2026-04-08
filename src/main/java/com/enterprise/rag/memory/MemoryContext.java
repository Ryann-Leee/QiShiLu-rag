package com.enterprise.rag.memory;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Memory context for RAG queries
 * 
 * Contains all memory information relevant to the current conversation
 */
@Data
@Builder
public class MemoryContext {
    
    /**
     * Session memory: recent messages in the current conversation
     */
    private List<MessageInfo> sessionMemory;
    
    /**
     * Episodic memories: relevant past experiences
     */
    private List<EpisodicMemoryInfo> episodicMemories;
    
    /**
     * User profile information
     */
    private UserProfileInfo userProfile;
    
    /**
     * Key entities mentioned in previous conversations
     */
    private List<String> keyEntities;
    
    /**
     * Formatted context string for LLM prompt
     */
    private String formattedContext;
    
    @Data
    @Builder
    public static class MessageInfo {
        private String role;
        private String content;
        private LocalDateTime timestamp;
    }
    
    @Data
    @Builder
    public static class EpisodicMemoryInfo {
        private String id;
        private String content;
        private String type;
        private Double relevanceScore;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    public static class UserProfileInfo {
        private String summary;
        private String profession;
        private String industry;
        private List<String> interests;
        private List<String> preferences;
        private Integer interactionCount;
    }
}
