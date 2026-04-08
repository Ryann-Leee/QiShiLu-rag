package com.enterprise.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 本地 Embedding 服务客户端
 * 
 * 支持 Qwen3-Embedding-0.6B 等本地模型服务
 * 
 * 使用方式:
 * 1. 启动 embedding_service.py: python embedding_service.py --port 5001
 * 2. 配置 EMBEDDING_PROVIDER=local 和 EMBEDDING_LOCAL_URL
 */
@Slf4j
@Service
public class LocalEmbeddingService {

    @Value("${embedding.local-url:http://localhost:5001}")
    private String serviceUrl;

    @Value("${embedding.dimension:1024}")
    private int dimension;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LocalEmbeddingService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取单个文本的 embedding 向量
     *
     * @param text 输入文本
     * @return float 数组形式的向量
     */
    public float[] getEmbedding(String text) {
        List<float[]> embeddings = getEmbeddings(Collections.singletonList(text));
        return embeddings.isEmpty() ? null : embeddings.get(0);
    }

    /**
     * 获取多个文本的 embedding 向量
     *
     * @param texts 输入文本列表
     * @return float 数组列表
     */
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("texts", texts);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding 服务请求失败: status={}, body={}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Embedding 服务请求失败: " + response.statusCode());
            }

            // 解析响应
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            List<List<Double>> embeddingsData = (List<List<Double>>) responseBody.get("embeddings");

            // 转换为 float[]
            List<float[]> result = new ArrayList<>();
            for (List<Double> embedding : embeddingsData) {
                float[] floatArray = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    floatArray[i] = embedding.get(i).floatValue();
                }
                result.add(floatArray);
            }

            log.debug("成功获取 {} 个文本的 embedding", texts.size());
            return result;

        } catch (Exception e) {
            log.error("获取 embedding 失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取 embedding 失败", e);
        }
    }

    /**
     * 计算两个文本的相似度
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 余弦相似度
     */
    public double getSimilarity(String text1, String text2) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text1", text1);
            requestBody.put("text2", text2);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/similarity"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("相似度计算失败: " + response.statusCode());
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            return ((Number) responseBody.get("similarity")).doubleValue();

        } catch (Exception e) {
            log.error("计算相似度失败: {}", e.getMessage(), e);
            throw new RuntimeException("计算相似度失败", e);
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/health"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Embedding 服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务信息
     */
    public Map<String, Object> getServiceInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            }
            return null;
        } catch (Exception e) {
            log.warn("获取服务信息失败: {}", e.getMessage());
            return null;
        }
    }

    public int getDimension() {
        return dimension;
    }

    // 内部类用于 JSON 解析
    private static class ObjectMapper {
        private final com.fasterxml.jackson.databind.ObjectMapper delegate = new com.fasterxml.jackson.databind.ObjectMapper();

        public String writeValueAsString(Object value) throws Exception {
            return delegate.writeValueAsString(value);
        }

        public <T> T readValue(String json, Class<T> clazz) throws Exception {
            return delegate.readValue(json, clazz);
        }
    }
}
