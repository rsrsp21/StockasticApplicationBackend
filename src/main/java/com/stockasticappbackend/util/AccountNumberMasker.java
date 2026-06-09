package com.stockasticappbackend.util;

/**
 * Utility class for masking sensitive account information.
 */
public final class AccountNumberMasker {

    private AccountNumberMasker() {
        // Prevent instantiation
    }

    /**
     * Masks an account number, showing only the last 4 digits.
     * 
     * Example:
     *   Input: "1234567890123456"
     *   Output: "XXXX-XXXX-3456"
     *
     * @param accountNumber The full account number.
     * @return Masked account number.
     */
    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "XXXX-XXXX-XXXX";
        }
        
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "XXXX-XXXX-" + last4;
    }
}
