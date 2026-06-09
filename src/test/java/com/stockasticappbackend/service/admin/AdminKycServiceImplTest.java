package com.stockasticappbackend.service.admin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PendingKycResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;

@SpringBootTest
@Transactional
class AdminKycServiceImplTest {

    @Autowired
    private AdminKycService adminKycService;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private AppUserRepository userRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("kyc_admin@example.com");
        testUser.setName("KYC User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void approveKyc_ShouldUpdateStatusToApproved() {
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setPanNumber("ABC1234");
        kyc.setAadhaarNumber("12345678");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        adminKycService.approveKyc(testUser.getUserId());
        Kyc updatedKyc = kycRepository.findByUser(testUser).get();
        assertEquals(KycStatus.APPROVED, updatedKyc.getKycStatus());
        assertNotNull(updatedKyc.getReviewedAt());
    }

    @Test
    void rejectKyc_ShouldUpdateStatusToRejected() {
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setPanNumber("ABC1234");
        kyc.setAadhaarNumber("12345678");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        adminKycService.rejectKyc(testUser.getUserId(), "Invalid document");
        Kyc updatedKyc = kycRepository.findByUser(testUser).get();
        assertEquals(KycStatus.REJECTED, updatedKyc.getKycStatus());
        assertEquals("Invalid document", updatedKyc.getRejectionReason());
    }

    @Test
    void rejectKyc_MaxAttempts_ShouldBlockUser() {
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setAttemptCount(3);
        kyc.setPanNumber("ABC1234");
        kyc.setAadhaarNumber("12345678");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        adminKycService.rejectKyc(testUser.getUserId(), "Final rejection");
        AppUser updatedUser = userRepository.findById(testUser.getUserId()).get();
        assertEquals(UserStatus.BLOCKED, updatedUser.getUserStatus());
    }

    @Test
    void getPendingKycs_ShouldReturnPagedResponse() {
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.PENDING);
        kyc.setPanNumber("ABC1234");
        kyc.setAadhaarNumber("12345678");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        PageResponse<PendingKycResponse> response = adminKycService.getPendingKycs(0, 10);
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void approveKyc_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            adminKycService.approveKyc(999999L);
        });
    }

    @Test
    void approveKyc_AlreadyReviewed_ShouldThrowException() {
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.APPROVED);
        kyc.setPanNumber("ABC1234");
        kyc.setAadhaarNumber("12345678");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        assertThrows(IllegalStateException.class, () -> {
            adminKycService.approveKyc(testUser.getUserId());
        });
    }
}

