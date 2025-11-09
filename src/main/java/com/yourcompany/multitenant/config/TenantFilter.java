package com.yourcompany.multitenant.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class TenantFilter implements Filter {

    // 🎯 Define the Super Admin Tenant ID constant. Made public for use in TenantService.
    public static final String SUPER_ADMIN_ID = "SUPERADMIN";

    // 🔧 Configurable base domain (e.g., "localhost" or "yourdomain.com")
    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String serverName = req.getServerName();

        // Resolve the Tenant ID based on the server name
        String tenantId = resolveTenantId(serverName);

        if (tenantId != null) {
            log.debug("Setting tenant context to: {}", tenantId);
            TenantContext.setTenantId(tenantId);
        } else {
            // Log a warning if no tenant could be determined
            log.warn("Could not determine tenant ID from server name: {}", serverName);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Crucial: Clear the tenant context after the request is processed
            TenantContext.clear();
        }
    }

    /**
     * Resolves the Tenant ID based on the server name.
     * - Base domain (no subdomain) maps to SUPERADMIN
     * - Subdomain maps to tenant subdomain
     */
    private String resolveTenantId(String serverName) {
        if (serverName == null) {
            return null;
        }

        // Handle loopback IP (127.0.0.1) as base domain
        if (serverName.equals("127.0.0.1")) {
            log.debug("Server name is loopback IP, mapping to Super Admin.");
            return SUPER_ADMIN_ID;
        }

        // Check if this is the base domain (no subdomain)
        if (isBaseDomain(serverName)) {
            log.debug("Server name is base domain '{}', mapping to Super Admin.", serverName);
            return SUPER_ADMIN_ID;
        }

        // Extract subdomain for tenant login
        String subdomain = extractSubdomain(serverName);
        if (subdomain != null && !subdomain.isEmpty()) {
            log.debug("Detected tenant subdomain: {}", subdomain);
            return subdomain;
        }

        // Fallback
        log.warn("Could not extract tenant from server name: {}", serverName);
        return null;
    }

    /**
     * Checks if the server name is the base domain (without subdomain).
     * Examples:
     *   - "localhost" -> true
     *   - "yourdomain.com" -> true
     *   - "tenant1.localhost" -> false
     *   - "tenant1.yourdomain.com" -> false
     */
    private boolean isBaseDomain(String serverName) {
        // Exact match with configured base domain
        if (serverName.equalsIgnoreCase(baseDomain)) {
            return true;
        }

        // For multi-level domains (e.g., "yourdomain.com")
        // Check if serverName matches exactly without any subdomain prefix
        String[] parts = serverName.split("\\.");

        // If base domain has dots (e.g., "yourdomain.com")
        if (baseDomain.contains(".")) {
            return serverName.equalsIgnoreCase(baseDomain);
        }

        // If base domain is single word (e.g., "localhost")
        // serverName must be exactly that word
        return parts.length == 1 && serverName.equalsIgnoreCase(baseDomain);
    }

    /**
     * Extracts the subdomain from the server name.
     * Examples:
     *   - "tenant1.localhost" -> "tenant1"
     *   - "tenant1.yourdomain.com" -> "tenant1"
     *   - "localhost" -> null
     *   - "yourdomain.com" -> null
     */
    private String extractSubdomain(String serverName) {
        // Handle IP addresses (no subdomain)
        if (serverName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            return null;
        }

        String[] parts = serverName.split("\\.");
        String[] baseParts = baseDomain.split("\\.");

        // Need more parts than base domain to have a subdomain
        if (parts.length <= baseParts.length) {
            return null;
        }

        // Verify that the end of serverName matches baseDomain
        boolean matchesBaseDomain = true;
        for (int i = 0; i < baseParts.length; i++) {
            if (!parts[parts.length - baseParts.length + i].equalsIgnoreCase(baseParts[i])) {
                matchesBaseDomain = false;
                break;
            }
        }

        if (!matchesBaseDomain) {
            return null;
        }

        // Return the first part as the subdomain
        // For "tenant1.localhost" or "tenant1.yourdomain.com", returns "tenant1"
        return parts[0];
    }
}