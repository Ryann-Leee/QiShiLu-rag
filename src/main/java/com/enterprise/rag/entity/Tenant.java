package com.enterprise.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Tenant configuration entity
 * 
 * Supports three isolation levels:
 * 1. COLLECTION - Each tenant has dedicated Milvus collection (strongest isolation)
 * 2. PARTITION - Each tenant has dedicated partition in shared collection
 * 3. METADATA - Tenants share collection, isolated by metadata filtering
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_tenant_name", columnList = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 36, unique = true)
    private String tenantId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantIsolationLevel isolationLevel;
    
    /**
     * Milvus collection name for COLLECTION isolation level
     */
    @Column(length = 255)
    private String collectionName;
    
    /**
     * Milvus partition name for PARTITION isolation level
     */
    @Column(length = 255)
    private String partitionName;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
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
     * Get the actual collection name to use based on isolation level
     */
    public String getEffectiveCollectionName(String baseCollectionName) {
        return switch (isolationLevel) {
            case COLLECTION -> collectionName;
            case PARTITION, METADATA -> baseCollectionName;
        };
    }
    
    /**
     * Get the partition name for PARTITION isolation level
     */
    public String getEffectivePartitionName() {
        return isolationLevel == TenantIsolationLevel.PARTITION ? partitionName : null;
    }
}
