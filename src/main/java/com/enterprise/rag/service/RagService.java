package com.enterprise.rag.service;

import com.enterprise.rag.cache.RedisChatCacheService;
import com.enterprise.rag.dto.ChatRequest;
import com.enterprise.rag.dto.ChatResponse;
import com.enterprise.rag.entity.Conversation;
import com.enterprise.rag.entity.EpisodicMemory;
import com.enterprise.rag.entity.KnowledgeDocument;
import com.enterprise.rag.entity.Message;
import com.enterprise.rag.memory.MemoryContext;
import com.enterprise.rag.memory.MemoryService;
import com.enterprise.rag.milvus.MilvusService;
import com.enterprise.rag.repository.ConversationRepository;
import com.enterprise.rag.repository.KnowledgeDocumentRepository;
import com.enterprise.rag.repository.MessageRepository;
import com.enterprise.rag.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) service
 * 
 * Core workflow:
 * 1. Receive user query
 * 2. Recall memory context (session + episodic + profile)
 * 3. Search knowledge base for relevant documents
 * 4. Construct enhanced prompt with context
 * 5. Generate response via LLM
 * 6. Store new memories and update profile
 * 
 * Redis Cache:
 * - Conversation list: 30min TTL
 * - Message list: 1hr TTL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final MemoryService memoryService;
    private final LLMService llmService;
    private final MilvusService milvusService;
    private final RedisChatCacheService cacheService;
    
    private static final String SYSTEM_PROMPT = """
            You are an intelligent enterprise assistant with access to the user's conversation history and relevant knowledge.
            
            When responding:
            1. Use the provided context to give accurate, relevant answers
            2. Reference previous conversations when appropriate
            3. Be concise but thorough
            4. If you don't know something, say so honestly
            
            Context information:
            %s
            
            Knowledge base results:
            %s
            """;
    
    /**
     * Process a chat message with RAG
     * 
     * @param request Chat request
     * @return Chat response with context
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();
        
        if (tenantId == null || userId == null) {
            throw new IllegalStateException("Tenant or user context not set");
        }
        
        // 1. Get or create conversation
        Conversation conversation = getOrCreateConversation(request.getConversationId(), userId, tenantId);
        
        // 2. Store user message
        Message userMessage = storeMessage(conversation.getId(), userId, tenantId, 
                Message.MessageRole.USER, request.getMessage());
        
        // 3. Recall memory context
        MemoryContext memoryContext = memoryService.recallMemory(conversation.getId(), request.getMessage());
        
        // 4. Search knowledge base
        List<ChatResponse.RetrievedDocument> retrievedDocs = searchKnowledgeBase(request.getMessage(), tenantId);
        
        // 5. Build enhanced prompt
        String enhancedPrompt = buildEnhancedPrompt(memoryContext, retrievedDocs);
        
        // 6. Get conversation history
        List<LLMService.Message> conversationHistory = getConversationHistory(conversation.getId());
        
        // 7. Generate response
        String response = llmService.generateCompletion(enhancedPrompt, conversationHistory);
        
        // 8. Store assistant message
        Message assistantMessage = storeMessage(conversation.getId(), userId, tenantId,
                Message.MessageRole.ASSISTANT, response);
        
        // 9. Extract and store new memories (async in production)
        extractAndStoreMemories(conversation.getId(), request.getMessage(), response);
        
        // 10. Build response
        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMessage.getId())
                .response(response)
                .retrievedMemories(buildMemoryResponse(memoryContext))
                .retrievedDocuments(retrievedDocs)
                .memoryContext(buildMemoryContextResponse(memoryContext))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Get or create conversation (with Redis cache)
     */
    private Conversation getOrCreateConversation(String conversationId, String userId, String tenantId) {
        if (conversationId != null) {
            // Try to find from cache first
            List<Conversation> conversations = cacheService.getConversations(
                tenantId,
                () -> conversationRepository.findByTenantId(tenantId)
            );
            
            Conversation cached = conversations.stream()
                .filter(c -> c.getId().equals(conversationId))
                .findFirst()
                .orElse(null);
            
            if (cached != null) {
                return cached;
            }
            
            // Not in cache, try database
            return conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                    .orElseGet(() -> createConversation(userId, tenantId));
        }
        return createConversation(userId, tenantId);
    }
    
    /**
     * Create new conversation (MySQL + Redis cache)
     */
    private Conversation createConversation(String userId, String tenantId) {
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tenantId(tenantId)
                .title("New Conversation")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Save to MySQL
        Conversation saved = conversationRepository.save(conversation);
        
        // Update cache
        cacheService.updateConversation(tenantId, saved);
        
        return saved;
    }
    
    /**
     * Store a message (MySQL + Redis cache)
     */
    private Message storeMessage(String conversationId, String userId, String tenantId,
                                  Message.MessageRole role, String content) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .content(content)
                .build();
        
        // Save to MySQL (持久化)
        Message savedMessage = messageRepository.save(message);
        
        // Append to Redis cache
        cacheService.appendMessage(conversationId, savedMessage);
        
        return savedMessage;
    }
    
    /**
     * Search knowledge base for relevant documents
     */
    private List<ChatResponse.RetrievedDocument> searchKnowledgeBase(String query, String tenantId) {
        try {
            // Generate query embedding
            float[] queryEmbedding = llmService.generateEmbedding(query);
            
            // Search Milvus
            List<MilvusService.SearchResult> results = milvusService.searchKnowledge(
                    queryEmbedding, tenantId, 5);
            
            // Convert to response DTOs
            return results.stream()
                    .map(r -> ChatResponse.RetrievedDocument.builder()
                            .id(r.getId())
                            .content(r.getFields() != null ? 
                                    (String) r.getFields().getOrDefault("content", "") : "")
                            .source(r.getFields() != null ? 
                                    (String) r.getFields().getOrDefault("source", "") : "")
                            .relevanceScore((double) r.getScore())
                            .build())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error searching knowledge base: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Build enhanced prompt with context
     */
    private String buildEnhancedPrompt(MemoryContext memoryContext, 
                                        List<ChatResponse.RetrievedDocument> documents) {
        // Format memory context
        String memoryStr = memoryContext.getFormattedContext() != null ? 
                memoryContext.getFormattedContext() : "No previous context available.";
        
        // Format knowledge documents
        String knowledgeStr = documents.isEmpty() ? 
                "No relevant documents found." :
                documents.stream()
                        .map(d -> String.format("- %s", d.getContent()))
                        .collect(Collectors.joining("\n"));
        
        return String.format(SYSTEM_PROMPT, memoryStr, knowledgeStr);
    }
    
    /**
     * Get conversation history for LLM (with Redis cache)
     */
    private List<LLMService.Message> getConversationHistory(String conversationId) {
        // Try to get from Redis cache first
        List<Message> messages = cacheService.getMessages(
            conversationId,
            () -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        );
        
        return messages.stream()
                .map(m -> new LLMService.Message(
                        m.getRole() == Message.MessageRole.USER ? "user" : "assistant",
                        m.getContent()))
                .collect(Collectors.toList());
    }
    
    /**
     * Extract and store new memories from conversation
     */
    private void extractAndStoreMemories(String conversationId, String userMessage, String assistantResponse) {
        // In production, use LLM to extract key information
        // For now, store the user message as a context memory
        
        try {
            // Simple memory extraction - check for patterns
            if (userMessage.toLowerCase().contains("prefer") || 
                userMessage.toLowerCase().contains("like") ||
                userMessage.toLowerCase().contains("want")) {
                
                memoryService.storeEpisodicMemory(
                        userMessage,
                        EpisodicMemory.MemoryType.PREFERENCE,
                        conversationId,
                        0.7f
                );
            }
            
            if (userMessage.toLowerCase().contains("decide") || 
                userMessage.toLowerCase().contains("will ") ||
                userMessage.toLowerCase().contains("going to")) {
                
                memoryService.storeEpisodicMemory(
                        userMessage,
                        EpisodicMemory.MemoryType.DECISION,
                        conversationId,
                        0.8f
                );
            }
            
        } catch (Exception e) {
            log.error("Error storing memories: {}", e.getMessage());
        }
    }
    
    /**
     * Build memory response from context
     */
    private List<ChatResponse.RetrievedMemory> buildMemoryResponse(MemoryContext context) {
        if (context.getEpisodicMemories() == null) {
            return Collections.emptyList();
        }
        
        return context.getEpisodicMemories().stream()
                .map(m -> ChatResponse.RetrievedMemory.builder()
                        .id(m.getId())
                        .content(m.getContent())
                        .type(m.getType())
                        .relevanceScore(m.getRelevanceScore())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Build memory context response
     */
    private ChatResponse.MemoryContext buildMemoryContextResponse(MemoryContext context) {
        if (context.getUserProfile() == null) {
            return null;
        }
        
        return ChatResponse.MemoryContext.builder()
                .userProfileSummary(context.getUserProfile().getSummary())
                .build();
    }
    
    // ==================== Streaming Methods ====================
    
    /**
     * Stream chat response (SSE)
     * 
     * @param request Chat request
     * @param onChunk Callback for each content chunk: (chunk, isComplete)
     */
    public void chatStream(ChatRequest request, ChunkCallback onChunk) {
        String userId = TenantContext.getCurrentUserId();
        String tenantId = TenantContext.getCurrentTenantId();
        
        // 1. Get or create conversation
        Conversation conversation = getOrCreateConversation(
            request.getConversationId(), userId, tenantId);
        
        // 2. Store user message
        Message userMessage = storeMessage(
            conversation.getId(), userId, tenantId,
            Message.MessageRole.USER, request.getMessage());
        
        // 3. Get conversation history for context
        List<LLMService.Message> history = getConversationHistory(conversation.getId());
        
        // 4. Recall memory context
        String memoryContext = "";
        if (request.isUseMemory()) {
            MemoryContext memory = memoryService.recall(request.getMessage(), tenantId, userId);
            memoryContext = buildMemoryContext(memory);
        }
        
        // 5. Build system prompt
        String systemPrompt = buildSystemPrompt(
            request.isUseRag() ? buildRagContext(request.getMessage()) : "",
            memoryContext
        );
        
        // 6. Generate streaming response
        StringBuilder fullContent = new StringBuilder();
        
        llmService.generateStreamingCompletion(
            systemPrompt,
            history,
            (chunk) -> {
                fullContent.append(chunk);
                // 回调：发送内容片段
                onChunk.onChunk(chunk, false);
            }
        ).exceptionally(ex -> {
            log.error("Streaming error: {}", ex.getMessage());
            onChunk.onChunk("\n[Error: " + ex.getMessage() + "]", true);
            return null;
        }).join();
        
        // 7. Store assistant message (持久化)
        Message assistantMessage = Message.builder()
            .id(UUID.randomUUID().toString())
            .conversationId(conversation.getId())
            .userId(userId)
            .tenantId(tenantId)
            .role(Message.MessageRole.ASSISTANT)
            .content(fullContent.toString())
            .build();
        messageRepository.save(assistantMessage);
        
        // 8. 更新 Redis 缓存
        cacheService.appendMessage(conversation.getId(), assistantMessage);
        
        // 9. Update episodic memory
        if (request.isUseMemory()) {
            memoryService.remember(
                request.getMessage(),
                fullContent.toString(),
                tenantId,
                userId
            );
        }
        
        // 10. 标记完成
        onChunk.onChunk("", true);
        
        log.info("Stream chat completed: conversationId={}, contentLength={}",
            conversation.getId(), fullContent.length());
    }
    
    /**
     * Chunk callback interface for streaming
     */
    @FunctionalInterface
    public interface ChunkCallback {
        /**
         * Called for each chunk of content
         * @param chunk The content chunk
         * @param isComplete Whether this is the final chunk
         */
        void onChunk(String chunk, boolean isComplete);
    }
}
