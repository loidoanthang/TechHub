package com.techhub.service;

import com.techhub.config.properties.TokenProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.VerificationTokenType;
import com.techhub.repository.VerificationTokensRepository;
import com.techhub.service.impl.VerificationTokensServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokensServiceImplTest {

    @Mock
    private VerificationTokensRepository repository;

    @Mock
    private TokenProperties tokenProperties;

    @InjectMocks
    private VerificationTokensServiceImpl verificationTokensService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@techhub.com");

        lenient().when(tokenProperties.getVerificationExpiration()).thenReturn(86400000L); // 24h
        lenient().when(tokenProperties.getPasswordResetExpiration()).thenReturn(1800000L); // 30m
    }

    @Test
    @DisplayName("Create EMAIL_VERIFICATION token - Khi chưa có token thì tạo mới")
    void create_EmailVerification_NewToken() {
        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(repository.save(any(VerificationTokens.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationTokens token = verificationTokensService.create(sampleUser, VerificationTokenType.EMAIL_VERIFICATION);

        assertNotNull(token);
        assertEquals(sampleUser, token.getUser());
        assertEquals(VerificationTokenType.EMAIL_VERIFICATION, token.getType());
        assertEquals(6, token.getToken().length());
        assertEquals(0, token.getFailedAttempts());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(repository, times(1)).save(token);
    }

    @Test
    @DisplayName("Create PASSWORD_RESET token - Khi đã có token thì cập nhật và reset failedAttempts")
    void create_PasswordReset_ExistingToken() {
        VerificationTokens existing = new VerificationTokens();
        existing.setUser(sampleUser);
        existing.setType(VerificationTokenType.PASSWORD_RESET);
        existing.setToken("111111");
        existing.setFailedAttempts(3);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(VerificationTokens.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationTokens token = verificationTokensService.create(sampleUser, VerificationTokenType.PASSWORD_RESET);

        assertSame(existing, token);
        assertNotEquals("111111", token.getToken());
        assertEquals(0, token.getFailedAttempts());
        assertEquals(VerificationTokenType.PASSWORD_RESET, token.getType());
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Validate thành công - Trả về token đúng")
    void validate_Success() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.EMAIL_VERIFICATION);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setFailedAttempts(0);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

        VerificationTokens result = verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.EMAIL_VERIFICATION);

        assertSame(token, result);
    }

    @Test
    @DisplayName("Validate thất bại - Token không tồn tại cho EMAIL_VERIFICATION ném INVALID_VERIFICATION_TOKEN")
    void validate_NotFound_EmailVerification_ThrowsException() {
        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.EMAIL_VERIFICATION));

        assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate thất bại - Token không tồn tại cho PASSWORD_RESET ném PASSWORD_RESET_TOKEN_NOT_FOUND")
    void validate_NotFound_PasswordReset_ThrowsException() {
        when(repository.findByUserAndType(sampleUser, VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.PASSWORD_RESET));

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate thất bại - Token hết hạn ném TOKEN_EXPIRED và không xóa token khỏi DB")
    void validate_Expired_DoesNotDeleteToken_AndThrows() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.EMAIL_VERIFICATION);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.EMAIL_VERIFICATION));

        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
        verify(repository, never()).delete(any(VerificationTokens.class));
    }

    @Test
    @DisplayName("Validate thành công - Mã OTP có khoảng trắng thừa được tự động trim")
    void validate_Success_TokenWithWhitespace_TrimsAndMatches() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.EMAIL_VERIFICATION);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setFailedAttempts(0);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

        VerificationTokens result = verificationTokensService.validate(sampleUser, "  123456  ", VerificationTokenType.EMAIL_VERIFICATION);

        assertSame(token, result);
        verify(repository, never()).save(token);
    }

    @Test
    @DisplayName("Validate thất bại - Đã vượt quá số lần thử tối đa (>= 5)")
    void validate_TooManyFailedAttempts_Initially() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.PASSWORD_RESET);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setFailedAttempts(5);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "123456", VerificationTokenType.PASSWORD_RESET));

        assertEquals(ErrorCode.TOO_MANY_FAILED_ATTEMPTS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Validate thất bại - Mã OTP không đúng tăng failedAttempts")
    void validate_WrongOtp_IncrementsFailedAttempts() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.EMAIL_VERIFICATION);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setFailedAttempts(0);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "999999", VerificationTokenType.EMAIL_VERIFICATION));

        assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, ex.getErrorCode());
        assertEquals(1, token.getFailedAttempts());
        verify(repository, times(1)).save(token);
    }

    @Test
    @DisplayName("Validate thất bại - Mã OTP không đúng lần thứ 5 ném TOO_MANY_FAILED_ATTEMPTS")
    void validate_WrongOtp_ReachingFifthAttempt_ThrowsTooManyFailed() {
        VerificationTokens token = new VerificationTokens();
        token.setUser(sampleUser);
        token.setType(VerificationTokenType.PASSWORD_RESET);
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setFailedAttempts(4);

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.validate(sampleUser, "999999", VerificationTokenType.PASSWORD_RESET));

        assertEquals(ErrorCode.TOO_MANY_FAILED_ATTEMPTS, ex.getErrorCode());
        assertEquals(5, token.getFailedAttempts());
        verify(repository, times(1)).save(token);
    }

    @Test
    @DisplayName("Create token thất bại - Khi gọi lại trong vòng 60 giây thì ném OTP_RESEND_COOLDOWN")
    void create_WithinCooldown_ThrowsOtpResendCooldown() {
        VerificationTokens existing = new VerificationTokens();
        existing.setUser(sampleUser);
        existing.setType(VerificationTokenType.EMAIL_VERIFICATION);
        existing.setUpdatedAt(LocalDateTime.now().minusSeconds(30));

        when(repository.findByUserAndType(sampleUser, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                verificationTokensService.create(sampleUser, VerificationTokenType.EMAIL_VERIFICATION));

        assertEquals(ErrorCode.OTP_RESEND_COOLDOWN, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Delete token - Xóa token khỏi repository")
    void delete_Success() {
        VerificationTokens token = new VerificationTokens();
        verificationTokensService.delete(token);
        verify(repository, times(1)).delete(token);
    }
}
