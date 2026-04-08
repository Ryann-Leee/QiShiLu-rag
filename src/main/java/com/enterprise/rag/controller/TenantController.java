package com.enterprise.rag.controller;

import com.enterprise.rag.dto.TenantRequest;
import com.enterprise.rag.dto.TenantResponse;
import com.enterprise.rag.entity.Tenant;
import com.enterprise.rag.entity.TenantIsolationLevel;
import com.enterprise.rag.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tenant management controller
 * 
 * Handles tenant creation and configuration
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {
    
    private final TenantService tenantService;
    
    /**
     * Create a new tenant
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        log.info("Creating tenant: {}", request.getName());
        
        TenantIsolationLevel level = TenantIsolationLevel.fromValue(request.getIsolationLevel());
        Tenant tenant = tenantService.createTenant(request.getName(), level);
        
        return ResponseEntity.ok(TenantResponse.builder()
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .isolationLevel(tenant.getIsolationLevel().getValue())
                .collectionName(tenant.getCollectionName())
                .partitionName(tenant.getPartitionName())
                .isActive(tenant.getIsActive())
                .createdAt(tenant.getCreatedAt())
                .build());
    }
    
    /**
     * Get tenant information
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String tenantId) {
        Tenant tenant = tenantService.getTenant(tenantId);
        
        return ResponseEntity.ok(TenantResponse.builder()
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .isolationLevel(tenant.getIsolationLevel().getValue())
                .collectionName(tenant.getCollectionName())
                .partitionName(tenant.getPartitionName())
                .isActive(tenant.getIsActive())
                .createdAt(tenant.getCreatedAt())
                .build());
    }
}
