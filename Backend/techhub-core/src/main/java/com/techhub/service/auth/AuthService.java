package com.techhub.service.auth;

import com.techhub.modules.auth.dto.response.LoginResponse;
import com.techhub.modules.auth.dto.response.TokenResponse;
import com.techhub.modules.auth.dto.request.ChangePasswordRequest;
import com.techhub.modules.auth.dto.request.DeleteAccountRequest;
import com.techhub.modules.auth.dto.request.ForgotPasswordRequest;
import com.techhub.modules.auth.dto.request.LoginRequest;
import com.techhub.modules.auth.dto.request.LogoutRequest;
import com.techhub.modules.auth.dto.request.RefreshTokenRequest;
import com.techhub.modules.auth.dto.request.RegisterRequest;
import com.techhub.modules.auth.dto.request.ResendVerificationRequest;
import com.techhub.modules.auth.dto.request.ResetPasswordRequest;
import com.techhub.modules.auth.dto.request.VerifyEmailRequest;

public interface AuthService {

    void register(RegisterRequest request);

    void verifyEmail(VerifyEmailRequest request);

    LoginResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);

    void resendVerificationEmail(ResendVerificationRequest request);

    void deleteAccount(DeleteAccountRequest request);
}