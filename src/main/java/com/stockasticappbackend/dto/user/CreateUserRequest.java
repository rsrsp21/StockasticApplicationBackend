package com.stockasticappbackend.dto.user;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 */
@Data
@NoArgsConstructor
public class CreateUserRequest {

    /** The user's display name. */
    @NotBlank(message = Constants.NAME_REQUIRED)
    @Size(min = 2, max = 50, message = Constants.NAME_RANGE_2_50)
    private String name;

    /** The user's email address (must be unique). */
    @NotBlank(message = Constants.EMAIL_REQUIRED)
    @Email(message = Constants.INVALID_EMAIL_FORMAT)
    private String email;

    /** The user's password. */
    @NotBlank(message = Constants.PASSWORD_REQUIRED)
    @Size(min = 8, message = Constants.PASSWORD_MIN_8)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = Constants.PASSWORD_COMPLEXITY
    )
    private String password;

    /** The user's mobile number (optional). */
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = Constants.MOBILE_10_DIGIT_INDIAN
    )
    private String mobile;
}


