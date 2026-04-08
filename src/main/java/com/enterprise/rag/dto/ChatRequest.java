package com.enterprise.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotBlank(message = "Message content is required")
    private String message;
    
    private String conversationId;
    
    private Boolean stream = false;
    
    /**
     * Additional context for the query
     */
    private String context;
}
