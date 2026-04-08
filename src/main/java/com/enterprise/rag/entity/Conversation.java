package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation entity
 * 
 * Represents a conversation session between user and AI
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conv_user_id", columnList = "userId"),
    @Index(name = "idx_conv_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_conv_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @Column(nullable = false, length = 36)
    private String tenantId;
    
    @Column(length = 255)
    private String title;
    
    /**
     * Conversation metadata (tags, category, etc.)
     */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String metadata = "{}";
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime endedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
