package com.techhub.service;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.ForgotPasswordRequest;
import com.techhub.model.dto.request.LoginRequest;
import com.techhub.model.dto.request.RegisterRequest;
import com.techhub.model.dto.request.ResendVerificationRequest;
import com.techhub.model.dto.request.ResetPasswordRequest;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.entity.EmailVerificationToken;
import com.techhub.model.entity.PasswordResetToken;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

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
        sampleUser.setRole(Role.BUYER);
        sampleUser.setEnabled(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setLockoutEndTime(null);
    }

    @Test
    @DisplayName("Đăng ký thành công - Lưu User và gửi mã OTP xác thực qua Email")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "new@techhub.com", "password123");

        when(userRepository.existsByEmail("new@techhub.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("123456");
        when(verificationTokenService.create(any(User.class))).thenReturn(token);

        assertDoesNotThrow(() -> authService.register(request));

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail("new@techhub.com", "123456");
    }

    @Test
    @DisplayName("Đăng ký thất bại - Email đã tồn tại thì ném lỗi EMAIL_IS_EXISTED")
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "test@techhub.com", "password123");

        when(userRepository.existsByEmail("test@techhub.com")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_IS_EXISTED, exception.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
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
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        LoginRequest request = new LoginRequest("test@techhub.com", "password123");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Reset mật khẩu thành công - Cập nhật pass mới và tự động mở khóa tài khoản")
    void resetPassword_Success_UnlocksAccount() {
        sampleUser.setFailedLoginAttempts(5);
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("654321");
        when(passwordResetTokenService.validate(sampleUser, "654321")).thenReturn(resetToken);
        when(passwordEncoder.matches("newPass123", sampleUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        ResetPasswordRequest request = new ResetPasswordRequest("test@techhub.com", "654321", "newPass123");

        assertDoesNotThrow(() -> authService.resetPassword(request));

        assertEquals("encodedNewPass", sampleUser.getPassword());
        assertEquals(0, sampleUser.getFailedLoginAttempts());
        assertNull(sampleUser.getLockoutEndTime());

        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenService, times(1)).revokeAll(sampleUser);
        verify(passwordResetTokenService, times(1)).delete(resetToken);
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void forgotPassword_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(passwordResetTokenService, never()).create(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Quên mật khẩu thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void forgotPassword_AccountNotVerified_ThrowsException() {
        sampleUser.setEnabled(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.forgotPassword(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(passwordResetTokenService, never()).create(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Quên mật khẩu thành công - Tạo token và gửi email")
    void forgotPassword_Success() {
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("123456");
        when(passwordResetTokenService.create(sampleUser)).thenReturn(resetToken);

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@techhub.com");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(passwordResetTokenService, times(1)).create(sampleUser);
        verify(emailService, times(1)).sendPasswordResetEmail("test@techhub.com", "123456");
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Email không tồn tại thì ném lỗi USER_NOT_FOUND")
    void resendVerificationEmail_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@techhub.com")).thenReturn(Optional.empty());

        ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(verificationTokenService, never()).create(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thất bại - Tài khoản đã kích hoạt rồi thì ném lỗi EMAIL_ALREADY_VERIFIED")
    void resendVerificationEmail_AlreadyVerified_ThrowsException() {
        sampleUser.setEnabled(true);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.resendVerificationEmail(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_VERIFIED, exception.getErrorCode());
        verify(verificationTokenService, never()).create(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Gửi lại email xác thực thành công - Tạo token mới và gửi email")
    void resendVerificationEmail_Success() {
        sampleUser.setEnabled(false);
        when(userRepository.findByEmail("test@techhub.com")).thenReturn(Optional.of(sampleUser));
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("654321");
        when(verificationTokenService.create(sampleUser)).thenReturn(token);

        ResendVerificationRequest request = new ResendVerificationRequest("test@techhub.com");

        assertDoesNotThrow(() -> authService.resendVerificationEmail(request));
        verify(verificationTokenService, times(1)).create(sampleUser);
        verify(emailService, times(1)).sendVerificationEmail("test@techhub.com", "654321");
    }
}
