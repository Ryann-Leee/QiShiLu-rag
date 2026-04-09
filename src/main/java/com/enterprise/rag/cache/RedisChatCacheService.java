package com.enterprise.rag.cache;

import com.enterprise.rag.entity.Conversation;
import com.enterprise.rag.entity.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 对话缓存服务
 * 
 * 缓存策略：
 * - 对话列表：Hash 结构，TTL 30分钟
 * - 消息列表：List 结构，TTL 1小时
 * - 缓存穿透：MySQL 作为持久化层
 */
@Service
public class RedisChatCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(RedisChatCacheService.class);
    
    // Key 前缀
    private static final String PREFIX_CONVERSATIONS = "tenant:%s:conversations";  // Hash
    private static final String PREFIX_MESSAGES = "conversation:%s:messages";    // List
    private static final String PREFIX_CONV_META = "conversation:%s:meta";       // Hash
    
    // TTL 配置
    private static final Duration CONVERSATIONS_TTL = Duration.ofMinutes(30);
    private static final Duration MESSAGES_TTL = Duration.ofHours(1);
    private static final Duration META_TTL = Duration.ofHours(24);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private final ObjectMapper objectMapper;
    
    public RedisChatCacheService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    // ==================== 对话列表缓存 ====================
    
    /**
     * 获取对话列表（从缓存或数据库）
     */
    public List<Conversation> getConversations(String tenantId, java.util.function.Supplier<List<Conversation>> dbLoader) {
        String key = String.format(PREFIX_CONVERSATIONS, tenantId);
        
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
            
            if (cached != null && !cached.isEmpty()) {
                log.debug("对话列表缓存命中: tenantId={}", tenantId);
                return cached.values().stream()
                    .map(this::toConversation)
                    .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed())
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Redis 读取失败，回退到 MySQL: {}", e.getMessage());
        }
        
        // 缓存未命中，从数据库加载
        log.debug("对话列表缓存未命中: tenantId={}", tenantId);
        List<Conversation> list = dbLoader.get();
        
        // 写入缓存
        cacheConversations(tenantId, list);
        
        return list;
    }
    
    /**
     * 缓存对话列表
     */
    public void cacheConversations(String tenantId, List<Conversation> conversations) {
        String key = String.format(PREFIX_CONVERSATIONS, tenantId);
        
        try {
            Map<String, String> map = new HashMap<>();
            for (Conversation conv : conversations) {
                map.put(conv.getId(), toJson(conv));
            }
            
            if (!map.isEmpty()) {
                redisTemplate.opsForHash().putAll(key, map);
                redisTemplate.expire(key, CONVERSATIONS_TTL);
                log.debug("对话列表已缓存: tenantId={}, count={}", tenantId, map.size());
            }
        } catch (Exception e) {
            log.warn("Redis 写入失败: {}", e.getMessage());
        }
    }
    
    /**
     * 更新单个对话缓存
     */
    public void updateConversation(String tenantId, Conversation conversation) {
        String key = String.format(PREFIX_CONVERSATIONS, tenantId);
        
        try {
            redisTemplate.opsForHash().put(key, conversation.getId(), toJson(conversation));
            redisTemplate.expire(key, CONVERSATIONS_TTL);
        } catch (Exception e) {
            log.warn("Redis 更新失败: {}", e.getMessage());
        }
    }
    
    /**
     * 删除对话缓存
     */
    public void deleteConversation(String tenantId, String conversationId) {
        String listKey = String.format(PREFIX_CONVERSATIONS, tenantId);
        String msgKey = String.format(PREFIX_MESSAGES, conversationId);
        String metaKey = String.format(PREFIX_CONV_META, conversationId);
        
        try {
            redisTemplate.opsForHash().delete(listKey, conversationId);
            redisTemplate.delete(msgKey);
            redisTemplate.delete(metaKey);
            log.debug("对话缓存已删除: conversationId={}", conversationId);
        } catch (Exception e) {
            log.warn("Redis 删除失败: {}", e.getMessage());
        }
    }
    
    /**
     * 使对话列表缓存失效
     */
    public void invalidateConversations(String tenantId) {
        String key = String.format(PREFIX_CONVERSATIONS, tenantId);
        redisTemplate.delete(key);
    }
    
    // ==================== 消息列表缓存 ====================
    
    /**
     * 获取消息列表（从缓存或数据库）
     */
    public List<Message> getMessages(String conversationId, java.util.function.Supplier<List<Message>> dbLoader) {
        String key = String.format(PREFIX_MESSAGES, conversationId);
        
        try {
            List<Object> cached = redisTemplate.opsForList().range(key, 0, -1);
            
            if (cached != null && !cached.isEmpty()) {
                log.debug("消息列表缓存命中: conversationId={}", conversationId);
                return cached.stream()
                    .map(this::toMessage)
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Redis 读取失败，回退到 MySQL: {}", e.getMessage());
        }
        
        // 缓存未命中
        log.debug("消息列表缓存未命中: conversationId={}", conversationId);
        List<Message> messages = dbLoader.get();
        
        // 写入缓存
        cacheMessages(conversationId, messages);
        
        return messages;
    }
    
    /**
     * 缓存消息列表
     */
    public void cacheMessages(String conversationId, List<Message> messages) {
        String key = String.format(PREFIX_MESSAGES, conversationId);
        
        try {
            if (!messages.isEmpty()) {
                List<String> jsonList = messages.stream()
                    .map(this::toJson)
                    .collect(Collectors.toList());
                
                redisTemplate.delete(key);  // 先清空
                redisTemplate.opsForList().rightPushAll(key, jsonList);
                redisTemplate.expire(key, MESSAGES_TTL);
                
                // 同时缓存元数据
                cacheMessageMeta(conversationId, messages.size());
                
                log.debug("消息列表已缓存: conversationId={}, count={}", conversationId, messages.size());
            }
        } catch (Exception e) {
            log.warn("Redis 写入失败: {}", e.getMessage());
        }
    }
    
    /**
     * 追加单条消息到缓存（不需要重新加载全部）
     */
    public void appendMessage(String conversationId, Message message) {
        String key = String.format(PREFIX_MESSAGES, conversationId);
        
        try {
            redisTemplate.opsForList().rightPush(key, toJson(message));
            redisTemplate.expire(key, MESSAGES_TTL);
            
            // 更新元数据中的消息计数
            String metaKey = String.format(PREFIX_CONV_META, conversationId);
            redisTemplate.opsForHash().increment(metaKey, "msgCount", 1);
            redisTemplate.opsForHash().put(metaKey, "lastMessageId", message.getId());
            redisTemplate.expire(metaKey, META_TTL);
            
            log.debug("消息已追加到缓存: conversationId={}", conversationId);
        } catch (Exception e) {
            log.warn("Redis 追加消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 使消息缓存失效
     */
    public void invalidateMessages(String conversationId) {
        String msgKey = String.format(PREFIX_MESSAGES, conversationId);
        String metaKey = String.format(PREFIX_CONV_META, conversationId);
        
        redisTemplate.delete(Arrays.asList(msgKey, metaKey));
        log.debug("消息缓存已失效: conversationId={}", conversationId);
    }
    
    // ==================== 元数据缓存 ====================
    
    private void cacheMessageMeta(String conversationId, int messageCount) {
        String key = String.format(PREFIX_CONV_META, conversationId);
        
        Map<String, Object> meta = new HashMap<>();
        meta.put("msgCount", messageCount);
        meta.put("updatedAt", System.currentTimeMillis());
        
        redisTemplate.opsForHash().putAll(key, meta);
        redisTemplate.expire(key, META_TTL);
    }
    
    public Map<String, Object> getMessageMeta(String conversationId) {
        String key = String.format(PREFIX_CONV_META, conversationId);
        Map<Object, Object> meta = redisTemplate.opsForHash().entries(key);
        
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        meta.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }
    
    // ==================== 工具方法 ====================
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }
    
    private Conversation toConversation(Object json) {
        try {
            if (json instanceof String) {
                return objectMapper.readValue((String) json, Conversation.class);
            }
            return objectMapper.convertValue(json, Conversation.class);
        } catch (Exception e) {
            log.error("反序列化 Conversation 失败", e);
            return null;
        }
    }
    
    private Message toMessage(Object json) {
        try {
            if (json instanceof String) {
                return objectMapper.readValue((String) json, Message.class);
            }
            return objectMapper.convertValue(json, Message.class);
        } catch (Exception e) {
            log.error("反序列化 Message 失败", e);
            return null;
        }
    }
    
    // ==================== 缓存统计 ====================
    
    /**
     * 获取缓存命中率统计
     */
    public Map<String, Object> getCacheStats(String tenantId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            String convKey = String.format(PREFIX_CONVERSATIONS, tenantId);
            Long convSize = redisTemplate.opsForHash().size(convKey);
            stats.put("cachedConversations", convSize != null ? convSize : 0);
            
            // 统计消息缓存数量（需要遍历或使用 SCAN）
            stats.put("redisConnected", true);
        } catch (Exception e) {
            stats.put("redisConnected", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 清理所有缓存（谨慎使用）
     */
    public void clearAllCache(String tenantId) {
        try {
            String pattern = "tenant:" + tenantId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清理租户 {} 的所有缓存，共 {} 条", tenantId, keys.size());
            }
        } catch (Exception e) {
            log.error("清理缓存失败", e);
        }
    }
}
