-- V3__create_sso_sample_config.sql
-- Insert sample SSO configurations for tenant 1 (disabled by default)
INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'JWT', false FROM tenants WHERE subdomain = '1';

INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'SAML', false FROM tenants WHERE subdomain = '1';

INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'OAUTH', false FROM tenants WHERE subdomain = '1';

-- Insert sample SSO configurations for superadmin tenant
INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'JWT', false FROM tenants WHERE subdomain = 'superadmin';

INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'SAML', false FROM tenants WHERE subdomain = 'superadmin';

INSERT INTO sso_config (tenant_id, provider, enabled)
SELECT id, 'OAUTH', false FROM tenants WHERE subdomain = 'superadmin';