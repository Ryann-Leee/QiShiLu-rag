package com.enterprise.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for creating/updating tenants
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequest {
    
    @NotBlank(message = "Tenant name is required")
    private String name;
    
    private String isolationLevel = "METADATA";
}
