package com.enterprise.rag.repository;

import com.enterprise.rag.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    
    Optional<UserProfile> findByUserIdAndTenantId(String userId, String tenantId);
    
    Optional<UserProfile> findByUserId(String userId);
}
