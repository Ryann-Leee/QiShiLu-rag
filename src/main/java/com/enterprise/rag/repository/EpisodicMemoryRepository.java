package com.enterprise.rag.repository;

import com.enterprise.rag.entity.EpisodicMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EpisodicMemoryRepository extends JpaRepository<EpisodicMemory, String> {
    
    List<EpisodicMemory> findByUserIdAndTenantIdOrderByImportanceScoreDesc(String userId, String tenantId);
    
    List<EpisodicMemory> findByUserIdAndTenantIdAndMemoryType(String userId, String tenantId, EpisodicMemory.MemoryType memoryType);
    
    List<EpisodicMemory> findTop10ByUserIdAndTenantIdOrderByCreatedAtDesc(String userId, String tenantId);
}
