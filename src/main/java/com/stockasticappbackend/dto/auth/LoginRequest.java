package com.stockasticappbackend.dto.auth;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * Request DTO for user login.
 * Contains the credentials required for authentication.
 */
@Data
public class LoginRequest {

    /** The user's email address. */
    @NotBlank(message = Constants.EMAIL_REQUIRED)
    @Email(message = Constants.INVALID_EMAIL_FORMAT)
    private String email;

    /** The user's password. */
    @NotBlank(message = Constants.PASSWORD_REQUIRED)
    private String password;
}


