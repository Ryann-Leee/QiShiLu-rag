package com.enterprise.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enterprise RAG System Application
 * 
 * Features:
 * - Multi-tenant architecture with data isolation
 * - Long-term memory with episodic memory and user profiles
 * - RAG (Retrieval-Augmented Generation) with Milvus vector database
 * - High-frequency natural language conversation support
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class RagSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RagSystemApplication.class, args);
    }
}
