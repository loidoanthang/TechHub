package com.techhub.service;

import com.techhub.model.dto.request.*;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.dto.response.TokenResponse;

public interface AuthService {

    void register(RegisterRequest request);

    void verifyEmail(String email, String token);

    LoginResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);

    void resendVerificationEmail(ResendVerificationRequest request);
}