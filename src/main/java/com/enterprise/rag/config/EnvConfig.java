package com.enterprise.rag.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Environment Configuration Loader
 * 
 * Loads configuration from .env file and sets them as system properties.
 * This allows application.yml to reference these values via ${ENV_VAR} syntax.
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@Component
public class EnvConfig {

    private static final String ENV_FILE_NAME = ".env";
    private Dotenv dotenv;

    @PostConstruct
    public void init() {
        loadEnvironmentVariables();
    }

    /**
     * Load environment variables from .env file
     */
    private void loadEnvironmentVariables() {
        try {
            // Try to load .env from project root
            File envFile = new File(ENV_FILE_NAME);
            
            if (envFile.exists()) {
                log.info("Loading environment variables from: {}", envFile.getAbsolutePath());
                dotenv = Dotenv.configure()
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                
                // Set system properties from .env file
                if (dotenv != null) {
                    for (DotenvEntry entry : dotenv.entries()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        
                        // Only set if not already defined as system property
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                            log.debug("Set environment variable: {}={}", key, maskSensitive(key, value));
                        }
                    }
                    log.info("Successfully loaded {} environment variables from .env", dotenv.entries().size());
                }
            } else {
                log.warn(".env file not found at: {}. Using default configuration.", 
                        envFile.getAbsolutePath());
                log.info("Copy .env.example to .env and configure your settings.");
            }
        } catch (Exception e) {
            log.warn("Failed to load .env file: {}. Using default configuration.", e.getMessage());
        }
    }

    /**
     * Get environment variable value
     * 
     * Priority: System Property > Environment Variable > .env File > Default Value
     */
    public String get(String key, String defaultValue) {
        // Check system property first
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Check environment variable
        value = System.getenv(key);
        if (value != null) {
            return value;
        }
        
        // Check .env file
        if (dotenv != null) {
            value = dotenv.get(key);
            if (value != null) {
                return value;
            }
        }
        
        return defaultValue;
    }

    /**
     * Get environment variable as Integer
     */
    public Integer getInt(String key, Integer defaultValue) {
        String value = get(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Get environment variable as Boolean
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = get(key, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Mask sensitive values for logging
     */
    private String maskSensitive(String key, String value) {
        String[] sensitiveKeys = {"PASSWORD", "SECRET", "KEY", "TOKEN", "API_KEY"};
        for (String sensitiveKey : sensitiveKeys) {
            if (key.toUpperCase().contains(sensitiveKey)) {
                if (value != null && value.length() > 4) {
                    return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                }
                return "****";
            }
        }
        return value;
    }

    /**
     * Get all defined environment variables (for debugging)
     */
    public void logEnvironmentSummary() {
        log.info("=== Environment Configuration Summary ===");
        
        String[] categories = {
            "LLM_", "EMBEDDING_", "MILVUS_", "DB_", "REDIS_", 
            "TENANT_", "MEMORY_", "SPLITTER_", "SERVER_"
        };
        
        for (String prefix : categories) {
            log.info("--- {} ---", prefix);
            System.getProperties().stringPropertyNames().stream()
                    .filter(key -> key.startsWith(prefix))
                    .forEach(key -> {
                        String value = System.getProperty(key);
                        log.info("  {} = {}", key, maskSensitive(key, value));
                    });
        }
        
        log.info("========================================");
    }
}
