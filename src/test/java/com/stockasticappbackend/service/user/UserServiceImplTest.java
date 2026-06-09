package com.stockasticappbackend.service.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.user.ChangePasswordRequest;
import com.stockasticappbackend.dto.user.CreateUserRequest;
import com.stockasticappbackend.dto.user.UpdateProfileRequest;
import com.stockasticappbackend.dto.user.UserKycStatusResponse;
import com.stockasticappbackend.dto.user.UserResponse;
import com.stockasticappbackend.exception.EmailAlreadyExistsException;
import com.stockasticappbackend.exception.InvalidCredentialsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.KycRepository;

@SpringBootTest
@Transactional
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository userRepository;
    
    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        autoSellRuleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldReturnUserResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");
        request.setPassword("password123");
        request.setMobile("1234567890");
        UserResponse response = userService.createUser(request);
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());

        AppUser savedUser = userRepository.findByEmail("test@example.com").orElse(null);
        assertNotNull(savedUser);
        assertTrue(passwordEncoder.matches("password123", savedUser.getPasswordHash()));
    }

    @Test
    void createUser_DuplicateEmail_ShouldThrowException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("duplicate@example.com");
        request.setName("First User");
        request.setPassword("password123");
        userService.createUser(request);

        CreateUserRequest duplicateRequest = new CreateUserRequest();
        duplicateRequest.setEmail("duplicate@example.com");
        duplicateRequest.setName("Second User");
        duplicateRequest.setPassword("password456");
        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.createUser(duplicateRequest);
        });
    }

    @Test
    void getProfile_ShouldReturnUserResponse() {
        AppUser user = new AppUser();
        user.setEmail("profile@example.com");
        user.setName("Profile User");
        user.setPasswordHash("hash");
        userRepository.save(user);
        UserResponse response = userService.getProfile("profile@example.com");
        assertNotNull(response);
        assertEquals("profile@example.com", response.getEmail());
    }

    @Test
    void getProfile_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getProfile("nonexistent@example.com");
        });
    }

    @Test
    void updateProfile_ShouldUpdateDetails() {
        AppUser user = new AppUser();
        user.setEmail("update@example.com");
        user.setName("Old Name");
        user.setMobile("1111111111");
        user.setPasswordHash("hash");
        userRepository.save(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        request.setMobile("2222222222");
        UserResponse response = userService.updateProfile("update@example.com", request);
        assertEquals("New Name", response.getName());
        assertEquals("2222222222", response.getMobile());
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        AppUser user = new AppUser();
        user.setEmail("password@example.com");
        user.setName("Pass User");
        user.setPasswordHash(passwordEncoder.encode("oldPassword"));
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        userService.changePassword("password@example.com", request);
        AppUser updatedUser = userRepository.findByEmail("password@example.com").orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches("newPassword", updatedUser.getPasswordHash()));
    }

    @Test
    void changePassword_InvalidOldPassword_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("invalid@example.com");
        user.setName("Invalid User");
        user.setPasswordHash(passwordEncoder.encode("correctPassword"));
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.changePassword("invalid@example.com", request);
        });
    }

    @Test
    void deleteProfile_ShouldSetStatusDeleted() {
        AppUser user = new AppUser();
        user.setEmail("delete@example.com");
        user.setName("Del User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        userService.deleteProfile("delete@example.com");
        AppUser deletedUser = userRepository.findByEmail("delete@example.com").orElse(null);
        assertNotNull(deletedUser);
        assertEquals(UserStatus.DELETED, deletedUser.getUserStatus());
    }

    @Test
    void submitKyc_FirstTime_ShouldCreateKycRecord() {
        AppUser user = new AppUser();
        user.setEmail("kyc@example.com");
        user.setName("Kyc User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());
        userService.submitKyc("kyc@example.com", "PAN123", "AADHAAR123", doc);
        Kyc kyc = kycRepository.findByUser(user).orElse(null);
        assertNotNull(kyc);
        assertEquals("PAN123", kyc.getPanNumber());
        assertEquals("AADHAAR123", kyc.getAadhaarNumber());
        assertEquals(KycStatus.PENDING, kyc.getKycStatus());
    }

    @Test
    void uploadProfileImage_ShouldSaveFileName() throws IOException {
        AppUser user = new AppUser();
        user.setEmail("image@example.com");
        user.setName("Img User");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        MockMultipartFile image = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image content".getBytes());
        UserResponse response = userService.uploadProfileImage("image@example.com", image);
        assertNotNull(response.getProfileImagePath());
        assertTrue(response.getProfileImagePath().contains("user_" + user.getUserId()));
    }

    @Test
    void getMyKycStatus_NotAttempted_ShouldReturnStatus() {
        AppUser user = new AppUser();
        user.setEmail("nokyc@example.com");
        user.setName("No Kyc");
        user.setPasswordHash("hash");
        userRepository.save(user);
        UserKycStatusResponse response = userService.getMyKycStatus("nokyc@example.com");
        assertEquals(KycStatus.NOT_ATTEMPTED, response.getKycStatus());
        assertEquals(0, response.getAttemptCount());
    }

    @Test
    void getMyKycStatus_WithExistingKyc_ShouldReturnKycDetails() {
        AppUser user = new AppUser();
        user.setEmail("kycdetail@example.com");
        user.setName("Kyc Detail");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.REJECTED);
        kyc.setPanNumber("PAN999");
        kyc.setAadhaarNumber("AAD999");
        kyc.setDocumentPath("test_path");
        kyc.setAttemptCount(2);
        kyc.setRejectionReason("Blurry document");
        kyc.setSubmittedAt(java.time.LocalDateTime.now());
        kycRepository.save(kyc);
        UserKycStatusResponse response = userService.getMyKycStatus("kycdetail@example.com");
        assertEquals(KycStatus.REJECTED, response.getKycStatus());
        assertEquals(2, response.getAttemptCount());
        assertEquals("Blurry document", response.getRejectionReason());
        assertNotNull(response.getSubmittedAt());
    }

    @Test
    void getMyKycStatus_PendingKyc_ShouldReturnPending() {
        AppUser user = new AppUser();
        user.setEmail("kycpending@example.com");
        user.setName("Kyc Pending");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setPanNumber("PAN111");
        kyc.setAadhaarNumber("AAD111");
        kyc.setDocumentPath("test_path");
        kyc.setAttemptCount(1);
        kyc.setSubmittedAt(java.time.LocalDateTime.now());
        kycRepository.save(kyc);
        UserKycStatusResponse response = userService.getMyKycStatus("kycpending@example.com");
        assertEquals(KycStatus.PENDING, response.getKycStatus());
        assertEquals(1, response.getAttemptCount());
    }

    @Test
    void getMyKycStatus_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getMyKycStatus("ghost@example.com");
        });
    }

    @Test
    void submitKyc_BlockedUser_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("blocked@example.com");
        user.setName("Blocked User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());
        assertThrows(IllegalStateException.class, () -> {
            userService.submitKyc("blocked@example.com", "PAN123", "AADHAAR123", doc);
        });
    }

    @Test
    void submitKyc_PendingKyc_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("pendingkyc@example.com");
        user.setName("Pending User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setPanNumber("PAN123");
        kyc.setAadhaarNumber("AAD123");
        kyc.setDocumentPath("test_path");
        kyc.setSubmittedAt(java.time.LocalDateTime.now());
        kycRepository.save(kyc);

        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());
        assertThrows(IllegalStateException.class, () -> {
            userService.submitKyc("pendingkyc@example.com", "PAN456", "AADHAAR456", doc);
        });
    }

    @Test
    void submitKyc_AlreadyApproved_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("approvedkyc@example.com");
        user.setName("Approved User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.APPROVED);
        kyc.setPanNumber("PAN123");
        kyc.setAadhaarNumber("AAD123");
        kyc.setDocumentPath("test_path");
        kyc.setSubmittedAt(java.time.LocalDateTime.now());
        kycRepository.save(kyc);

        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());
        assertThrows(IllegalStateException.class, () -> {
            userService.submitKyc("approvedkyc@example.com", "PAN456", "AADHAAR456", doc);
        });
    }

    @Test
    void submitKyc_ReSubmitAfterRejection_ShouldUpdateKyc() {
        AppUser user = new AppUser();
        user.setEmail("resubmitkyc@example.com");
        user.setName("Resubmit User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.REJECTED);
        kyc.setPanNumber("OLDPAN");
        kyc.setAadhaarNumber("OLDAAD");
        kyc.setDocumentPath("old_path");
        kyc.setAttemptCount(1);
        kyc.setRejectionReason("Blurry");
        kyc.setSubmittedAt(java.time.LocalDateTime.now().minusDays(1));
        kycRepository.save(kyc);

        MockMultipartFile doc = new MockMultipartFile("document", "newdoc.pdf", "application/pdf", "new content".getBytes());
        userService.submitKyc("resubmitkyc@example.com", "NEWPAN", "NEWAAD", doc);
        Kyc updatedKyc = kycRepository.findByUser(user).orElse(null);
        assertNotNull(updatedKyc);
        assertEquals("NEWPAN", updatedKyc.getPanNumber());
        assertEquals("NEWAAD", updatedKyc.getAadhaarNumber());
        assertEquals(KycStatus.PENDING, updatedKyc.getKycStatus());
        assertEquals(2, updatedKyc.getAttemptCount());
        assertEquals(null, updatedKyc.getRejectionReason());
    }

    @Test
    void submitKyc_MaxAttempts_ShouldBlockUserAndThrow() {
        AppUser user = new AppUser();
        user.setEmail("maxkyc@example.com");
        user.setName("Max Attempts User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(KycStatus.REJECTED);
        kyc.setPanNumber("PAN123");
        kyc.setAadhaarNumber("AAD123");
        kyc.setDocumentPath("test_path");
        kyc.setAttemptCount(3);
        kycRepository.save(kyc);

        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());
        assertThrows(IllegalStateException.class, () -> {
            userService.submitKyc("maxkyc@example.com", "PAN456", "AADHAAR456", doc);
        });
        AppUser blockedUser = userRepository.findByEmail("maxkyc@example.com").orElse(null);
        assertNotNull(blockedUser);
        assertEquals(UserStatus.BLOCKED, blockedUser.getUserStatus());
    }

    @Test
    void submitKyc_UserNotFound_ShouldThrowException() {
        MockMultipartFile doc = new MockMultipartFile("document", "doc.pdf", "application/pdf", "content".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.submitKyc("ghost@example.com", "PAN", "AAD", doc);
        });
    }

    @Test
    void uploadProfileImage_NullFile_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("nullimg@example.com");
        user.setName("Null Img");
        user.setPasswordHash("hash");
        userRepository.save(user);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadProfileImage("nullimg@example.com", null);
        });
    }

    @Test
    void uploadProfileImage_EmptyFile_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("emptyimg@example.com");
        user.setName("Empty Img");
        user.setPasswordHash("hash");
        userRepository.save(user);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadProfileImage("emptyimg@example.com", emptyFile);
        });
    }

    @Test
    void uploadProfileImage_InvalidContentType_ShouldThrowException() {
        AppUser user = new AppUser();
        user.setEmail("badtype@example.com");
        user.setName("Bad Type");
        user.setPasswordHash("hash");
        userRepository.save(user);

        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", "content".getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadProfileImage("badtype@example.com", file);
        });
    }

    @Test
    void uploadProfileImage_PngFile_ShouldSaveFileName() throws IOException {
        AppUser user = new AppUser();
        user.setEmail("pngimg@example.com");
        user.setName("PNG Img");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        MockMultipartFile image = new MockMultipartFile("file", "test.png", "image/png", "png content".getBytes());
        UserResponse response = userService.uploadProfileImage("pngimg@example.com", image);
        assertNotNull(response.getProfileImagePath());
        assertTrue(response.getProfileImagePath().endsWith(".png"));
    }

    @Test
    void uploadProfileImage_UserNotFound_ShouldThrowException() {
        MockMultipartFile image = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.uploadProfileImage("nobody@example.com", image);
        });
    }

    @Test
    void updateProfile_OnlyName_ShouldKeepMobile() {
        AppUser user = new AppUser();
        user.setEmail("partial@example.com");
        user.setName("Old Name");
        user.setMobile("9999999999");
        user.setPasswordHash("hash");
        userRepository.save(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        UserResponse response = userService.updateProfile("partial@example.com", request);
        assertEquals("New Name", response.getName());
        assertEquals("9999999999", response.getMobile());
    }

    @Test
    void updateProfile_OnlyMobile_ShouldKeepName() {
        AppUser user = new AppUser();
        user.setEmail("partialm@example.com");
        user.setName("Keep Name");
        user.setMobile("1111111111");
        user.setPasswordHash("hash");
        userRepository.save(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setMobile("2222222222");
        UserResponse response = userService.updateProfile("partialm@example.com", request);
        assertEquals("Keep Name", response.getName());
        assertEquals("2222222222", response.getMobile());
    }

    @Test
    void updateProfile_UserNotFound_ShouldThrowException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Test");

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateProfile("nobody@example.com", request);
        });
    }

    @Test
    void deleteProfile_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteProfile("nobody@example.com");
        });
    }

    @Test
    void changePassword_UserNotFound_ShouldThrowException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.changePassword("nobody@example.com", request);
        });
    }
}


