package com.enterprise.rag.controller;

import com.enterprise.rag.dto.ChatRequest;
import com.enterprise.rag.dto.ChatResponse;
import com.enterprise.rag.entity.Message;
import com.enterprise.rag.service.RagService;
import com.enterprise.rag.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE (Server-Sent Events) 流式对话控制器
 * 
 * 前端通过 EventSource 连接 /api/chat/stream 接口，
 * 后端实时推送 LLM 生成的内容片段。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatStreamController {
    
    private final RagService ragService;
    private final ObjectMapper objectMapper;
    
    // SSE 超时时间：10 分钟
    private static final long SSE_TIMEOUT = 10 * 60 * 1000L;
    
    // 线程池用于异步生成
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * SSE 流式对话接口
     * 
     * 前端连接方式：
     * ```javascript
     * const eventSource = new EventSource(`/api/chat/stream?message=xxx&conversationId=xxx`);
     * eventSource.onmessage = (event) => {
     *   const data = JSON.parse(event.data);
     *   if (data.type === 'content') {
     *     // 实时显示生成内容
     *     appendText(data.content);
     *   } else if (data.type === 'done') {
     *     // 生成完成
     *   }
     * };
     * ```
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "true") boolean useRag,
            @RequestParam(defaultValue = "true") boolean useMemory) {
        
        String tenantId = TenantContext.getCurrentTenantId();
        
        log.info("SSE stream started: tenantId={}, useRag={}, useMemory={}", tenantId, useRag, useMemory);
        
        // 创建 SSE emitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 异步处理，避免阻塞 HTTP 线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 发送消息存储事件
                sendEvent(emitter, "message_stored", "正在保存消息...");
                
                // 2. 开始流式生成
                StringBuilder fullContent = new StringBuilder();
                
                ragService.chatStream(
                    ChatRequest.builder()
                        .message(message)
                        .conversationId(conversationId)
                        .useRag(useRag)
                        .useMemory(useMemory)
                        .build(),
                    // 流式回调：每次 LLM 返回片段时触发
                    (chunk, isComplete) -> {
                        fullContent.append(chunk);
                        try {
                            if (isComplete) {
                                // 生成完成，发送完成事件
                                sendEvent(emitter, "done", fullContent.toString());
                                emitter.complete();
                                log.info("SSE stream completed: tenantId={}, contentLength={}", 
                                    tenantId, fullContent.length());
                            } else {
                                // 发送内容片段
                                sendEvent(emitter, "content", chunk);
                            }
                        } catch (IOException e) {
                            log.error("SSE send error", e);
                            emitter.completeWithError(e);
                        }
                    }
                );
                
            } catch (Exception e) {
                log.error("SSE stream error: {}", e.getMessage(), e);
                try {
                    sendEvent(emitter, "error", e.getMessage());
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("Failed to send error event", ex);
                }
            }
        }, executor);
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE stream timeout: tenantId={}", tenantId);
        });
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.debug("SSE stream completed: tenantId={}", tenantId);
        });
        
        // 设置错误回调
        emitter.onError(e -> {
            log.error("SSE stream error: tenantId={}, error={}", tenantId, e.getMessage());
        });
        
        return emitter;
    }
    
    /**
     * POST 方式的 SSE 流式对话（支持更长的消息）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(@RequestBody ChatRequest request) {
        return streamChat(
            request.getMessage(),
            request.getConversationId(),
            request.isUseRag(),
            request.isUseMemory()
        );
    }
    
    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, String eventType, String data) throws IOException {
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
            .name(eventType)
            .data(data);
        
        // 设置重连时间（防止前端断开）
        builder.reconnectTime(5000L);
        
        emitter.send(builder);
    }
}
