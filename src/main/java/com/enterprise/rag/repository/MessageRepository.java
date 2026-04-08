package com.enterprise.rag.repository;

import com.enterprise.rag.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    
    List<Message> findByConversationIdAndTenantIdOrderByCreatedAtAsc(String conversationId, String tenantId);
    
    List<Message> findTop20ByConversationIdOrderByCreatedAtDesc(String conversationId);
}
