package com.enterprise.rag.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String conversationId;
    
    private String messageId;
    
    private String response;
    
    private List<RetrievedMemory> retrievedMemories;
    
    private List<RetrievedDocument> retrievedDocuments;
    
    private MemoryContext memoryContext;
    
    private LocalDateTime timestamp;
    
    /**
     * Token usage information
     */
    private TokenUsage tokenUsage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedMemory {
        private String id;
        private String content;
        private String type;
        private Double relevanceScore;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedDocument {
        private String id;
        private String title;
        private String content;
        private String source;
        private Double relevanceScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryContext {
        private String userProfileSummary;
        private List<String> recentTopics;
        private List<String> keyEntities;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
