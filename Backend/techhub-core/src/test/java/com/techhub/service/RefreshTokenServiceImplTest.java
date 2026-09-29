package com.techhub.service;

import com.techhub.config.properties.JwtProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.repository.RefreshTokenRepository;
import com.techhub.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User sampleUser;
    private RefreshToken sampleToken;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@techhub.com");

        sampleToken = new RefreshToken();
        sampleToken.setId(UUID.randomUUID());
        sampleToken.setToken("sample-uuid-token");
        sampleToken.setUser(sampleUser);
        sampleToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        sampleToken.setRevoked(false);
    }

    @Test
    @DisplayName("Create - Tạo Refresh Token mới thành công và lưu DB")
    void create_Success() {
        when(jwtProperties.getRefreshExpiration()).thenReturn(604800000L); // 7 days in ms
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken createdToken = refreshTokenService.create(sampleUser);

        assertNotNull(createdToken);
        assertNotNull(createdToken.getToken());
        assertEquals(sampleUser, createdToken.getUser());
        assertFalse(createdToken.isRevoked());
        assertTrue(createdToken.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(repository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("GetByToken - Tìm thấy token hợp lệ")
    void getByToken_Success() {
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        RefreshToken result = refreshTokenService.getByToken("sample-uuid-token");

        assertNotNull(result);
        assertEquals("sample-uuid-token", result.getToken());
    }

    @Test
    @DisplayName("GetByToken - Tự động trim khoảng trắng trước khi query DB")
    void getByToken_TrimsWhitespace() {
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        RefreshToken result = refreshTokenService.getByToken("   sample-uuid-token   ");

        assertNotNull(result);
        verify(repository, times(1)).findByToken("sample-uuid-token");
    }

    @Test
    @DisplayName("GetByToken - Không tìm thấy token thì ném lỗi REFRESH_TOKEN_NOT_FOUND")
    void getByToken_NotFound_ThrowsException() {
        when(repository.findByToken("unknown-token")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> refreshTokenService.getByToken("unknown-token"));
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Validate - Token hợp lệ, chưa hết hạn, chưa revoked")
    void validate_Success() {
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        RefreshToken result = refreshTokenService.validate("sample-uuid-token");

        assertNotNull(result);
        assertEquals(sampleToken, result);
    }

    @Test
    @DisplayName("Validate - Token đã bị revoked (Reuse Detection) -> Thu hồi tất cả token của User và ném lỗi REFRESH_TOKEN_REVOKED")
    void validate_Revoked_RevokesAllAndThrowsException() {
        sampleToken.setRevoked(true);
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        RefreshToken token1 = new RefreshToken();
        token1.setRevoked(false);
        RefreshToken token2 = new RefreshToken();
        token2.setRevoked(false);
        when(repository.findAllByUser(sampleUser)).thenReturn(List.of(token1, token2));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> refreshTokenService.validate("sample-uuid-token"));
        assertEquals(ErrorCode.REFRESH_TOKEN_REVOKED, exception.getErrorCode());

        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());
        verify(repository, times(1)).saveAll(List.of(token1, token2));
    }

    @Test
    @DisplayName("Validate - Token đã hết hạn -> Ném lỗi REFRESH_TOKEN_EXPIRED và KHÔNG xóa khỏi repository")
    void validate_Expired_ThrowsExceptionWithoutDeleting() {
        sampleToken.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> refreshTokenService.validate("sample-uuid-token"));
        assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Revoke - Đánh dấu token là revoked và lưu DB")
    void revoke_Success() {
        refreshTokenService.revoke(sampleToken);

        assertTrue(sampleToken.isRevoked());
        verify(repository, times(1)).save(sampleToken);
    }

    @Test
    @DisplayName("RevokeAll - Đánh dấu toàn bộ token của User là revoked")
    void revokeAll_Success() {
        RefreshToken token1 = new RefreshToken();
        token1.setRevoked(false);
        RefreshToken token2 = new RefreshToken();
        token2.setRevoked(false);
        when(repository.findAllByUser(sampleUser)).thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAll(sampleUser);

        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());
        verify(repository, times(1)).saveAll(List.of(token1, token2));
    }

    @Test
    @DisplayName("RevokeByToken - Tìm token và đánh dấu revoked")
    void revokeByToken_Success() {
        when(repository.findByToken("sample-uuid-token")).thenReturn(Optional.of(sampleToken));

        refreshTokenService.revokeByToken("   sample-uuid-token   ");

        assertTrue(sampleToken.isRevoked());
        verify(repository, times(1)).save(sampleToken);
    }

    @Test
    @DisplayName("RevokeByToken - Token không tồn tại thì không làm gì")
    void revokeByToken_NotFound_DoesNothing() {
        when(repository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revokeByToken("unknown-token"));
        verify(repository, never()).save(any());
    }
}
