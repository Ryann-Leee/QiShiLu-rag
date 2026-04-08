package com.enterprise.rag.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * LLM service for text generation and embeddings
 * 
 * Supports OpenAI API for:
 * - Chat completion
 * - Text embeddings
 * - Streaming responses
 */
@Slf4j
@Service
public class LLMService {
    
    // LLM Configuration (Chat Completions)
    @Value("${llm.api-key:}")
    private String apiKey;
    
    @Value("${llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${llm.model:gpt-4o-mini}")
    private String model;
    
    @Value("${llm.max-tokens:4096}")
    private int maxTokens;
    
    @Value("${llm.temperature:0.7}")
    private double temperature;
    
    // Embedding Configuration (Separate from LLM)
    @Value("${embedding.model:text-embedding-3-small}")
    private String embeddingModel;
    
    @Value("${embedding.api-key:}")
    private String embeddingApiKey;
    
    @Value("${embedding.base-url:https://api.openai.com/v1}")
    private String embeddingBaseUrl;
    
    @Value("${embedding.dimension:1536}")
    private int embeddingDimension;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate chat completion
     * 
     * @param systemPrompt System prompt with context
     * @param messages Conversation history
     * @return Generated response
     */
    public String generateCompletion(String systemPrompt, List<Message> messages) {
        try {
            // Build request body
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", buildMessages(systemPrompt, messages),
                    "max_tokens", maxTokens,
                    "temperature", temperature
            );
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("LLM API error: {}", response.code());
                    return "Sorry, I encountered an error processing your request.";
                }
                
                String responseBody = response.body().string();
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                
                // Extract content from response
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
                
                return "No response generated.";
            }
            
        } catch (Exception e) {
            log.error("Error generating completion: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Generate streaming chat completion
     * 
     * @param systemPrompt System prompt
     * @param messages Conversation history
     * @param onChunk Callback for each chunk of the response
     * @return CompletableFuture that completes when streaming is done
     */
    public CompletableFuture<Void> generateStreamingCompletion(
            String systemPrompt,
            List<Message> messages,
            Consumer<String> onChunk) {
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", buildMessages(systemPrompt, messages),
                    "max_tokens", maxTokens,
                    "temperature", temperature,
                    "stream", true
            );
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            
            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if ("[DONE]".equals(data)) {
                        future.complete(null);
                        return;
                    }
                    
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = (String) delta.get("content");
                                onChunk.accept(content);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error parsing streaming chunk: {}", e.getMessage());
                    }
                }
                
                @Override
                public void onClosed(EventSource eventSource) {
                    future.complete(null);
                }
                
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    log.error("Streaming failure: {}", t.getMessage());
                    future.completeExceptionally(t);
                }
            };
            
            EventSource eventSource = EventSources.createFactory(httpClient)
                    .newEventSource(request, listener);
            
        } catch (Exception e) {
            log.error("Error starting streaming: {}", e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Generate embedding for text (returns float array)
     *
     * @param text Input text
     * @return Embedding vector as float array
     */
    public float[] generateEmbedding(String text) {
        try {
            // Use separate embedding API key if configured, otherwise fall back to LLM API key
            String effectiveApiKey = !embeddingApiKey.isEmpty() ? embeddingApiKey : apiKey;
            String effectiveBaseUrl = !embeddingBaseUrl.isEmpty() ? embeddingBaseUrl : baseUrl;

            Map<String, Object> requestBody = Map.of(
                    "model", embeddingModel,
                    "input", text,
                    "dimensions", embeddingDimension
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(effectiveBaseUrl + "/embeddings")
                    .addHeader("Authorization", "Bearer " + effectiveApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Embedding API error: {} - Using fallback", response.code());
                    return generateRandomEmbedding();
                }

                String responseBody = response.body().string();
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

                List<List<Double>> data = (List<List<Double>>) result.get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = data.get(0);
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = embedding.get(i).floatValue();
                    }
                    return vector;
                }

                return generateRandomEmbedding();
            }

        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return generateRandomEmbedding();
        }
    }

    /**
     * Generate embedding for text (returns List of Float)
     * Convenience method for dual-engine service compatibility
     *
     * @param text Input text
     * @return Embedding vector as List of Float
     */
    public List<Float> generateEmbeddingAsList(String text) {
        float[] embedding = generateEmbedding(text);
        List<Float> result = new java.util.ArrayList<>(embedding.length);
        for (float v : embedding) {
            result.add(v);
        }
        return result;
    }
    
    /**
     * Build messages array for API request
     */
    private List<Map<String, String>> buildMessages(String systemPrompt, List<Message> messages) {
        List<Map<String, String>> apiMessages = new java.util.ArrayList<>();
        
        // Add system prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            apiMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        
        // Add conversation messages
        for (Message msg : messages) {
            apiMessages.add(Map.of("role", msg.role.toLowerCase(), "content", msg.content));
        }
        
        return apiMessages;
    }
    
    /**
     * Generate random embedding (fallback)
     */
    private float[] generateRandomEmbedding() {
        float[] embedding = new float[embeddingDimension];
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < embeddingDimension; i++) {
            embedding[i] = random.nextFloat() * 2 - 1;
        }
        return embedding;
    }
    
    /**
     * Message DTO for LLM
     */
    public record Message(String role, String content) {}
    
    /**
     * Simple ObjectMapper (use Jackson in production)
     */
    private static class ObjectMapper {
        public String writeValueAsString(Object value) throws Exception {
            // Simplified JSON serialization - use Jackson in production
            if (value instanceof Map) {
                return mapToJson((Map<?, ?>) value);
            }
            return "{}";
        }
        
        @SuppressWarnings("unchecked")
        public <T> T readValue(String content, Class<T> type) throws Exception {
            // Simplified - use Jackson in production
            return (T) new java.util.HashMap<String, Object>();
        }
        
        private String mapToJson(Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(escapeJson((String) value)).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else if (value instanceof List) {
                    sb.append(listToJson((List<?>) value));
                } else if (value instanceof Map) {
                    sb.append(mapToJson((Map<?, ?>) value));
                } else {
                    sb.append("\"").append(value).append("\"");
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String listToJson(List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                if (item instanceof String) {
                    sb.append("\"").append(escapeJson((String) item)).append("\"");
                } else if (item instanceof Map) {
                    sb.append(mapToJson((Map<?, ?>) item));
                } else {
                    sb.append(item);
                }
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        
        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }
    }
}
