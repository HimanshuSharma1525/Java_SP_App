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

    // SUPER ADMIN tenant ID is always "1"
    public static final String SUPER_ADMIN_ID = "1";

    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        // 🟢 REAL DOMAIN FIX FOR RENDER
        String serverName = req.getHeader("X-Forwarded-Host");
        if (serverName == null || serverName.isEmpty()) {
            serverName = req.getServerName();
        }

        log.debug("TenantFilter detected server name: {}", serverName);

        // Resolve tenant ID
        String tenantId = resolveTenantId(serverName);

        if (tenantId != null) {
            log.debug("Setting TenantContext to {}", tenantId);
            TenantContext.setTenantId(tenantId);
        } else {
            log.warn("Could not determine tenant from server name: {}", serverName);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    // 🟢 Tenant resolver
    private String resolveTenantId(String serverName) {
        if (serverName == null) {
            return null;
        }

        // Super Admin handling (base domain)
        if (isBaseDomain(serverName)) {
            log.debug("Base domain detected → SUPER ADMIN tenant (1)");
            return SUPER_ADMIN_ID;
        }

        // Localhost fallback
        if (serverName.equals("127.0.0.1") || serverName.equalsIgnoreCase("localhost")) {
            log.debug("Localhost → SUPER ADMIN tenant (1)");
            return SUPER_ADMIN_ID;
        }

        // Tenant subdomain
        String subdomain = extractSubdomain(serverName);
        if (subdomain != null && !subdomain.isEmpty()) {
            log.debug("Subdomain detected → tenant {}", subdomain);
            return subdomain;
        }

        return null;
    }

    private boolean isBaseDomain(String serverName) {
        return serverName.equalsIgnoreCase(baseDomain);
    }

    private String extractSubdomain(String serverName) {
        if (serverName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            return null; // IP address
        }

        String[] parts = serverName.split("\\.");
        String[] baseParts = baseDomain.split("\\.");

        if (parts.length <= baseParts.length) {
            return null;
        }

        // Ensure end matches the base domain
        for (int i = 0; i < baseParts.length; i++) {
            if (!parts[parts.length - baseParts.length + i].equalsIgnoreCase(baseParts[i])) {
                return null;
            }
        }

        return parts[0]; // return subdomain
    }
}
