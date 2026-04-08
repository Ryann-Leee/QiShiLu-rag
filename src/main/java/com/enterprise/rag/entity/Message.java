package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Message entity
 * 
 * Represents a single message in a conversation
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_conv_id", columnList = "conversationId"),
    @Index(name = "idx_msg_user_id", columnList = "userId"),
    @Index(name = "idx_msg_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_msg_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36)
    private String conversationId;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @Column(nullable = false, length = 36)
    private String tenantId;
    
    /**
     * Message role: USER, ASSISTANT, SYSTEM
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * Additional message metadata (tokens, model, etc.)
     */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String metadata = "{}";
    
    /**
     * Token count for the message
     */
    private Integer tokenCount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversationId", insertable = false, updatable = false)
    private Conversation conversation;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * Message role enumeration
     */
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
