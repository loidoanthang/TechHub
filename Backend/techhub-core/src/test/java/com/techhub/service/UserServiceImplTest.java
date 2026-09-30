package com.techhub.service;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProfileRequest;
import com.techhub.model.dto.response.UserProfileResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserService - Use Case Manage Profile")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("buyer@techhub.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setFirstName("Thang");
        sampleUser.setLastName("Loi");
        sampleUser.setPhone("0987654321");
        sampleUser.setAvatarUrl("https://techhub.com/avatars/sample.png");
        sampleUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setLockoutEndTime(null);
        sampleUser.setCreatedAt(LocalDateTime.now().minusDays(1));
        sampleUser.setUpdatedAt(LocalDateTime.now());
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

    // ==================== GET PROFILE TESTS ====================

    @Test
    @DisplayName("Lấy thông tin cá nhân thành công - Trả về đầy đủ dữ liệu hồ sơ")
    void getProfile_Success() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userService.getProfile();

        assertNotNull(response);
        assertEquals(sampleUser.getId(), response.id());
        assertEquals(sampleUser.getFirstName(), response.firstName());
        assertEquals(sampleUser.getLastName(), response.lastName());
        assertEquals(sampleUser.getEmail(), response.email());
        assertEquals(sampleUser.getPhone(), response.phone());
        assertEquals(sampleUser.getAvatarUrl(), response.avatarUrl());
        assertEquals(sampleUser.getRoles(), response.roles());
        assertEquals(sampleUser.getStatus(), response.status());
        assertTrue(response.emailVerified());
        assertEquals(sampleUser.getCreatedAt(), response.createdAt());
        assertEquals(sampleUser.getUpdatedAt(), response.updatedAt());

        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - User ID trong JWT không tồn tại trong DB thì ném lỗi USER_NOT_FOUND")
    void getProfile_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Tài khoản bị cấm (BANNED) thì ném lỗi ACCOUNT_BANNED")
    void getProfile_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Tài khoản bị tạm ngưng (SUSPENDED) thì ném lỗi ACCOUNT_SUSPENDED")
    void getProfile_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Tài khoản đã bị xóa (DELETED) thì ném lỗi ACCOUNT_DELETED")
    void getProfile_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void getProfile_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Tài khoản đang bị khóa tạm thời 15m thì ném lỗi ACCOUNT_LOCKED")
    void getProfile_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
    }

    @Test
    @DisplayName("Lấy thông tin thất bại - Chưa xác thực / không có SecurityContext thì ném lỗi UNAUTHENTICATED")
    void getProfile_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile());
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    @DisplayName("Cập nhật thông tin thành công - Đầy đủ các trường và tự động trim khoảng trắng")
    void updateProfile_Success_WithAllFields() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "  Nguyen  ",
                "  Van A  ",
                "  0987654321  ",
                "  https://techhub.com/avatar.png  "
        );
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(request);

        assertNotNull(response);
        assertEquals("Nguyen", response.firstName());
        assertEquals("Van A", response.lastName());
        assertEquals("0987654321", response.phone());
        assertEquals("https://techhub.com/avatar.png", response.avatarUrl());

        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Cập nhật thông tin thành công - Không truyền avatarUrl thì giữ nguyên ảnh đại diện hiện tại")
    void updateProfile_Success_WithoutAvatarUrl_PreservesExistingAvatar() {
        sampleUser.setAvatarUrl("https://techhub.com/avatars/existing.png");
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(request);

        assertNotNull(response);
        assertEquals("Nguyen", response.firstName());
        assertEquals("Van A", response.lastName());
        assertEquals("0987654321", response.phone());
        assertEquals("https://techhub.com/avatars/existing.png", response.avatarUrl());

        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - User ID trong JWT không tồn tại trong DB thì ném lỗi USER_NOT_FOUND")
    void updateProfile_UserNotFound_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Tài khoản bị cấm (BANNED) thì ném lỗi ACCOUNT_BANNED")
    void updateProfile_AccountBanned_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Tài khoản bị tạm ngưng (SUSPENDED) thì ném lỗi ACCOUNT_SUSPENDED")
    void updateProfile_AccountSuspended_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Tài khoản đã bị xóa (DELETED) thì ném lỗi ACCOUNT_DELETED")
    void updateProfile_AccountDeleted_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        sampleUser.setStatus(UserStatus.DELETED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Tài khoản chưa kích hoạt email thì ném lỗi ACCOUNT_NOT_VERIFIED")
    void updateProfile_AccountNotVerified_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Tài khoản đang bị khóa tạm thời 15m thì ném lỗi ACCOUNT_LOCKED")
    void updateProfile_AccountLocked_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại - Chưa xác thực / không có SecurityContext thì ném lỗi UNAUTHENTICATED")
    void updateProfile_Unauthenticated_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen", "Van A", "0987654321", null);
        SecurityContextHolder.clearContext();

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateProfile(request));
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
}
