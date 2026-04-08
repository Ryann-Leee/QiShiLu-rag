package com.enterprise.rag.repository;

import com.enterprise.rag.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    List<Conversation> findByUserIdAndTenantIdOrderByCreatedAtDesc(String userId, String tenantId);
    
    Page<Conversation> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    
    Optional<Conversation> findByIdAndTenantId(String id, String tenantId);
    
    List<Conversation> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);
}
