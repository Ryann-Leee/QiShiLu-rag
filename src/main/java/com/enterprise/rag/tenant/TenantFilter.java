package com.enterprise.rag.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Tenant filter for extracting tenant information from requests
 * 
 * Supports multiple methods for tenant identification:
 * 1. HTTP Header: X-Tenant-ID
 * 2. Query Parameter: tenant_id
 * 3. JWT Token claim (future)
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter implements Filter {
    
    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String USER_HEADER = "X-User-ID";
    public static final String TENANT_PARAM = "tenant_id";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // Extract tenant ID from header or parameter
            String tenantId = httpRequest.getHeader(TENANT_HEADER);
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = httpRequest.getParameter(TENANT_PARAM);
            }
            
            // Extract user ID from header
            String userId = httpRequest.getHeader(USER_HEADER);
            
            // Set tenant context if available
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setTenantId(tenantId);
                log.debug("Set tenant context: tenantId={}", tenantId);
            }
            
            if (userId != null && !userId.isEmpty()) {
                TenantContext.setUserId(userId);
                log.debug("Set user context: userId={}", userId);
            }
            
            chain.doFilter(request, response);
            
        } finally {
            // Always clear the tenant context after request
            TenantContext.clear();
            log.debug("Cleared tenant context");
        }
    }
}
