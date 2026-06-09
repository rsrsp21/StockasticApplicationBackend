package com.stockasticappbackend.service.wallet;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of OtpService.
 * Uses in-memory storage for OTPs with expiry time.
 * In production, this would integrate with an SMS/email gateway.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    private static final int OTP_LENGTH = 4;
    private static final int OTP_EXPIRY_MINUTES = 5;

    /** In-memory store for OTPs: key = email + purpose, value = OtpData */
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    private final Random random = new Random();

    /**
     * Internal class to store OTP data with expiry.
     */
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;

        OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    @Override
    public String generateOtp(String email, String purpose) {
        // Generate 4-digit OTP
        String otp = String.format("%04d", random.nextInt(10000));
        
        // Store with expiry time
        String key = buildKey(email, purpose);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStore.put(key, new OtpData(otp, expiryTime));

        // Log OTP for demo purposes (in production, send via SMS/email)
        logger.info("========================================");
        logger.info("OTP for {} ({}): {}", email, purpose, otp);
        logger.info("Expires at: {}", expiryTime);
        logger.info("========================================");

        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp, String purpose) {
        String key = buildKey(email, purpose);
        OtpData otpData = otpStore.get(key);

        if (otpData == null) {
            logger.warn("No OTP found for key: {}", key);
            return false;
        }

        if (otpData.isExpired()) {
            logger.warn("OTP expired for key: {}", key);
            otpStore.remove(key);
            return false;
        }

        boolean isValid = otpData.otp.equals(otp);
        if (!isValid) {
            logger.warn("Invalid OTP attempt for key: {}", key);
        }

        return isValid;
    }

    @Override
    public void invalidateOtp(String email, String purpose) {
        String key = buildKey(email, purpose);
        otpStore.remove(key);
        logger.info("OTP invalidated for key: {}", key);
    }

    /**
     * Builds a unique key for OTP storage.
     *
     * @param email   The user's email.
     * @param purpose The purpose of the OTP.
     * @return The combined key.
     */
    private String buildKey(String email, String purpose) {
        return email + ":" + purpose;
    }
}
