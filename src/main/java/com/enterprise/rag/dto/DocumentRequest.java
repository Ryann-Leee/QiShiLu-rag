package com.enterprise.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for uploading knowledge documents
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    
    @NotBlank(message = "Document title is required")
    private String title;
    
    @NotBlank(message = "Document content is required")
    private String content;
    
    private String source;
    
    private String docType;
    
    private String metadata;
}
