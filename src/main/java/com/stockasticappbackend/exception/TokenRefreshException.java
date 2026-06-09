package com.stockasticappbackend.exception;

import static com.stockasticappbackend.util.Constants.TOKEN_REFRESH_ERROR_TEMPLATE;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenRefreshException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TokenRefreshException(String token, String message) {
        super(String.format(TOKEN_REFRESH_ERROR_TEMPLATE, token, message));
    }
}
