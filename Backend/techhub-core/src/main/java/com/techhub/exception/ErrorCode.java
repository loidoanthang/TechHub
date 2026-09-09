package com.techhub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    EMAIL_IS_EXISTED(HttpStatus.BAD_REQUEST, "Email is already exist"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "Email is already verified"),
    PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "New password cannot be the same as the old password"),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Token is expired"),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "Invalid verification token"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "Refresh token not found"),
    REFRESH_TOKEN_REVOKED(HttpStatus.BAD_REQUEST, "Refresh token is revoked"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Refresh token has expired"),
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "Password reset token not found"),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Password reset token has expired"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Google ID token"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    TOO_MANY_FAILED_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please request a new code"),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "Invalid password reset token"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is temporarily locked due to multiple failed login attempts. Please try again in 15 minutes"),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Account is not verified. Please verify your email first"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password");

    private final HttpStatus status;
    private final String message;

    ErrorCode(
            HttpStatus status,
            String message
    ) {
        this.status = status;
        this.message = message;
    }
}
