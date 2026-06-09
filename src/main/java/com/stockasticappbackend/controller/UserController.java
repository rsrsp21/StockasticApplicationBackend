package com.stockasticappbackend.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stockasticappbackend.dto.user.ChangePasswordRequest;
import com.stockasticappbackend.dto.user.CreateUserRequest;
import com.stockasticappbackend.dto.user.UpdateProfileRequest;
import com.stockasticappbackend.dto.user.UserKycStatusResponse;
import com.stockasticappbackend.dto.user.UserResponse;
import com.stockasticappbackend.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for user account management.
 * Provides endpoints for user registration, profile management, KYC submission,
 * and password changes.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Creates a new user account.
     *
     * @param request The user creation request containing name, email, password,
     *                and mobile.
     * @return UserResponse containing the created user's details.
     */
    @PostMapping("/create")
    public UserResponse create(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param auth The authentication object containing user credentials.
     * @return ResponseEntity containing the UserResponse.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> profile(Authentication auth) {
        UserResponse response = userService.getProfile(auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param auth    The authentication object.
     * @param request The update request containing name and/or mobile.
     * @return UserResponse with updated user details.
     */
    @PutMapping("/me")
    public UserResponse update(
            Authentication auth,
            @RequestBody @Valid UpdateProfileRequest request) {
        return userService.updateProfile(auth.getName(), request);
    }

    /**
     * Submits KYC documents for verification.
     *
     * @param authentication The authentication object.
     * @param panNumber      The user's PAN card number.
     * @param aadhaarNumber  The user's Aadhaar card number.
     * @param document       The KYC document file.
     * @return ResponseEntity with a success message.
     */
    @PostMapping(value = "/me/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitKyc(
            Authentication authentication,
            @RequestParam("panNumber") String panNumber,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam("document") MultipartFile document) {

        userService.submitKyc(
                authentication.getName(),
                panNumber,
                aadhaarNumber,
                document);

        return ResponseEntity.ok("KYC submitted successfully and is under review");
    }

    /**
     * Uploads or updates the profile image for the authenticated user.
     *
     * @param authentication The authentication object.
     * @param file           The profile image file.
     * @return ResponseEntity containing the updated UserResponse.
     * @throws IOException If the file cannot be processed.
     */
    @PutMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {

        UserResponse response = userService.uploadProfileImage(authentication.getName(), file);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft deletes the currently authenticated user's account.
     *
     * @param auth The authentication object.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(Authentication auth) {
        userService.deleteProfile(auth.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param authentication The authentication object.
     * @param request        The change password request containing old and new
     *                       passwords.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest request) {

        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the KYC status for the currently authenticated user.
     *
     * @param authentication The authentication object.
     * @return UserKycStatusResponse containing KYC status details.
     */
    @GetMapping("/me/kyc-status")
    public UserKycStatusResponse getMyKycStatus(Authentication authentication) {
        return userService.getMyKycStatus(authentication.getName());
    }
}