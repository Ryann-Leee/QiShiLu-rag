package com.enterprise.rag.tenant;

import lombok.Getter;
import lombok.Setter;

/**
 * Tenant context holder for multi-tenancy
 * 
 * Stores the current tenant information in ThreadLocal for request-scoped access.
 * This ensures all operations within a request are scoped to the correct tenant.
 */
public class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
    
    /**
     * Set the current tenant ID for this request
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }
    
    /**
     * Get the current tenant ID
     */
    public static String getTenantId() {
        return CURRENT_TENANT_ID.get();
    }
    
    /**
     * Set the current user ID for this request
     */
    public static void setUserId(String userId) {
        CURRENT_USER_ID.set(userId);
    }
    
    /**
     * Get the current user ID
     */
    public static String getUserId() {
        return CURRENT_USER_ID.get();
    }
    
    /**
     * Clear the tenant context (call at the end of each request)
     */
    public static void clear() {
        CURRENT_TENANT_ID.remove();
        CURRENT_USER_ID.remove();
    }
    
    /**
     * Check if tenant context is set
     */
    public static boolean isSet() {
        return CURRENT_TENANT_ID.get() != null;
    }
}
