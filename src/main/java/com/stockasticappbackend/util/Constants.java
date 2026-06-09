package com.stockasticappbackend.util;

public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Common Exception Messages
    public static final String USER_NOT_FOUND_ID = "User not found with ID: ";
    public static final String USER_NOT_FOUND_EMAIL = "User not found with email: ";

    // Watchlist Messages
    public static final String WATCHLIST_NOT_FOUND = "Watchlist not found or access denied";
    public static final String WATCHLIST_DUPLICATE_NAME = "Watchlist with name '%s' already exists";
    public static final String STOCK_NOT_FOUND_ID = "Stock not found with ID: ";
    public static final String STOCK_ALREADY_IN_WATCHLIST = "Stock already exists in watchlist";
    public static final String STOCK_NOT_IN_WATCHLIST = "Stock not found in watchlist";

    // Stock Messages
    public static final String STOCK_NOT_FOUND_SYMBOL = "Stock not found with symbol: ";
    public static final String NO_PRICE_DATA_ID = "No price data found for stock ID: ";
    public static final String NO_PRICE_DATA_SYMBOL = "No price data found for symbol: ";
    public static final String STOCK_DUPLICATE_SYMBOL = "Stock with symbol %s already exists";
    public static final String STOCK_IMAGE_TYPE_ERROR = "Only WEBP images are allowed";
    public static final String STOCK_IMAGE_UPLOAD_ERROR = "Failed to upload stock image";

    // User Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String INVALID_OLD_PASSWORD = "Old password is incorrect";
    public static final String ACCOUNT_BLOCKED_CONTACT_HELP = "Your account is blocked. Contact Help";
    public static final String FILE_REQUIRED = "File is required";
    public static final String PROFILE_IMAGE_TYPE_ERROR = "Only JPG or PNG images are allowed";
    public static final String PROFILE_IMAGE_UPLOAD_ERROR = "Failed to upload profile image";

    // Auth Messages
    public static final String REFRESH_TOKEN_MISSING_IN_COOKIE = "Refresh token is missing in cookie!";
    public static final String USER_ACCOUNT_NOT_ACTIVE = "User account is not active";
    public static final String REFRESH_TOKEN_NOT_IN_DATABASE = "Refresh token is not in database!";
    public static final String LOGOUT_SUCCESS = "Logged out successfully";
    public static final String TOKEN_REFRESH_ERROR_TEMPLATE = "Failed for info [%s]: %s";

    // Validation Messages
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String STOCK_ID_REQUIRED = "Stock ID is required";
    public static final String ORDER_TYPE_REQUIRED = "Order type is required";
    public static final String ORDER_MODE_REQUIRED = "Order mode is required";
    public static final String QUANTITY_REQUIRED = "Quantity is required";
    public static final String QUANTITY_MIN_ONE = "Quantity must be at least 1";
    public static final String FREQUENCY_REQUIRED = "Frequency is required";
    public static final String START_DATE_REQUIRED = "Start date is required";
    public static final String START_DATE_PRESENT_OR_FUTURE = "Start date must be in the present or future";
    public static final String START_DATE_MUST_BE_FUTURE = "Start date must be after today";
    public static final String START_DATE_MUST_BE_TRADING_DAY = "Start date must be a trading day (no weekend/holiday)";
    public static final String STOCK_SYMBOL_REQUIRED = "Stock symbol is required";
    public static final String SYMBOL_MAX_10 = "Symbol must not exceed 10 characters";
    public static final String STOCK_NAME_REQUIRED = "Stock name is required";
    public static final String EXCHANGE_REQUIRED = "Exchange is required";
    public static final String EXCHANGE_MAX_50 = "Exchange must not exceed 50 characters";
    public static final String SECTOR_MAX_100 = "Sector must not exceed 100 characters";
    public static final String DESCRIPTION_MAX_500 = "Description must not exceed 500 characters";
    public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
    public static final String NEW_PASSWORD_REQUIRED = "New password is required";
    public static final String NEW_PASSWORD_MIN_8 = "New password must be at least 8 characters long";
    public static final String NEW_PASSWORD_COMPLEXITY = "New password must contain uppercase, lowercase, number, and special character";
    public static final String NAME_REQUIRED = "Name is required";
    public static final String NAME_RANGE_2_50 = "Name must be between 2 and 50 characters";
    public static final String PASSWORD_MIN_8 = "Password must be at least 8 characters long";
    public static final String PASSWORD_COMPLEXITY = "Password must contain uppercase, lowercase, number, and special character";
    public static final String MOBILE_10_DIGIT_INDIAN = "Mobile number must be a valid 10-digit Indian number";
    public static final String REJECTION_REASON_REQUIRED = "Rejection reason is required";
    public static final String REJECTION_REASON_RANGE_5_255 = "Rejection reason must be between 5 and 255 characters";
    public static final String AMOUNT_REQUIRED = "Amount is required";
    public static final String MIN_DEPOSIT_AMOUNT_1 = "Minimum deposit amount is ₹1";
    public static final String DESCRIPTION_MAX_255 = "Description cannot exceed 255 characters";
    public static final String PAYMENT_METHOD_REQUIRED = "Payment method is required";
    public static final String OTP_REQUIRED = "OTP is required";
    public static final String OTP_4_DIGITS = "OTP must be 4 digits";
    public static final String BANK_NAME_REQUIRED = "Bank name is required";
    public static final String BANK_NAME_MAX_100 = "Bank name cannot exceed 100 characters";
    public static final String ACCOUNT_NUMBER_REQUIRED = "Account number is required";
    public static final String ACCOUNT_NUMBER_RANGE_9_18 = "Account number must be between 9 and 18 digits";
    public static final String ACCOUNT_NUMBER_DIGITS_ONLY = "Account number must contain only digits";
    public static final String IFSC_REQUIRED = "IFSC code is required";
    public static final String IFSC_11_CHARS = "IFSC code must be 11 characters";
    public static final String IFSC_FORMAT_INVALID = "Invalid IFSC code format";
    public static final String ACCOUNT_HOLDER_NAME_REQUIRED = "Account holder name is required";
    public static final String HOLDER_NAME_MAX_100 = "Holder name cannot exceed 100 characters";
    public static final String PURPOSE_REQUIRED = "Purpose is required";
    public static final String MIN_WITHDRAWAL_AMOUNT_1 = "Minimum withdrawal amount is ₹1";
    public static final String BANK_ACCOUNT_ID_REQUIRED = "Bank account ID is required";
    public static final String WATCHLIST_NAME_REQUIRED = "Watchlist name is required";
    public static final String NAME_MAX_100_OR_LESS = "Name must be 100 characters or less";

    // KYC Messages
    public static final String KYC_NOT_FOUND = "KYC not found";
    public static final String KYC_ALREADY_REVIEWED = "KYC already reviewed";
    public static final String KYC_ALREADY_UNDER_REVIEW = "KYC already under review";
    public static final String KYC_ALREADY_APPROVED = "KYC already approved";
    public static final String KYC_USER_BLOCKED = "User is blocked due to KYC failures";
    public static final String KYC_MAX_ATTEMPTS = "Maximum KYC attempts exceeded";
    public static final String KYC_DOCUMENT_EMPTY = "KYC document is empty";
    public static final String KYC_DOCUMENT_NOT_FOUND = "KYC document not found";
    public static final String KYC_DOCUMENT_ERROR = "Error loading KYC document";
    public static final String KYC_DOCUMENT_STORE_ERROR = "Failed to store KYC document";

    // Stock Price Module Constants
    public static final int CANDLE_INTERVAL_MINUTES = 5;
    public static final int MAX_PRICE_RECORDS_PER_STOCK = 1000;
    public static final int HUNDRED = 100;

    // Stock Price Log Messages
    public static final String LOG_FAILED_REFRESH_STOCK = "Failed to refresh data for stock: {}";
    public static final String LOG_FAILED_FETCH_PRICE = "Failed to fetch price for symbol: {}";
    public static final String LOG_FAILED_CLEAR_PRICES = "Failed to clear prices for stock: {}";

    // Wallet Messages
    public static final String WALLET_NOT_FOUND = "Wallet not found";
    public static final String INSUFFICIENT_FUNDS = "Insufficient funds in wallet";
    public static final String INVALID_DEPOSIT_AMOUNT = "Deposit amount must be greater than zero";
    public static final String INVALID_WITHDRAWAL_AMOUNT = "Withdrawal amount must be greater than zero";

    // Bank Account Messages
    public static final String BANK_ACCOUNT_NOT_FOUND = "Bank account not found";
    public static final String BANK_ACCOUNT_ALREADY_LINKED = "This bank account is already linked";

    // OTP Messages
    public static final String OTP_SENT_SUCCESS = "OTP sent successfully";
    public static final String OTP_INVALID = "Invalid or expired OTP";
    public static final String OTP_EXPIRED = "OTP has expired";

    // Order Messages
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String INSUFFICIENT_HOLDINGS = "Insufficient holdings to sell";
    public static final String INVALID_ORDER_QUANTITY = "Order quantity must be greater than zero";
    public static final String LIMIT_PRICE_REQUIRED = "Price is required for limit orders";
    public static final String KYC_NOT_APPROVED = "KYC approval is required to trade. Please complete your KYC verification.";

    // Bulk Upload Messages
    public static final String BULK_UPLOAD_EMPTY_FILE = "Uploaded file is empty";
    public static final String BULK_UPLOAD_INVALID_FORMAT = "Invalid file format. Only CSV and Excel (.xlsx, .xls) files are supported";
    public static final String BULK_UPLOAD_MISSING_HEADERS = "Required columns missing. File must contain: symbol, name, exchange";
    public static final String BULK_UPLOAD_ROW_SYMBOL_REQUIRED = "Stock symbol is required";
    public static final String BULK_UPLOAD_ROW_NAME_REQUIRED = "Stock name is required";
    public static final String BULK_UPLOAD_ROW_EXCHANGE_REQUIRED = "Exchange is required";
    public static final String BULK_UPLOAD_ROW_SYMBOL_TOO_LONG = "Symbol must not exceed 10 characters";
    public static final String BULK_UPLOAD_ROW_EXCHANGE_TOO_LONG = "Exchange must not exceed 50 characters";
    public static final String BULK_UPLOAD_ROW_SECTOR_TOO_LONG = "Sector must not exceed 100 characters";
    public static final String BULK_UPLOAD_ROW_DESC_TOO_LONG = "Description must not exceed 500 characters";
    public static final String BULK_UPLOAD_DUPLICATE_IN_FILE = "Duplicate symbol within the uploaded file";
    public static final String BULK_UPLOAD_PARSE_ERROR = "Failed to parse the uploaded file";

}

