package com.stockasticappbackend.dto.user;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Request DTO for updating user profile information.
 */
@Data
public class UpdateProfileRequest {

    /** The new display name (optional). */
    @Size(min = 2, max = 50, message = Constants.NAME_RANGE_2_50)
    private String name;

    /** The new mobile number (optional). */
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = Constants.MOBILE_10_DIGIT_INDIAN
    )
    private String mobile;
}


