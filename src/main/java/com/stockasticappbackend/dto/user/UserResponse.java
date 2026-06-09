package com.stockasticappbackend.dto.user;

import lombok.Data;

/**
 * Response DTO for user profile information.
 */
@Data
public class UserResponse {

    /** The unique identifier of the user. */
    private Long userId;

    /** The user's display name. */
    private String name;

    /** The user's email address. */
    private String email;

    /** The user's mobile number. */
    private String mobile;

    /** The path to the user's profile image. */
    private String profileImagePath;

    /** The user's role (USER or ADMIN). */
    private String role;

    /** The user's account status (ACTIVE, BLOCKED, DELETED). */
    private String userStatus;
}
