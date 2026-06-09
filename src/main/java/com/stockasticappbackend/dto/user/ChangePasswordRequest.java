package com.stockasticappbackend.dto.user;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing user password.
 */
@Data
@NoArgsConstructor
public class ChangePasswordRequest {

    /** The user's current password for verification. */
    @NotBlank(message = Constants.CURRENT_PASSWORD_REQUIRED)
    private String oldPassword;

    /** The new password to set. */
    @NotBlank(message = Constants.NEW_PASSWORD_REQUIRED)
    @Size(min = 8, message = Constants.NEW_PASSWORD_MIN_8)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = Constants.NEW_PASSWORD_COMPLEXITY
    )
    private String newPassword;
}


