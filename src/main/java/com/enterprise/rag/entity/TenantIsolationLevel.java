package com.enterprise.rag.entity;

/**
 * Tenant isolation level enumeration
 * 
 * Defines how tenant data is isolated in Milvus vector database
 */
public enum TenantIsolationLevel {
    
    /**
     * Collection-level isolation
     * - Each tenant has a dedicated Milvus collection
     * - Strongest isolation, suitable for large enterprise tenants
     * - Higher resource consumption
     */
    COLLECTION("collection"),
    
    /**
     * Partition-level isolation
     * - Each tenant has a dedicated partition in a shared collection
     * - Medium isolation, suitable for medium-sized tenants
     * - Balanced resource usage
     */
    PARTITION("partition"),
    
    /**
     * Metadata-level isolation
     * - Tenants share collection, isolated by metadata filtering
     * - Weakest isolation, suitable for small tenants
     * - Most efficient resource usage
     */
    METADATA("metadata");
    
    private final String value;
    
    TenantIsolationLevel(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static TenantIsolationLevel fromValue(String value) {
        for (TenantIsolationLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown isolation level: " + value);
    }
}
