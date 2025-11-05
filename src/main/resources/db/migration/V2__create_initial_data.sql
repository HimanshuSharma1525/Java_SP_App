-- V2__create_initial_data.sql
-- Insert super admin tenant
INSERT INTO tenants (subdomain, name, active)
VALUES ('superadmin', 'Super Admin Organization', true);

-- Insert super admin user (password: Admin@123)
-- BCrypt hash for 'Admin@123'
INSERT INTO users (email, password, first_name, last_name, role, tenant_id, active)
SELECT
    'superadmin@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Super',
    'Admin',
    'SUPER_ADMIN',
    id,
    true
FROM tenants WHERE subdomain = 'superadmin';

-- Insert sample customer admin tenants
INSERT INTO tenants (subdomain, name, active)
VALUES
    ('1', 'Customer Organization 1', true),
    ('2', 'Customer Organization 2', true);

-- Insert sample customer admin users (password: Admin@123)
INSERT INTO users (email, password, first_name, last_name, role, tenant_id, active)
SELECT
    'admin1@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Customer',
    'Admin One',
    'CUSTOMER_ADMIN',
    id,
    true
FROM tenants WHERE subdomain = '1';

INSERT INTO users (email, password, first_name, last_name, role, tenant_id, active)
SELECT
    'admin2@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Customer',
    'Admin Two',
    'CUSTOMER_ADMIN',
    id,
    true
FROM tenants WHERE subdomain = '2';

-- Insert sample end users (password: User@123)
INSERT INTO users (email, password, first_name, last_name, role, tenant_id, active)
SELECT
    'user1@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'End',
    'User One',
    'END_USER',
    id,
    true
FROM tenants WHERE subdomain = '1';

INSERT INTO users (email, password, first_name, last_name, role, tenant_id, active)
SELECT
    'user2@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'End',
    'User Two',
    'END_USER',
    id,
    true
FROM tenants WHERE subdomain = '2';