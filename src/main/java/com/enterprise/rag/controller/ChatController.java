package com.enterprise.rag.controller;

import com.enterprise.rag.dto.ChatRequest;
import com.enterprise.rag.dto.ChatResponse;
import com.enterprise.rag.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chat controller for RAG conversations
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final RagService ragService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Send a chat message and get a response
     * 
     * Headers:
     * - X-Tenant-ID: Tenant identifier (required)
     * - X-User-ID: User identifier (required)
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request for conversation: {}", request.getConversationId());
        
        try {
            ChatResponse response = ragService.chat(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Send a chat message with streaming response
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request");
        
        SseEmitter emitter = new SseEmitter(60000L); // 60 second timeout
        
        executor.execute(() -> {
            try {
                ChatResponse response = ragService.chat(request);
                
                // Split response into chunks for streaming effect
                String fullResponse = response.getResponse();
                int chunkSize = 10;
                
                for (int i = 0; i < fullResponse.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, fullResponse.length());
                    String chunk = fullResponse.substring(i, end);
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(chunk));
                    Thread.sleep(50); // Simulate typing effect
                }
                
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(""));
                emitter.complete();
                
            } catch (Exception e) {
                log.error("Error in streaming: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
