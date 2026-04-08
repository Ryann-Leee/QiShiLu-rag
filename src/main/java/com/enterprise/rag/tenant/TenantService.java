package com.enterprise.rag.tenant;

import com.enterprise.rag.entity.Tenant;
import com.enterprise.rag.entity.TenantIsolationLevel;
import com.enterprise.rag.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Tenant service for multi-tenant management
 * 
 * Handles tenant creation, retrieval, and isolation strategy determination
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {
    
    private final TenantRepository tenantRepository;
    
    private static final String BASE_COLLECTION_NAME = "knowledge_base";
    private static final String MEMORY_COLLECTION_NAME = "episodic_memories";
    
    /**
     * Create a new tenant with specified isolation level
     */
    @Transactional
    public Tenant createTenant(String name, TenantIsolationLevel isolationLevel) {
        String tenantId = UUID.randomUUID().toString();
        
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(name)
                .isolationLevel(isolationLevel)
                .collectionName(generateCollectionName(tenantId))
                .partitionName(generatePartitionName(tenantId))
                .isActive(true)
                .build();
        
        return tenantRepository.save(tenant);
    }
    
    /**
     * Get tenant by ID
     */
    @Transactional(readOnly = true)
    public Tenant getTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }
    
    /**
     * Get tenant with isolation information
     */
    @Transactional(readOnly = true)
    public TenantIsolationInfo getIsolationInfo(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        
        return TenantIsolationInfo.builder()
                .tenantId(tenant.getTenantId())
                .isolationLevel(tenant.getIsolationLevel())
                .knowledgeCollectionName(tenant.getEffectiveCollectionName(BASE_COLLECTION_NAME))
                .memoryCollectionName(tenant.getEffectiveCollectionName(MEMORY_COLLECTION_NAME))
                .partitionName(tenant.getEffectivePartitionName())
                .build();
    }
    
    /**
     * Build Milvus search filter based on isolation level
     */
    public String buildSearchFilter(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        
        // For METADATA isolation, we need to add tenant_id filter
        if (tenant.getIsolationLevel() == TenantIsolationLevel.METADATA) {
            return String.format("tenant_id == \"%s\"", tenantId);
        }
        
        // For PARTITION and COLLECTION isolation, no additional filter needed
        // as isolation is handled by partition or collection selection
        return null;
    }
    
    /**
     * Build combined filter for user-scoped search
     */
    public String buildUserSearchFilter(String tenantId, String userId) {
        String tenantFilter = buildSearchFilter(tenantId);
        String userFilter = String.format("user_id == \"%s\"", userId);
        
        if (tenantFilter != null) {
            return String.format("%s and %s", tenantFilter, userFilter);
        }
        return userFilter;
    }
    
    private String generateCollectionName(String tenantId) {
        return "tenant_" + tenantId.replace("-", "_") + "_knowledge";
    }
    
    private String generatePartitionName(String tenantId) {
        return "partition_" + tenantId.replace("-", "_");
    }
    
    /**
     * Tenant isolation information DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class TenantIsolationInfo {
        private String tenantId;
        private TenantIsolationLevel isolationLevel;
        private String knowledgeCollectionName;
        private String memoryCollectionName;
        private String partitionName;
    }
}
