-- V1__init_schema.sql
-- Create tenants table
CREATE TABLE tenants (
                         id BIGSERIAL PRIMARY KEY,
                         subdomain VARCHAR(100) UNIQUE NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT true,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create users table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       role VARCHAR(50) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'CUSTOMER_ADMIN', 'END_USER')),
                       tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                       active BOOLEAN NOT NULL DEFAULT true,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_tenant_role ON users(tenant_id, role);
CREATE UNIQUE INDEX idx_email_tenant ON users(email, tenant_id);

-- Create SSO config table
CREATE TABLE sso_config (
                            id BIGSERIAL PRIMARY KEY,
                            tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                            provider VARCHAR(50) NOT NULL CHECK (provider IN ('JWT', 'SAML', 'OAUTH')),
                            enabled BOOLEAN NOT NULL DEFAULT false,
                            idp_entity_id VARCHAR(512),

    -- JWT fields
                            jwt_secret VARCHAR(512),
                            jwt_token_endpoint VARCHAR(512),
                            jwt_userinfo_endpoint VARCHAR(512),
                            jwt_issuer VARCHAR(255),
                            jwt_audience VARCHAR(255),

    -- SAML fields
                            saml_sso_url VARCHAR(512),
                            saml_certificate TEXT,
                            saml_sp_entity_id VARCHAR(255),
                            saml_acs_url VARCHAR(512),

    -- OAuth fields
                            oauth_client_id VARCHAR(255),
                            oauth_client_secret VARCHAR(512),
                            oauth_authorization_url VARCHAR(512),
                            oauth_token_url VARCHAR(512),
                            oauth_redirect_uri VARCHAR(512),
                            oauth_scopes VARCHAR(512),

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT unique_tenant_provider UNIQUE (tenant_id, provider)
);
