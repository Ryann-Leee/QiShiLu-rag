package com.enterprise.rag.memory;

import com.enterprise.rag.entity.EpisodicMemory;
import com.enterprise.rag.entity.Message;
import com.enterprise.rag.entity.UserProfile;
import com.enterprise.rag.milvus.MilvusService;
import com.enterprise.rag.repository.EpisodicMemoryRepository;
import com.enterprise.rag.repository.MessageRepository;
import com.enterprise.rag.repository.UserProfileRepository;
import com.enterprise.rag.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory service for long-term memory management
 * 
 * Implements a three-layer memory architecture:
 * 1. Session Memory - Recent messages in current conversation
 * 2. Episodic Memory - Important past experiences stored in Milvus
 * 3. User Profile - Persistent user information
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {
    
    private final MessageRepository messageRepository;
    private final EpisodicMemoryRepository episodicMemoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final MilvusService milvusService;
    
    @Value("${memory.session.max-messages:20}")
    private int maxSessionMessages;
    
    @Value("${memory.episodic.top-k:5}")
    private int episodicTopK;
    
    /**
     * Recall memory context for a conversation
     * 
     * @param conversationId Current conversation ID
     * @param query User's query for semantic search
     * @return Memory context containing all relevant memories
     */
    public MemoryContext recallMemory(String conversationId, String query) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();
        
        if (tenantId == null || userId == null) {
            log.warn("No tenant or user context set, returning empty memory context");
            return MemoryContext.builder().build();
        }
        
        // 1. Get session memory (recent messages)
        List<Message> recentMessages = getRecentMessages(conversationId);
        
        // 2. Search episodic memories using vector similarity
        List<EpisodicMemory> relevantMemories = searchEpisodicMemories(query, userId, tenantId);
        
        // 3. Get user profile
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserIdAndTenantId(userId, tenantId);
        
        // 4. Build memory context
        return buildMemoryContext(recentMessages, relevantMemories, profileOpt.orElse(null));
    }
    
    /**
     * Store new episodic memory
     * 
     * @param content Memory content
     * @param memoryType Type of memory
     * @param conversationId Source conversation ID
     * @param importanceScore Importance score (0.0 - 1.0)
     * @return Created episodic memory
     */
    @Transactional
    public EpisodicMemory storeEpisodicMemory(
            String content,
            EpisodicMemory.MemoryType memoryType,
            String conversationId,
            float importanceScore) {
        
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();
        
        if (tenantId == null || userId == null) {
            throw new IllegalStateException("No tenant or user context set");
        }
        
        // Create memory entity
        EpisodicMemory memory = EpisodicMemory.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tenantId(tenantId)
                .conversationId(conversationId)
                .content(content)
                .memoryType(memoryType)
                .importanceScore(importanceScore)
                .accessCount(0)
                .build();
        
        // Generate embedding and store in Milvus
        // Note: In production, use actual embedding API (e.g., OpenAI text-embedding-3-small)
        float[] embedding = generatePlaceholderEmbedding(content);
        String embeddingId = milvusService.insertMemory(content, embedding, userId, tenantId, memory.getId());
        memory.setEmbeddingId(embeddingId);
        
        // Save to database
        return episodicMemoryRepository.save(memory);
    }
    
    /**
     * Extract and store important information from conversation
     * 
     * @param conversationId Conversation ID
     * @param messages Recent messages to analyze
     */
    @Transactional
    public void extractAndStoreMemories(String conversationId, List<Message> messages) {
        // In a real implementation, this would use an LLM to extract key information
        // For now, we'll implement a simple extraction based on message patterns
        
        String combinedContent = messages.stream()
                .filter(m -> m.getRole() == Message.MessageRole.USER)
                .map(Message::getContent)
                .collect(Collectors.joining(" "));
        
        // Simple keyword-based memory extraction (placeholder for LLM-based extraction)
        // In production, use LLM to extract: preferences, decisions, entities, facts
        
        // Update user profile based on conversation
        updateUserProfile(messages);
        
        log.info("Extracted memories for conversation: {}", conversationId);
    }
    
    /**
     * Update user profile based on conversation
     */
    @Transactional
    public void updateUserProfile(List<Message> messages) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();
        
        if (tenantId == null || userId == null) {
            return;
        }
        
        UserProfile profile = userProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> UserProfile.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .tenantId(tenantId)
                        .profileData("{}")
                        .interactionCount(0)
                        .build());
        
        // Increment interaction count
        profile.setInteractionCount(profile.getInteractionCount() + 1);
        profile.setLastInteractionAt(java.time.LocalDateTime.now());
        
        // In production, use LLM to update profile data
        // For now, just save the updated interaction count
        userProfileRepository.save(profile);
    }
    
    /**
     * Get recent messages for session memory
     */
    private List<Message> getRecentMessages(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }
        return messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
    }
    
    /**
     * Search episodic memories using vector similarity
     */
    private List<EpisodicMemory> searchEpisodicMemories(String query, String userId, String tenantId) {
        try {
            // Search in Milvus
            List<String> memoryIds = milvusService.searchMemories(query, userId, tenantId, episodicTopK);
            
            if (memoryIds.isEmpty()) {
                // Fallback to database query
                return episodicMemoryRepository.findTop10ByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId);
            }
            
            // Retrieve full memory objects
            return episodicMemoryRepository.findAllById(memoryIds);
            
        } catch (Exception e) {
            log.error("Error searching episodic memories: {}", e.getMessage());
            // Fallback to database query
            return episodicMemoryRepository.findTop10ByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId);
        }
    }
    
    /**
     * Build memory context from components
     */
    private MemoryContext buildMemoryContext(
            List<Message> recentMessages,
            List<EpisodicMemory> episodicMemories,
            UserProfile profile) {
        
        // Convert messages to info objects
        List<MemoryContext.MessageInfo> messageInfos = recentMessages.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(m -> MemoryContext.MessageInfo.builder()
                        .role(m.getRole().name())
                        .content(m.getContent())
                        .timestamp(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        // Convert episodic memories to info objects
        List<MemoryContext.EpisodicMemoryInfo> memoryInfos = episodicMemories.stream()
                .map(m -> MemoryContext.EpisodicMemoryInfo.builder()
                        .id(m.getId())
                        .content(m.getContent())
                        .type(m.getMemoryType() != null ? m.getMemoryType().name() : "UNKNOWN")
                        .relevanceScore((double) m.getImportanceScore())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        // Build user profile info
        MemoryContext.UserProfileInfo profileInfo = null;
        if (profile != null) {
            profileInfo = MemoryContext.UserProfileInfo.builder()
                    .summary(profile.getSummary())
                    .interactionCount(profile.getInteractionCount())
                    .build();
        }
        
        // Build formatted context for LLM
        String formattedContext = formatContextForLLM(messageInfos, memoryInfos, profileInfo);
        
        return MemoryContext.builder()
                .sessionMemory(messageInfos)
                .episodicMemories(memoryInfos)
                .userProfile(profileInfo)
                .formattedContext(formattedContext)
                .build();
    }
    
    /**
     * Format memory context for LLM prompt
     */
    private String formatContextForLLM(
            List<MemoryContext.MessageInfo> messages,
            List<MemoryContext.EpisodicMemoryInfo> memories,
            MemoryContext.UserProfileInfo profile) {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== Memory Context ===\n\n");
        
        if (profile != null && profile.getSummary() != null) {
            sb.append("User Profile:\n").append(profile.getSummary()).append("\n\n");
        }
        
        if (!memories.isEmpty()) {
            sb.append("Relevant Past Memories:\n");
            for (MemoryContext.EpisodicMemoryInfo memory : memories) {
                sb.append("- [").append(memory.getType()).append("] ").append(memory.getContent()).append("\n");
            }
            sb.append("\n");
        }
        
        if (!messages.isEmpty()) {
            sb.append("Recent Conversation:\n");
            for (MemoryContext.MessageInfo msg : messages) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    @Value("${llm.embedding-dimension:1536}")
    private int embeddingDimension;
    
    /**
     * Generate placeholder embedding (for development)
     * In production, use actual embedding API (e.g., OpenAI text-embedding-3-small)
     */
    private float[] generatePlaceholderEmbedding(String text) {
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
}
