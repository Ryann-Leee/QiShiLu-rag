package com.enterprise.rag.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for tenant information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    
    private String tenantId;
    
    private String name;
    
    private String isolationLevel;
    
    private String collectionName;
    
    private String partitionName;
    
    private Boolean isActive;
    
    private LocalDateTime createdAt;
}
