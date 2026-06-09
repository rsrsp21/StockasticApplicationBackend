package com.stockasticappbackend.service.user;

import com.stockasticappbackend.dto.user.ChangePasswordRequest;
import com.stockasticappbackend.dto.user.CreateUserRequest;
import com.stockasticappbackend.dto.user.UpdateProfileRequest;
import com.stockasticappbackend.dto.user.UserKycStatusResponse;
import com.stockasticappbackend.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    UserResponse uploadProfileImage(String email, MultipartFile file) throws IOException;

    void deleteProfile(String email);

    void changePassword(String email, ChangePasswordRequest request);

	void submitKyc(String email, String panNumber, String aadhaarNumber, MultipartFile document);

	UserKycStatusResponse getMyKycStatus(String email);
}