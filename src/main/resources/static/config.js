// config.js - Frontend Configuration for Multi-Tenant Application
// This file should be included before other scripts in your HTML files

const APP_CONFIG = {
    development: {
        BASE_DOMAIN: 'localhost'
    },
    production: {
        BASE_DOMAIN: 'oggyandolivia.cfd'  // Your production domain
    }
};

// Auto-detect environment based on hostname
const ENV = window.location.hostname.includes('localhost') || window.location.hostname === '127.0.0.1'
    ? 'development'
    : 'production';

// Export the base domain for the current environment
const BASE_DOMAIN = APP_CONFIG[ENV].BASE_DOMAIN;

console.log('Environment:', ENV);
console.log('Base Domain:', BASE_DOMAIN);
console.log('Current Hostname:', window.location.hostname);

// Utility function to get tenant display name
function getTenantDisplay() {
    const hostname = window.location.hostname;

    // 1. Handle loopback IP
    if (hostname === '127.0.0.1') {
        return 'SUPER ADMIN Console (Base Domain)';
    }

    // 2. Check if it's the exact base domain
    if (hostname.toLowerCase() === BASE_DOMAIN.toLowerCase()) {
        return 'SUPER ADMIN Console (Base Domain)';
    }

    // 3. Extract subdomain
    const parts = hostname.split('.');
    const baseParts = BASE_DOMAIN.split('.');

    // If hostname has more parts than base domain, it has a subdomain
    if (parts.length > baseParts.length) {
        // Verify the end matches base domain
        let matchesBase = true;
        for (let i = 0; i < baseParts.length; i++) {
            if (parts[parts.length - baseParts.length + i].toLowerCase() !== baseParts[i].toLowerCase()) {
                matchesBase = false;
                break;
            }
        }

        if (matchesBase) {
            const subdomain = parts[0];
            return `Tenant: ${subdomain.toUpperCase()}`;
        }
    }

    // 4. Fallback
    return 'Unknown Domain Context';
}

// Utility function to check if current domain is Super Admin domain
function isSuperAdminDomain() {
    const hostname = window.location.hostname;

    // Handle loopback IP
    if (hostname === '127.0.0.1') {
        return true;
    }

    // Check if it's the exact base domain
    return hostname.toLowerCase() === BASE_DOMAIN.toLowerCase();
}

// Utility function to extract subdomain
function getSubdomain() {
    const hostname = window.location.hostname;

    // Handle loopback IP or base domain
    if (hostname === '127.0.0.1' || hostname.toLowerCase() === BASE_DOMAIN.toLowerCase()) {
        return null;
    }

    const parts = hostname.split('.');
    const baseParts = BASE_DOMAIN.split('.');

    // If hostname has more parts than base domain, extract subdomain
    if (parts.length > baseParts.length) {
        // Verify the end matches base domain
        let matchesBase = true;
        for (let i = 0; i < baseParts.length; i++) {
            if (parts[parts.length - baseParts.length + i].toLowerCase() !== baseParts[i].toLowerCase()) {
                matchesBase = false;
                break;
            }
        }

        if (matchesBase) {
            return parts[0];
        }
    }

    return null;
}

console.log('App Configuration Loaded:', {
    environment: ENV,
    baseDomain: BASE_DOMAIN,
    currentHost: window.location.hostname,
    isSuperAdmin: isSuperAdminDomain(),
    subdomain: getSubdomain()
});