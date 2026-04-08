package com.enterprise.rag.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Application Startup Configuration
 * 
 * Loads .env file before Spring context starts and prints configuration summary.
 * 
 * @author Enterprise RAG System
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ApplicationStartupConfig {

    private static final String ENV_FILE_NAME = ".env";
    private final EnvConfig envConfig;

    @PostConstruct
    public void init() {
        loadEnvFile();
        printConfigurationSummary();
    }

    /**
     * Load .env file and set as system properties
     */
    private void loadEnvFile() {
        try {
            File envFile = new File(ENV_FILE_NAME);
            String envPath = envFile.getAbsolutePath();
            
            if (envFile.exists()) {
                log.info("========================================");
                log.info("Loading configuration from: {}", envPath);
                log.info("========================================");
                
                Dotenv dotenv = Dotenv.configure()
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                
                int loadedCount = 0;
                for (DotenvEntry entry : dotenv.entries()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // Only set if not already defined
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
                
                log.info("Loaded {} configuration values from .env file", loadedCount);
                log.info("========================================");
            } else {
                log.warn("========================================");
                log.warn(".env file not found: {}", envPath);
                log.warn("Please copy .env.example to .env and configure your settings.");
                log.warn("========================================");
            }
        } catch (Exception e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
        }
    }

    /**
     * Print configuration summary (masking sensitive values)
     */
    private void printConfigurationSummary() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║           Enterprise RAG System - Configuration Summary         ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        
        // LLM Configuration
        printSection("LLM (Large Language Model)", new String[]{
                "LLM_PROVIDER", "LLM_MODEL", "LLM_BASE_URL", "LLM_MAX_TOKENS"
        });
        
        // Embedding Configuration
        printSection("Embedding Model", new String[]{
                "EMBEDDING_MODEL", "EMBEDDING_DIMENSION"
        });
        
        // Milvus Configuration
        printSection("Milvus Vector DB", new String[]{
                "MILVUS_HOST", "MILVUS_PORT", "MILVUS_DATABASE"
        });
        
        // Database Configuration
        printSection("MySQL Database", new String[]{
                "DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME"
        });
        
        // Redis Configuration
        printSection("Redis Cache", new String[]{
                "REDIS_HOST", "REDIS_PORT", "REDIS_DATABASE"
        });
        
        // Server Configuration
        printSection("Server", new String[]{
                "SERVER_PORT", "ENVIRONMENT"
        });
        
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printSection(String title, String[] keys) {
        log.info("║ ▶ {}", padRight(title + " Configuration", 62) + "║");
        for (String key : keys) {
            String value = getConfigValue(key);
            log.info("║   {}: {}", padRight(key, 20), padRight(value, 38) + "║");
        }
    }

    private String getConfigValue(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null) {
            return "(not set)";
        }
        return maskSensitive(key, value);
    }

    private String maskSensitive(String key, String value) {
        if (value == null) return "(not set)";
        
        String upperKey = key.toUpperCase();
        String[] sensitiveKeys = {"PASSWORD", "SECRET", "KEY", "TOKEN", "API_KEY", "CREDENTIAL"};
        
        for (String sensitiveKey : sensitiveKeys) {
            if (upperKey.contains(sensitiveKey)) {
                if (value.length() > 8) {
                    return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                }
                return "****";
            }
        }
        
        if (value.length() > 40) {
            return value.substring(0, 37) + "...";
        }
        return value;
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }
}
