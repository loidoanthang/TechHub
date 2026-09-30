package com.techhub.service.admin;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateUserStatusRequest;
import com.techhub.model.dto.response.AdminUserResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.User;
import com.techhub.model.enums.AddressLabel;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.RefreshTokenService;
import com.techhub.service.admin.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminUserService (View Users & Ban/Suspend User)")
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User adminUser;
    private User user1;
    private User user2;
    private Address address1;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Root");
        adminUser.setEmail("admin@techhub.com");
        adminUser.setPhone("0900000000");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        adminUser.setCreatedAt(LocalDateTime.now().minusDays(30));

        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setFirstName("Thang");
        user1.setLastName("Loi Doan");
        user1.setEmail("thang@techhub.com");
        user1.setPhone("0987654321");
        user1.setStatus(UserStatus.ACTIVE);
        user1.setEmailVerified(true);
        user1.setAvatarUrl("https://techhub.com/avatars/user1.png");
        user1.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        user1.setCreatedAt(LocalDateTime.now().minusDays(5));

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setFirstName("Van A");
        user2.setLastName("Nguyen");
        user2.setEmail("vana@techhub.com");
        user2.setPhone("0912345678");
        user2.setStatus(UserStatus.SUSPENDED);
        user2.setEmailVerified(false);
        user2.setAvatarUrl(null);
        user2.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        user2.setCreatedAt(LocalDateTime.now().minusDays(2));

        address1 = Address.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .recipientName("Thang Loi")
                .phone("0987654321")
                .province("TP. Ho Chi Minh")
                .district("Quan 1")
                .ward("Phuong Ben Nghe")
                .streetAddress("123 Le Loi")
                .label(AddressLabel.HOME)
                .isDefault(true)
                .build();
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

    // ==================== GET USERS TESTS ====================

    @Test
    @DisplayName("Lấy danh sách người dùng thành công với tham số mặc định và map đúng địa chỉ mặc định")
    void getUsers_Success_DefaultParameters() {
        Page<User> mockPage = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(addressRepository.findDefaultAddressesByUserIds(anyCollection())).thenReturn(List.of(address1));

        PageResponse<AdminUserResponse> response = adminUserService.getUsers(
                null, null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertTrue(response.last());

        AdminUserResponse firstUser = response.content().get(0);
        assertEquals(user1.getId(), firstUser.id());
        assertEquals("Thang", firstUser.firstName());
        assertEquals("Loi Doan", firstUser.lastName());
        assertEquals("thang@techhub.com", firstUser.email());
        assertEquals("0987654321", firstUser.phone());
        assertTrue(firstUser.emailVerified());
        assertEquals(UserStatus.ACTIVE, firstUser.status());
        assertEquals("123 Le Loi, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh", firstUser.defaultAddress());
        assertEquals("https://techhub.com/avatars/user1.png", firstUser.avatarUrl());

        AdminUserResponse secondUser = response.content().get(1);
        assertEquals(user2.getId(), secondUser.id());
        assertEquals("Van A", secondUser.firstName());
        assertNull(secondUser.defaultAddress());
        assertNull(secondUser.avatarUrl());
        assertFalse(secondUser.emailVerified());
        assertEquals(UserStatus.SUSPENDED, secondUser.status());
    }

    @Test
    @DisplayName("Khi kết quả user rỗng -> Không gọi AddressRepository và trả về PageResponse rỗng")
    void getUsers_EmptyResult_DoesNotQueryAddresses() {
        Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminUserResponse> response = adminUserService.getUsers(
                "nonexistent", UserStatus.ACTIVE, true, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(addressRepository, never()).findDefaultAddressesByUserIds(anyCollection());
    }

    @Test
    @DisplayName("User không có địa chỉ mặc định -> defaultAddress trả về null")
    void getUsers_UserWithoutDefaultAddress_ReturnsNull() {
        Page<User> mockPage = new PageImpl<>(List.of(user2), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(addressRepository.findDefaultAddressesByUserIds(anyCollection())).thenReturn(List.of());

        PageResponse<AdminUserResponse> response = adminUserService.getUsers(
                null, null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertNull(response.content().get(0).defaultAddress());
    }

    @Test
    @DisplayName("Sắp xếp không hợp lệ -> Fallback an toàn về 'createdAt'")
    void getUsers_InvalidSortBy_FallbackToCreatedAt() {
        Page<User> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(mockPage);

        adminUserService.getUsers(null, null, null, 0, 20, "malicious_column_or_injection", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals("createdAt: DESC", capturedPageable.getSort().toString());
    }

    @Test
    @DisplayName("Sắp xếp theo trường hợp lệ trong Whitelist (email, firstName, status)")
    void getUsers_ValidSortBy_AppliedSuccessfully() {
        Page<User> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(mockPage);

        adminUserService.getUsers(null, null, null, 0, 20, "email", "asc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals("email: ASC", capturedPageable.getSort().toString());
    }

    @Test
    @DisplayName("Sanitize page và size khi truyền giá trị biên bất thường")
    void getUsers_SanitizePageAndSize() {
        Page<User> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(mockPage);

        adminUserService.getUsers(null, null, null, -5, 500, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(100, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("Sanitize size khi truyền giá trị <= 0 -> Điều chỉnh về 1")
    void getUsers_SanitizeSizeLessThanOne() {
        Page<User> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(mockPage);

        adminUserService.getUsers(null, null, null, 0, 0, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(1, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("Triệt tiêu N+1 Query: findDefaultAddressesByUserIds chỉ gọi đúng 1 lần duy nhất cho toàn bộ trang")
    void getUsers_ZeroNPlusOneVerification() {
        List<User> userList = List.of(user1, user2);
        Page<User> mockPage = new PageImpl<>(userList, PageRequest.of(0, 20), 2);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(addressRepository.findDefaultAddressesByUserIds(anyCollection())).thenReturn(List.of(address1));

        adminUserService.getUsers(null, null, null, 0, 20, "createdAt", "desc");

        verify(addressRepository, times(1)).findDefaultAddressesByUserIds(argThat(collection ->
                collection.contains(user1.getId()) && collection.contains(user2.getId()) && collection.size() == 2
        ));
    }

    // ==================== UPDATE USER STATUS TESTS ====================

    @Test
    @DisplayName("Ban user thành công -> Cập nhật status BANNED và lập tức thu hồi toàn bộ Refresh Token")
    void updateUserStatus_Success_BanUser_RevokesTokens() {
        setSecurityContextUser(adminUser);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, "Gian lận voucher khuyến mãi");

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findDefaultAddressesByUserIds(List.of(user1.getId()))).thenReturn(List.of(address1));

        AdminUserResponse response = adminUserService.updateUserStatus(user1.getId(), request);

        assertNotNull(response);
        assertEquals(UserStatus.BANNED, response.status());
        assertEquals("123 Le Loi, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh", response.defaultAddress());
        verify(userRepository, times(1)).save(user1);
        verify(refreshTokenService, times(1)).revokeAll(user1);
    }

    @Test
    @DisplayName("Đình chỉ (SUSPENDED) user thành công -> Thu hồi toàn bộ Refresh Token")
    void updateUserStatus_Success_SuspendUser_RevokesTokens() {
        setSecurityContextUser(adminUser);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, "Tạm khóa để điều tra hành vi");

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findDefaultAddressesByUserIds(List.of(user1.getId()))).thenReturn(List.of());

        AdminUserResponse response = adminUserService.updateUserStatus(user1.getId(), request);

        assertNotNull(response);
        assertEquals(UserStatus.SUSPENDED, response.status());
        assertNull(response.defaultAddress());
        verify(userRepository, times(1)).save(user1);
        verify(refreshTokenService, times(1)).revokeAll(user1);
    }

    @Test
    @DisplayName("Khôi phục (ACTIVE) user thành công -> Không thu hồi Refresh Token")
    void updateUserStatus_Success_UnbanUser_DoesNotRevokeTokens() {
        setSecurityContextUser(adminUser);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE, "Khiếu nại thành công, gỡ cấm");

        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findDefaultAddressesByUserIds(List.of(user2.getId()))).thenReturn(List.of());

        AdminUserResponse response = adminUserService.updateUserStatus(user2.getId(), request);

        assertNotNull(response);
        assertEquals(UserStatus.ACTIVE, response.status());
        verify(userRepository, times(1)).save(user2);
        verify(refreshTokenService, never()).revokeAll(any(User.class));
    }

    @Test
    @DisplayName("Admin tự khóa tài khoản của chính mình -> Ném lỗi CANNOT_BAN_SELF (400)")
    void updateUserStatus_SelfBan_ThrowsCannotBanSelf() {
        setSecurityContextUser(adminUser);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, "Tự khóa");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminUserService.updateUserStatus(adminUser.getId(), request)
        );

        assertEquals(ErrorCode.CANNOT_BAN_SELF, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Admin cố tình cấm một Admin khác -> Ném lỗi CANNOT_MODIFY_ADMIN_USER (403)")
    void updateUserStatus_TargetIsAdmin_ThrowsCannotModifyAdminUser() {
        setSecurityContextUser(adminUser);
        User anotherAdmin = new User();
        anotherAdmin.setId(UUID.randomUUID());
        anotherAdmin.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        anotherAdmin.setStatus(UserStatus.ACTIVE);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, "Khóa admin khác");

        when(userRepository.findById(anotherAdmin.getId())).thenReturn(Optional.of(anotherAdmin));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminUserService.updateUserStatus(anotherAdmin.getId(), request)
        );

        assertEquals(ErrorCode.CANNOT_MODIFY_ADMIN_USER, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Cố tình chuyển trạng thái sang DELETED -> Ném lỗi INVALID_STATUS_TRANSITION (400)")
    void updateUserStatus_TransitionToDeleted_ThrowsInvalidStatusTransition() {
        setSecurityContextUser(adminUser);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.DELETED, "Xóa tài khoản qua API status");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminUserService.updateUserStatus(user1.getId(), request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Cố tình đổi trạng thái của tài khoản đã bị DELETED -> Ném lỗi INVALID_STATUS_TRANSITION (400)")
    void updateUserStatus_TargetAlreadyDeleted_ThrowsInvalidStatusTransition() {
        setSecurityContextUser(adminUser);
        User deletedUser = new User();
        deletedUser.setId(UUID.randomUUID());
        deletedUser.setStatus(UserStatus.DELETED);
        deletedUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, "Cấm user đã bị xóa");

        when(userRepository.findById(deletedUser.getId())).thenReturn(Optional.of(deletedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminUserService.updateUserStatus(deletedUser.getId(), request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("User ID không tồn tại trong hệ thống -> Ném lỗi USER_NOT_FOUND (404)")
    void updateUserStatus_UserNotFound_ThrowsUserNotFound() {
        setSecurityContextUser(adminUser);
        UUID randomId = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, "Khóa user không tồn tại");

        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminUserService.updateUserStatus(randomId, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }
}
