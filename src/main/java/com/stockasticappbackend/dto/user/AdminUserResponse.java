package com.stockasticappbackend.dto.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for admin view of user information.
 * <p>
 * Contains summary user data for administrative purposes.
 * </p>
 */
@Data
@NoArgsConstructor
public class AdminUserResponse {

    /** The unique identifier of the user. */
    private Long userId;

    /** The user's display name. */
    private String name;

    /** The user's email address. */
    private String email;

    /** The user's role (USER or ADMIN). */
    private String role;

    /** The user's account status (ACTIVE, BLOCKED, DELETED). */
    private String status;

    /** The user's mobile number. */
    private String mobile;

    /** When the user account was created. */
    private LocalDateTime createdAt;
}