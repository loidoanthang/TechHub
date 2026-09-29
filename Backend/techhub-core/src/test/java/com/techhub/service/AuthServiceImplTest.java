package com.techhub.service;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.ChangePasswordRequest;
import com.techhub.model.dto.request.DeleteAccountRequest;
import com.techhub.model.dto.request.ForgotPasswordRequest;
import com.techhub.model.dto.request.LoginRequest;
import com.techhub.model.dto.request.LogoutRequest;
import com.techhub.model.dto.request.RefreshTokenRequest;
import com.techhub.model.dto.request.RegisterRequest;
import com.techhub.model.dto.request.ResendVerificationRequest;
import com.techhub.model.dto.request.ResetPasswordRequest;
import com.techhub.model.dto.request.VerifyEmailRequest;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.dto.response.TokenResponse;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.model.enums.VerificationTokenType;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokensService verificationTokensService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@techhub.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        sampleUser.setEmailVerified(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setLockoutEndTime(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContextUser(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Đăng ký thành công - Lưu User và gửi mã OTP xác thực qua Email")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "new@techhub.com", "password123");

        when(userRepository.findByEmail("new@techhub.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        VerificationTokens token = new VerificationTokens();
        token.setToken("123456");
        when(verificationTokensService.create(any(User.class), eq(VerificationTokenType.EMAIL_VERIFICATION))).thenReturn(token);

        assertDoesNotThrow(() -> authService.register(request));

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail("new@techhub.com", "123456");
    }

    @Test
    @DisplayName("Đăng ký thất bại - Email đã tồn tại và đã xác thực thì ném lỗi EMAIL_IS_EXISTED")
    void register_EmailAlreadyExistsAndVerified_ThrowsException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "test@techhub.com", "password123");

        sampleUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_IS_EXISTED, exception.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Đăng ký thất bại - Email đã tồn tại nhưng chưa xác thực thì ném lỗi EMAIL_EXISTS_UNVERIFIED")
    void register_EmailAlreadyExistsUnverified_ThrowsException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "test@techhub.com", "password123");

        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_EXISTS_UNVERIFIED, exception.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Xác thực email thành công - Kích hoạt tài khoản và xóa token")
    void verifyEmail_Success() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerificationTokens token = new VerificationTokens();
        token.setToken("123456");
        when(verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.EMAIL_VERIFICATION)).thenReturn(token);

        VerifyEmailRequest request = new VerifyEmailRequest("test@techhub.com", "123456");
        assertDoesNotThrow(() -> authService.verifyEmail(request));

        assertTrue(sampleUser.isEmailVerified());
        verify(userRepository, times(1)).save(sampleUser);
        verify(verificationTokensService, times(1)).delete(token);
    }

    @Test
    @DisplayName("Xác thực email thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void verifyEmail_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        VerifyEmailRequest request = new VerifyEmailRequest("nonexistent@techhub.com", "123456");
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

        verify(verificationTokensService, never()).validate(any(), anyString(), any());
    }

    @Test
    @DisplayName("Xác thực email thất bại - Tài khoản đã xác thực rồi thì ném lỗi EMAIL_ALREADY_VERIFIED")
    void verifyEmail_AlreadyVerified_ThrowsException() {
        sampleUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerifyEmailRequest request = new VerifyEmailRequest("test@techhub.com", "123456");
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_VERIFIED, exception.getErrorCode());

        verify(verificationTokensService, never()).validate(any(), anyString(), any());
    }

    @Test
    @DisplayName("Đăng nhập thành công - Trả về Access Token và Refresh Token")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@techhub.com", "correctPassword");

        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        when(jwtService.generateAccessToken(sampleUser)).thenReturn("mockAccessToken");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("mockRefreshTokenUuid");
        when(refreshTokenService.create(sampleUser)).thenReturn(mockRefreshToken);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockAccessToken", response.accessToken());
        assertEquals("mockRefreshTokenUuid", response.refreshToken());
    }

    @Test
    @DisplayName("Đăng nhập thất bại - Tài khoản đang bị khóa thì chặn ngay lập tức")
    void login_AccountLocked_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("Account is locked"));

        LoginRequest request = new LoginRequest("test@techhub.com", "anyPassword");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập sai 5 lần liên tiếp - Khóa tài khoản 15 phút")
    void login_FiveFailedAttempts_LocksAccount() {
        sampleUser.setFailedLoginAttempts(4); // đã sai 4 lần trước đó
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("test@techhub.com", "wrongPassword");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());

        assertNotNull(sampleUser.getLockoutEndTime());
        assertTrue(sampleUser.getLockoutEndTime().isAfter(LocalDateTime.now()));
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Đăng nhập thất bại - Tài khoản chưa kích hoạt email thì báo ACCOUNT_NOT_VERIFIED")
    void login_DisabledAccount_ThrowsAccountNotVerified() {
        sampleUser.setEmailVerified(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        LoginRequest request = new LoginRequest("test@techhub.com", "password123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại - Tài khoản đã bị xóa thì báo ACCOUNT_DELETED")
    void login_DeletedAccount_ThrowsAccountDeleted() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        LoginRequest request = new LoginRequest("test@techhub.com", "password123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại - Tài khoản bị cấm thì báo ACCOUNT_BANNED")
    void login_BannedAccount_ThrowsAccountBanned() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User is locked"));
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        LoginRequest request = new LoginRequest("test@techhub.com", "password123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Reset mật khẩu thành công - Cập nhật pass mới và tự động mở khóa tài khoản")
    void resetPassword_Success_UnlocksAccount() {
        sampleUser.setFailedLoginAttempts(5);
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerificationTokens resetToken = new VerificationTokens();
        resetToken.setToken("654321");
        when(verificationTokensService.validate(sampleUser, "654321", VerificationTokenType.PASSWORD_RESET)).thenReturn(resetToken);
        when(passwordEncoder.matches("newPass123", sampleUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "654321", "newPass123");

        assertDoesNotThrow(() -> authService.resetPassword(request));

        assertEquals("encodedNewPass", sampleUser.getPassword());
        assertEquals(0, sampleUser.getFailedLoginAttempts());
        assertNull(sampleUser.getLockoutEndTime());

        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenService, times(1)).revokeAll(sampleUser);
        verify(verificationTokensService, times(1)).delete(resetToken);
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void resetPassword_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest("nonexistent@techhub.com", "123456", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Tài khoản chưa xác thực email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void resetPassword_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Tài khoản bị cấm thì ném lỗi ACCOUNT_BANNED")
    void resetPassword_AccountBanned_ThrowsAccountBanned() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void resetPassword_AccountSuspended_ThrowsAccountSuspended() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Mật khẩu mới trùng với mật khẩu hiện tại thì ném lỗi PASSWORD_SAME_AS_OLD")
    void resetPassword_PasswordSameAsOld_ThrowsException() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        VerificationTokens resetToken = new VerificationTokens();
        resetToken.setToken("123456");
        when(verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.PASSWORD_RESET)).thenReturn(resetToken);
        when(passwordEncoder.matches("samePassword123", sampleUser.getPassword())).thenReturn(true);

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "samePassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.PASSWORD_SAME_AS_OLD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
        verify(verificationTokensService, never()).delete(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Token OTP không hợp lệ thì ném lỗi INVALID_PASSWORD_RESET_TOKEN")
    void resetPassword_InvalidToken_ThrowsException() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        when(verificationTokensService.validate(sampleUser, "999999", VerificationTokenType.PASSWORD_RESET))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "999999", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.INVALID_PASSWORD_RESET_TOKEN, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
        verify(verificationTokensService, never()).delete(any());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Token OTP hết hạn thì ném lỗi TOKEN_EXPIRED")
    void resetPassword_ExpiredToken_ThrowsException() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        when(verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.PASSWORD_RESET))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "newPass123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
        verify(verificationTokensService, never()).delete(any());
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void forgotPassword_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void forgotPassword_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Quên mật khẩu thành công - Tạo token và gửi email")
    void forgotPassword_Success() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        VerificationTokens resetToken = new VerificationTokens();
        resetToken.setToken("123456");
        when(verificationTokensService.create(sampleUser, VerificationTokenType.PASSWORD_RESET)).thenReturn(resetToken);

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(verificationTokensService, times(1)).create(sampleUser, VerificationTokenType.PASSWORD_RESET);
        verify(emailService, times(1)).sendPasswordResetEmail("test@techhub.com", "123456");
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void resendVerificationEmail_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Tài khoản đã kích hoạt rồi thì ném lỗi EMAIL_ALREADY_VERIFIED")
    void resendVerificationEmail_AlreadyVerified_ThrowsException() {
        sampleUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_VERIFIED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thành công - Tạo token mới và gửi email")
    void resendVerificationEmail_Success() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        VerificationTokens token = new VerificationTokens();
        token.setToken("654321");
        when(verificationTokensService.create(sampleUser, VerificationTokenType.EMAIL_VERIFICATION)).thenReturn(token);

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        assertDoesNotThrow(() -> authService.resendVerificationEmail(request));
        verify(verificationTokensService, times(1)).create(sampleUser, VerificationTokenType.EMAIL_VERIFICATION);
        verify(emailService, times(1)).sendVerificationEmail("test@techhub.com", "654321");
    }

    @Test
    @DisplayName("Refresh token thành công - Cấp Access Token mới và xoay vòng Refresh Token")
    void refreshToken_Success() {
        RefreshToken mockOldToken = new RefreshToken();
        mockOldToken.setUser(sampleUser);
        when(refreshTokenService.validate("validToken")).thenReturn(mockOldToken);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("newAccessToken");

        RefreshToken mockNewToken = new RefreshToken();
        mockNewToken.setToken("newRefreshTokenUuid");
        when(refreshTokenService.create(sampleUser)).thenReturn(mockNewToken);

        RefreshTokenRequest request = new RefreshTokenRequest("validToken");
        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertEquals("newRefreshTokenUuid", response.refreshToken());
        verify(refreshTokenService, times(1)).revoke(mockOldToken);
        verify(refreshTokenService, times(1)).create(sampleUser);
    }

    @Test
    @DisplayName("Refresh token thành công với token có khoảng trắng - Tự động trim token")
    void refreshToken_WithWhitespaceToken_TrimsAndSucceeds() {
        RefreshToken mockOldToken = new RefreshToken();
        mockOldToken.setUser(sampleUser);
        when(refreshTokenService.validate("validToken")).thenReturn(mockOldToken);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("newAccessToken");

        RefreshToken mockNewToken = new RefreshToken();
        mockNewToken.setToken("newRefreshTokenUuid");
        when(refreshTokenService.create(sampleUser)).thenReturn(mockNewToken);

        RefreshTokenRequest request = new RefreshTokenRequest("   validToken   ");
        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        verify(refreshTokenService, times(1)).validate("validToken");
    }

    @Test
    @DisplayName("Refresh token thất bại - Token không tồn tại thì ném lỗi REFRESH_TOKEN_NOT_FOUND")
    void refreshToken_TokenNotFound_ThrowsException() {
        when(refreshTokenService.validate("nonExistentToken"))
                .thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        RefreshTokenRequest request = new RefreshTokenRequest("nonExistentToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Token đã bị thu hồi (Reuse Detection) thì ném lỗi REFRESH_TOKEN_REVOKED")
    void refreshToken_RevokedToken_ThrowsException() {
        when(refreshTokenService.validate("revokedToken"))
                .thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED));

        RefreshTokenRequest request = new RefreshTokenRequest("revokedToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_REVOKED, exception.getErrorCode());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Token đã hết hạn thì ném lỗi REFRESH_TOKEN_EXPIRED")
    void refreshToken_ExpiredToken_ThrowsException() {
        when(refreshTokenService.validate("expiredToken"))
                .thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Tài khoản đang bị khóa 15m thì ném lỗi ACCOUNT_LOCKED")
    void refreshToken_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setUser(sampleUser);
        when(refreshTokenService.validate("mockToken")).thenReturn(mockRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("mockToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void refreshToken_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setUser(sampleUser);
        when(refreshTokenService.validate("mockToken")).thenReturn(mockRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("mockToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Tài khoản bị cấm thì ném lỗi ACCOUNT_BANNED")
    void refreshToken_BannedAccount_ThrowsAccountBanned() {
        sampleUser.setStatus(UserStatus.BANNED);
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setUser(sampleUser);
        when(refreshTokenService.validate("mockToken")).thenReturn(mockRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("mockToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void refreshToken_SuspendedAccount_ThrowsAccountSuspended() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setUser(sampleUser);
        when(refreshTokenService.validate("mockToken")).thenReturn(mockRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("mockToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("Refresh token thất bại - Tài khoản đã bị xóa thì ném lỗi ACCOUNT_DELETED")
    void refreshToken_DeletedAccount_ThrowsAccountDeleted() {
        sampleUser.setStatus(UserStatus.DELETED);
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setUser(sampleUser);
        when(refreshTokenService.validate("mockToken")).thenReturn(mockRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("mockToken");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("Đăng xuất thành công - Thu hồi Refresh Token theo token truyền lên")
    void logout_Success() {
        LogoutRequest request = new LogoutRequest("sampleRefreshToken");

        assertDoesNotThrow(() -> authService.logout(request));
        verify(refreshTokenService, times(1)).revokeByToken("sampleRefreshToken");
    }

    @Test
    @DisplayName("Đăng xuất thành công với token có khoảng trắng - Tự động trim token")
    void logout_WithWhitespaceToken_TrimsAndSucceeds() {
        LogoutRequest request = new LogoutRequest("   sampleRefreshToken   ");

        assertDoesNotThrow(() -> authService.logout(request));
        verify(refreshTokenService, times(1)).revokeByToken("sampleRefreshToken");
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Tài khoản bị cấm thì ném lỗi ACCOUNT_BANNED")
    void forgotPassword_BannedAccount_ThrowsAccountBanned() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void forgotPassword_SuspendedAccount_ThrowsAccountSuspended() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Tài khoản bị xóa thì ném lỗi ACCOUNT_DELETED")
    void forgotPassword_DeletedAccount_ThrowsAccountDeleted() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Quên mật khẩu - Tài khoản đang bị khóa tạm 15m (ACCOUNT_LOCKED) vẫn được phép gửi mã reset (Chuẩn BR-24/26)")
    void forgotPassword_AccountLocked_AllowsReset() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        sampleUser.setFailedLoginAttempts(5);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerificationTokens resetToken = new VerificationTokens();
        resetToken.setToken("123456");
        when(verificationTokensService.create(sampleUser, VerificationTokenType.PASSWORD_RESET)).thenReturn(resetToken);

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(verificationTokensService, times(1)).create(sampleUser, VerificationTokenType.PASSWORD_RESET);
        verify(emailService, times(1)).sendPasswordResetEmail("test@techhub.com", "123456");
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Bị chặn Cooldown 60s giữa 2 lần xin mã")
    void forgotPassword_Cooldown_ThrowsException() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        when(verificationTokensService.create(sampleUser, VerificationTokenType.PASSWORD_RESET))
                .thenThrow(new BusinessException(ErrorCode.OTP_RESEND_COOLDOWN));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.OTP_RESEND_COOLDOWN, exception.getErrorCode());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - Tài khoản bị xóa thì ném lỗi ACCOUNT_DELETED")
    void resetPassword_DeletedAccount_ThrowsAccountDeleted() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "123456", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
    }

    @Test
    @DisplayName("Xác thực email thất bại - Tài khoản bị cấm thì ném lỗi ACCOUNT_BANNED")
    void verifyEmail_BannedAccount_ThrowsAccountBanned() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerifyEmailRequest request = new VerifyEmailRequest("test@techhub.com", "123456");
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Tài khoản bị xóa thì ném lỗi ACCOUNT_DELETED")
    void resendVerificationEmail_DeletedAccount_ThrowsAccountDeleted() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Xác thực email thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void verifyEmail_SuspendedAccount_ThrowsAccountSuspended() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        VerifyEmailRequest request = new VerifyEmailRequest("test@techhub.com", "123456");
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(verificationTokensService, never()).validate(any(), anyString(), any());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void resendVerificationEmail_SuspendedAccount_ThrowsAccountSuspended() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(verificationTokensService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công - Băm BCrypt mật khẩu mới, lưu DB và thu hồi toàn bộ Refresh Tokens")
    void changePassword_Success() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("currentPassword123", sampleUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", sampleUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        assertDoesNotThrow(() -> authService.changePassword(request));

        assertEquals("encodedNewPassword", sampleUser.getPassword());
        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenService, times(1)).revokeAll(sampleUser);
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Mật khẩu hiện tại không chính xác thì ném lỗi WRONG_CURRENT_PASSWORD")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongCurrentPass", sampleUser.getPassword())).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrongCurrentPass", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.WRONG_CURRENT_PASSWORD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Mật khẩu mới trùng với mật khẩu cũ thì ném lỗi PASSWORD_SAME_AS_OLD")
    void changePassword_PasswordSameAsOld_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("currentPassword123", sampleUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("currentPassword123", sampleUser.getPassword())).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "currentPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.PASSWORD_SAME_AS_OLD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Không tìm thấy User trong DB thì ném lỗi USER_NOT_FOUND")
    void changePassword_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Tài khoản đang bị tạm khóa thì ném lỗi ACCOUNT_LOCKED")
    void changePassword_AccountLocked_ThrowsException() {
        sampleUser.setFailedLoginAttempts(5);
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void changePassword_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Tài khoản bị cấm thì ném lỗi ACCOUNT_BANNED")
    void changePassword_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Tài khoản bị tạm ngưng thì ném lỗi ACCOUNT_SUSPENDED")
    void changePassword_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại - Tài khoản bị xóa thì ném lỗi ACCOUNT_DELETED")
    void changePassword_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword123", "newPassword123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thành công - Xác nhận mật khẩu đúng, chuyển status DELETED, lưu deletedAt và revoke toàn bộ Refresh Tokens")
    void deleteAccount_Success() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", sampleUser.getPassword())).thenReturn(true);

        DeleteAccountRequest request = new DeleteAccountRequest("password123", "Tôi không còn nhu cầu sử dụng");

        assertDoesNotThrow(() -> authService.deleteAccount(request));

        assertEquals(UserStatus.DELETED, sampleUser.getStatus());
        assertNotNull(sampleUser.getDeletedAt());
        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenService, times(1)).revokeAll(sampleUser);
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Mật khẩu xác nhận không chính xác thì ném lỗi WRONG_CURRENT_PASSWORD")
    void deleteAccount_WrongPassword_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPassword())).thenReturn(false);

        DeleteAccountRequest request = new DeleteAccountRequest("wrongPassword", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.WRONG_CURRENT_PASSWORD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Không tìm thấy User trong DB thì ném lỗi USER_NOT_FOUND")
    void deleteAccount_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void deleteAccount_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Tài khoản đang bị tạm khóa 15m thì ném lỗi ACCOUNT_LOCKED")
    void deleteAccount_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Tài khoản đang bị cấm thì ném lỗi ACCOUNT_BANNED")
    void deleteAccount_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Tài khoản đang bị đình chỉ thì ném lỗi ACCOUNT_SUSPENDED")
    void deleteAccount_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Xóa tài khoản thất bại - Tài khoản đã bị xóa trước đó thì ném lỗi ACCOUNT_DELETED")
    void deleteAccount_AlreadyDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        DeleteAccountRequest request = new DeleteAccountRequest("password123", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.deleteAccount(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }
}
