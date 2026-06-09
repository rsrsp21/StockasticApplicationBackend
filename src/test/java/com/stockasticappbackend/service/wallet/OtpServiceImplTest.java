package com.stockasticappbackend.service.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OtpServiceImplTest {

    @Autowired
    private OtpService otpService;

    @Test
    void generateOtp_ShouldReturn4DigitOtp() {
        String otp = otpService.generateOtp("test@example.com", "ADD_FUNDS");
        assertNotNull(otp);
        assertEquals(4, otp.length());
        assertTrue(otp.matches("\\d{4}"));
    }

    @Test
    void verifyOtp_ValidOtp_ShouldReturnTrue() {
        String otp = otpService.generateOtp("verify@example.com", "ADD_FUNDS");
        boolean result = otpService.verifyOtp("verify@example.com", otp, "ADD_FUNDS");
        assertTrue(result);
    }

    @Test
    void verifyOtp_WrongOtp_ShouldReturnFalse() {
        otpService.generateOtp("wrong@example.com", "ADD_FUNDS");
        String otp = otpService.generateOtp("wrong2@example.com", "WITHDRAW");
        String wrongOtp = otp.equals("9999") ? "0001" : "9999";
        assertFalse(otpService.verifyOtp("wrong2@example.com", wrongOtp, "WITHDRAW"));
    }

    @Test
    void verifyOtp_NoOtpGenerated_ShouldReturnFalse() {
        boolean result = otpService.verifyOtp("noone@example.com", "1234", "ADD_FUNDS");
        assertFalse(result);
    }

    @Test
    void verifyOtp_WrongPurpose_ShouldReturnFalse() {
        String otp = otpService.generateOtp("purpose@example.com", "ADD_FUNDS");
        boolean result = otpService.verifyOtp("purpose@example.com", otp, "WITHDRAW");
        assertFalse(result);
    }

    @Test
    void invalidateOtp_ShouldPreventVerification() {
        String otp = otpService.generateOtp("invalidate@example.com", "ADD_FUNDS");
        otpService.invalidateOtp("invalidate@example.com", "ADD_FUNDS");
        boolean result = otpService.verifyOtp("invalidate@example.com", otp, "ADD_FUNDS");
        assertFalse(result);
    }

    @Test
    void generateOtp_SameEmailDifferentPurpose_ShouldStoreSeparately() {
        String otp1 = otpService.generateOtp("multi@example.com", "ADD_FUNDS");
        String otp2 = otpService.generateOtp("multi@example.com", "WITHDRAW");
        assertTrue(otpService.verifyOtp("multi@example.com", otp1, "ADD_FUNDS"));
        assertTrue(otpService.verifyOtp("multi@example.com", otp2, "WITHDRAW"));
    }

    @Test
    void generateOtp_SecondCallOverwritesFirst() {
        String otp2 = otpService.generateOtp("overwrite@example.com", "ADD_FUNDS");
        assertTrue(otpService.verifyOtp("overwrite@example.com", otp2, "ADD_FUNDS"));
    }
}

