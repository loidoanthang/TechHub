package com.techhub.controller;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.*;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.dto.response.TokenResponse;
import com.techhub.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null, "Registration successful. Please check your email for verification instructions.", null);
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String token
    ) {
        authService.verifyEmail(email, token);
        return ApiResponse.success(null, "Email verified successfully.", null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful.", null);
    }


    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request), "Token refreshed successfully.", null);

    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success(null, "Logout successful.", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(null, "Password reset instructions sent to your email.", null);

    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password reset successfully.", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success(null, "Password changed successfully.", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request);
        return ApiResponse.success(null, "Verification email resent successfully.", null);
    }
}
