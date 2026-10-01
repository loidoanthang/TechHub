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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Google ID token"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    TOO_MANY_FAILED_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please request a new code"),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "Invalid password reset token"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is temporarily locked due to multiple failed login attempts. Please try again in 15 minutes"),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Account is not verified. Please verify your email first"),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "Account has been deleted"),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "Account has been banned"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Account has been suspended"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    EMAIL_EXISTS_UNVERIFIED(HttpStatus.BAD_REQUEST, "Email is already registered but not verified. Please verify your account"),
    OTP_RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "Please wait 60 seconds before requesting a new code"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Address not found"),
    CANNOT_UNSET_DEFAULT_ADDRESS(HttpStatus.BAD_REQUEST, "Cannot unset default address. Please set another address as default instead"),
    CANNOT_DELETE_DEFAULT_ADDRESS(HttpStatus.BAD_REQUEST, "Cannot delete default address. Please set another address as default first"),
    ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Address limit reached. Maximum 10 addresses allowed per user"),
    CANNOT_BAN_SELF(HttpStatus.BAD_REQUEST, "You cannot ban or suspend your own account"),
    CANNOT_MODIFY_ADMIN_USER(HttpStatus.FORBIDDEN, "Cannot ban or suspend another administrator"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid status transition"),
    SELLER_ALREADY_PENDING(HttpStatus.BAD_REQUEST, "Your seller registration is pending approval"),
    ALREADY_A_SELLER(HttpStatus.BAD_REQUEST, "User is already a registered seller"),
    SELLER_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Seller name / slug is already taken"),
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "Seller profile not found"),
    SHOP_NOT_FOUND(HttpStatus.NOT_FOUND, "Shop not found"),
    SHOP_UNAVAILABLE(HttpStatus.NOT_FOUND, "Shop is temporarily closed or suspended"),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Rejection reason is required when rejecting a seller"),
    INVALID_SHOP_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid shop status transition"),
    NOT_A_SELLER(HttpStatus.FORBIDDEN, "Access denied: Account is not a registered seller"),
    SHIPPING_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Shipping option not found");

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
