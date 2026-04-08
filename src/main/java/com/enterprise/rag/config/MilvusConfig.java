package com.enterprise.rag.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus Vector Database Configuration
 *
 * Supports Milvus 2.5+ with dual-engine hybrid search and RRF reranking.
 *
 * @author Enterprise RAG System
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.database:default}")
    private String database;

    @Value("${milvus.username:}")
    private String username;

    @Value("${milvus.password:}")
    private String password;

    @Value("${milvus.timeout:5000}")
    private int timeout;

    @Value("${milvus.embedding-dimension:1536}")
    private int embeddingDimension;

    @Value("${milvus.collection-name:knowledge_base}")
    private String collectionName;

    private MilvusClientV2 milvusClientV2;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("Initializing Milvus V2 Client (v2.5+)");
        log.info("========================================");
        log.info("Host: {}", host);
        log.info("Port: {}", port);
        log.info("Database: {}", database);
        log.info("Embedding Dimension: {}", embeddingDimension);
        log.info("Collection Name: {}", collectionName);
        log.info("========================================");
    }

    /**
     * Create Milvus V2 Client
     *
     * @return MilvusClientV2 instance
     */
    @Bean
    public MilvusClientV2 milvusClientV2() {
        try {
            ConnectConfig.ConnectConfigBuilder configBuilder = ConnectConfig.builder()
                    .uri("http://" + host + ":" + port)
                    .connectTimeoutMs(timeout);

            // Add authentication if provided
            if (username != null && !username.isEmpty()) {
                configBuilder.username(username);
                if (password != null && !password.isEmpty()) {
                    configBuilder.password(password);
                }
            }

            milvusClientV2 = new MilvusClientV2(configBuilder.build());

            // Switch to target database
            if (database != null && !database.isEmpty() && !"default".equals(database)) {
                milvusClientV2.useDatabase(database);
            }

            log.info("Milvus V2 Client initialized successfully");
            log.info("Collection will be auto-created on first document insertion");

            return milvusClientV2;
        } catch (Exception e) {
            log.error("Failed to initialize Milvus V2 Client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Milvus V2 Client", e);
        }
    }

    /**
     * Get collection name from configuration
     *
     * @return collection name
     */
    @Bean
    public String milvusCollectionName() {
        return collectionName;
    }

    /**
     * Get embedding dimension from configuration
     *
     * @return embedding dimension
     */
    @Bean
    public Integer milvusEmbeddingDimension() {
        return embeddingDimension;
    }

    @PreDestroy
    public void cleanup() {
        if (milvusClientV2 != null) {
            try {
                milvusClientV2.close();
                log.info("Milvus V2 Client closed successfully");
            } catch (Exception e) {
                log.warn("Error closing Milvus V2 Client: {}", e.getMessage());
            }
        }
    }
}
