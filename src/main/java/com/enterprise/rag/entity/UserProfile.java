package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * User profile entity for long-term memory
 * 
 * Stores persistent user information including:
 * - Basic attributes (profession, industry, preferences)
 * - Behavioral patterns (activity patterns, common query types)
 * - Relationship graph (mentioned persons, projects, organizations)
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_profile_user_id", columnList = "userId"),
    @Index(name = "idx_profile_tenant_id", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @Column(nullable = false, length = 36)
    private String tenantId;
    
    /**
     * Profile data stored as JSON
     * Contains: profession, industry, preferences, interests, etc.
     */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String profileData = "{}";
    
    /**
     * Milvus vector ID for profile embedding
     */
    @Column(length = 255)
    private String embeddingId;
    
    /**
     * Profile summary for quick retrieval
     */
    @Column(length = 1000)
    private String summary;
    
    /**
     * Number of interactions
     */
    @Builder.Default
    private Integer interactionCount = 0;
    
    /**
     * Last interaction time
     */
    private LocalDateTime lastInteractionAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Parse profile data to map
     */
    public Map<String, Object> getProfileMap() {
        // Simple JSON parsing (in production, use Jackson ObjectMapper)
        Map<String, Object> map = new HashMap<>();
        if (profileData != null && !profileData.isEmpty() && !profileData.equals("{}")) {
            // Basic parsing - in real implementation use Jackson
            try {
                // Remove braces and split by comma
                String content = profileData.substring(1, profileData.length() - 1);
                if (!content.isEmpty()) {
                    for (String pair : content.split(",")) {
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replace("\"", "");
                            String value = keyValue[1].trim().replace("\"", "");
                            map.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                // Return empty map on parse error
            }
        }
        return map;
    }
}
