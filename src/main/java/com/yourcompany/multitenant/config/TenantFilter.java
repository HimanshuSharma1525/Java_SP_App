// TenantFilter.java
package com.yourcompany.multitenant.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class TenantFilter implements Filter {

    private static final String SUPER_ADMIN_SUBDOMAIN = "superadmin";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String serverName = req.getServerName();

        // Extract subdomain from server name
        // Format: {subdomain}.himanshu.localhost
        String subdomain = extractSubdomain(serverName);

        if (subdomain != null) {
            log.debug("Setting tenant context to: {}", subdomain);
            TenantContext.setTenantId(subdomain);
        } else {
            log.warn("Could not extract subdomain from: {}", serverName);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractSubdomain(String serverName) {
        // Expected format: {subdomain}.himanshu.localhost
        if (serverName == null || !serverName.contains(".")) {
            return null;
        }

        String[] parts = serverName.split("\\.");
        if (parts.length >= 3) {
            return parts[0]; // Returns "superadmin", "1", "2", etc.
        }

        return null;
    }
}