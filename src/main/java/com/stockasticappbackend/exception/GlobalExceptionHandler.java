package com.stockasticappbackend.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.stockasticappbackend.util.ResponseBuilder;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 * Provides centralized exception handling across all controllers,
 * returning consistent JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles validation errors from @Valid annotated request bodies.
         * Returns field-level error messages in a structured format.
         *
         * @param ex The MethodArgumentNotValidException.
         * @return ResponseEntity with HTTP 400 status and field errors.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationErrors(
                        MethodArgumentNotValidException ex) {

                Map<String, String> fieldErrors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        fieldErrors.put(fieldName, errorMessage);
                });

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Validation Failed");
                response.put("message", "One or more fields have validation errors");
                response.put("fieldErrors", fieldErrors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * Handles ResourceNotFoundException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 404 status.
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleNotFound(
                        ResourceNotFoundException ex) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
        }

        /**
         * Handles EmailAlreadyExistsException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 409 status.
         */
        @ExceptionHandler(EmailAlreadyExistsException.class)
        public ResponseEntity<Map<String, Object>> handleConflict(
                        EmailAlreadyExistsException ex) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT));
        }

        /**
         * Handles TokenRefreshException.
         * Returns 401 when refresh token is missing or invalid.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 401 status.
         */
        @ExceptionHandler(TokenRefreshException.class)
        public ResponseEntity<Map<String, Object>> handleTokenRefresh(
                        TokenRefreshException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED));
        }

        /**
         * Handles all uncaught exceptions.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 500 status.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGeneric(
                        Exception ex) {

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ResponseBuilder.buildErrorResponse("Internal server error",
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        }

        /**
         * Handles InvalidCredentialsException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 400 status.
         */
        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
                        InvalidCredentialsException ex) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        /**
         * Handles DuplicateResourceException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 409 status.
         */
        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<Map<String, Object>> handleDuplicateResource(
                        DuplicateResourceException ex) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT));
        }

        /**
         * Handles IllegalArgumentException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 400 status.
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgument(
                        IllegalArgumentException ex) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        /**
         * Handles IllegalStateException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 409 status.
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalState(
                        IllegalStateException ex) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT));
        }

        /**
         * Handles InsufficientFundsException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 400 status.
         */
        @ExceptionHandler(InsufficientFundsException.class)
        public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
                        InsufficientFundsException ex) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        /**
         * Handles WalletNotFoundException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 404 status.
         */
        @ExceptionHandler(WalletNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleWalletNotFound(
                        WalletNotFoundException ex) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
        }

        /**
         * Handles BankAccountNotFoundException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 404 status.
         */
        @ExceptionHandler(BankAccountNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleBankAccountNotFound(
                        BankAccountNotFoundException ex) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
        }

        /**
         * Handles InvalidOtpException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 400 status.
         */
        @ExceptionHandler(InvalidOtpException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidOtp(
                        InvalidOtpException ex) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
        }

        /**
         * Handles InsufficientHoldingsException.
         *
         * @param ex The exception.
         * @return ResponseEntity with HTTP 400 status.
         */
        @ExceptionHandler(InsufficientHoldingsException.class)
        public ResponseEntity<Map<String, Object>> handleInsufficientHoldings(
                        InsufficientHoldingsException ex) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
        }
}