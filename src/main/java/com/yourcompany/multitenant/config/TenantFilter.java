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

    public static final String SUPER_ADMIN_ID = "SUPERADMIN";

    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        // Render sends real domain here
        String serverName = req.getHeader("X-Forwarded-Host");
        if (serverName == null || serverName.isEmpty()) {
            serverName = req.getServerName();
        }

        log.debug("Resolved server name (after proxy fix): {}", serverName);

        String tenantId = resolveTenantId(serverName);

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
            log.debug("Tenant set to {}", tenantId);
        } else {
            log.warn("Could not determine tenant ID from server name: {}", serverName);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantId(String serverName) {
        if (serverName == null) {
            return null;
        }

        // Super Admin Domain
        if (isBaseDomain(serverName) || serverName.equals("127.0.0.1")) {
            log.debug("Base domain detected → SUPERADMIN");
            return SUPER_ADMIN_ID;
        }

        // Tenant domain
        String subdomain = extractSubdomain(serverName);
        if (subdomain != null && !subdomain.isEmpty()) {
            log.debug("Tenant subdomain detected → {}", subdomain);
            return subdomain;
        }

        return null;
    }

    private boolean isBaseDomain(String serverName) {
        return serverName.equalsIgnoreCase(baseDomain);
    }

    private String extractSubdomain(String serverName) {
        String[] parts = serverName.split("\\.");
        String[] baseParts = baseDomain.split("\\.");

        if (parts.length <= baseParts.length) return null;

        for (int i = 0; i < baseParts.length; i++) {
            if (!parts[parts.length - baseParts.length + i].equalsIgnoreCase(baseParts[i])) {
                return null;
            }
        }

        return parts[0];
    }
}
