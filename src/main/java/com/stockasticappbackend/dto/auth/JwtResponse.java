package com.stockasticappbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO for successful authentication.
 * Contains the JWT token and the user's role for client-side authorization.
 */
@Data
@AllArgsConstructor
public class JwtResponse {

    /** The JWT access token. */
    private String token;

    /** The user's role (e.g., "ROLE_USER", "ROLE_ADMIN"). */
    private String role;
    
    /** The refresh token. */
    private String refreshToken;
}
