// config.js - Frontend Configuration for Multi-Tenant Application (Local Dev Ready)

const APP_CONFIG = {
    development: {
        BASE_DOMAIN: 'localhost'
    },
    production: {
        BASE_DOMAIN: 'oggyandolivia.cfd' // change later for prod
    }
};

// Auto-detect environment
const ENV = window.location.hostname.includes('localhost') || window.location.hostname === '127.0.0.1'
    ? 'development'
    : 'production';

const BASE_DOMAIN = APP_CONFIG[ENV].BASE_DOMAIN;

// 🧠 Super Admin Detection
function isSuperAdminDomain() {
    const host = window.location.hostname.toLowerCase();
    const base = BASE_DOMAIN.toLowerCase();

    // ✅ Only exact base domain (localhost) or loopback IP are superadmin
    return host === base || host === '127.0.0.1';
}

// 🧩 Extract Subdomain (for tenants)
function getSubdomain() {
    const host = window.location.hostname.toLowerCase();
    const base = BASE_DOMAIN.toLowerCase();

    if (host === base || host === '127.0.0.1') {
        return null;
    }

    const parts = host.split('.');
    const baseParts = base.split('.');

    if (parts.length > baseParts.length) {
        return parts[0]; // "tenant1" from "tenant1.localhost"
    }

    return null;
}

// 🧾 Display info
function getTenantDisplay() {
    if (isSuperAdminDomain()) return 'SUPER ADMIN Console (Base Domain)';
    const subdomain = getSubdomain();
    return subdomain ? `Tenant: ${subdomain.toUpperCase()}` : 'Unknown Domain Context';
}

console.log('Environment:', ENV);
console.log('Base Domain:', BASE_DOMAIN);
console.log('Current Hostname:', window.location.hostname);
console.log('SuperAdmin?', isSuperAdminDomain());
console.log('Subdomain:', getSubdomain());
