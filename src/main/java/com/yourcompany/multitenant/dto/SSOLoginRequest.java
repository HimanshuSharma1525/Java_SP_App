package com.yourcompany.multitenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for handling SSO login requests.
 * This class is used by the /api/sso/authenticate endpoint.
 *
 * Depending on the provider type, only specific fields are required:
 * - JWT: token
 * - SAML: samlResponse
 * - OAUTH: code
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSOLoginRequest {

    @NotBlank(message = "Provider is required (JWT, SAML, or OAUTH)")
    private String provider;

    // JWT-based login (JWT token)
    private String token;

    // SAML-based login (Base64 encoded SAML response)
    private String samlResponse;

    // OAuth-based login (Authorization code)
    private String code;
}
