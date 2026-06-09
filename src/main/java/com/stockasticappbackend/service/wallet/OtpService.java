package com.stockasticappbackend.service.wallet;

/**
 * Service interface for OTP operations.
 */
public interface OtpService {

    /**
     * Generates and sends an OTP to the user.
     * In demo mode, logs the OTP to console.
     *
     * @param email   The user's email.
     * @param purpose The purpose of the OTP (ADD_FUNDS, WITHDRAW, LINK_BANK).
     * @return The generated OTP (for demo/testing purposes).
     */
    String generateOtp(String email, String purpose);

    /**
     * Verifies the OTP entered by the user.
     *
     * @param email   The user's email.
     * @param otp     The OTP to verify.
     * @param purpose The purpose of the OTP.
     * @return True if OTP is valid and not expired.
     */
    boolean verifyOtp(String email, String otp, String purpose);

    /**
     * Invalidates the OTP after successful use.
     *
     * @param email   The user's email.
     * @param purpose The purpose of the OTP.
     */
    void invalidateOtp(String email, String purpose);
}
