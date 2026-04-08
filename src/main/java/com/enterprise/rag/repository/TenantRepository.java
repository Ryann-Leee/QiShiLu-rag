package com.enterprise.rag.repository;

import com.enterprise.rag.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    
    Optional<Tenant> findByTenantId(String tenantId);
    
    Optional<Tenant> findByName(String name);
    
    boolean existsByTenantId(String tenantId);
}
