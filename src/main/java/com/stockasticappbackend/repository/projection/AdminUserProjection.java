package com.stockasticappbackend.repository.projection;

import java.time.LocalDateTime;

/**
 * Lightweight projection for admin user listing.
 * Spring Data JPA maps native query columns directly to these getters,
 * completely bypassing JPA entity loading and the N+1 problem.
 */
public interface AdminUserProjection {
    Long getUserId();
    String getName();
    String getEmail();
    String getRole();
    String getUserStatus();
    String getMobile();
    LocalDateTime getCreatedAt();
}
