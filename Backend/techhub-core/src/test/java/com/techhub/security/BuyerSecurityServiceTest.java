package com.techhub.security;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.enums.Role;
import com.techhub.modules.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerSecurityService - Kiểm soát hàng rào bảo mật SpEL kênh Buyer")
class BuyerSecurityServiceTest {

    @InjectMocks
    private BuyerSecurityService buyerSecurityService;

    private User activeUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("active@techhub.com");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setRoles(Set.of(Role.BUYER));

        suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setEmail("suspended@techhub.com");
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        suspendedUser.setRoles(Set.of(Role.BUYER));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("TC-BUYER-01: User ACTIVE gọi isActiveBuyer() -> Hợp lệ, trả về true")
    void isActiveBuyer_Success_WhenUserActive() {
        setSecurityContext(activeUser);

        boolean result = buyerSecurityService.isActiveBuyer();

        assertTrue(result, "User ACTIVE phải được phép vượt qua hàng rào bảo mật Buyer");
    }

    @Test
    @DisplayName("TC-BUYER-02: User SUSPENDED gọi isActiveBuyer() -> Ném lỗi BUYER_ACCOUNT_SUSPENDED (HTTP 403)")
    void isActiveBuyer_ThrowsException_WhenUserSuspended() {
        setSecurityContext(suspendedUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerSecurityService.isActiveBuyer()
        );

        assertEquals(ErrorCode.BUYER_ACCOUNT_SUSPENDED, exception.getErrorCode(),
                "Tài khoản SUSPENDED phải bị chặn với mã lỗi BUYER_ACCOUNT_SUSPENDED");
    }

    @Test
    @DisplayName("TC-BUYER-04: User BANNED cố gọi isActiveBuyer() -> Ném lỗi ACCOUNT_BANNED (HTTP 403)")
    void isActiveBuyer_ThrowsException_WhenUserBanned() {
        User bannedUser = new User();
        bannedUser.setId(UUID.randomUUID());
        bannedUser.setEmail("banned@techhub.com");
        bannedUser.setStatus(UserStatus.BANNED);
        bannedUser.setRoles(Set.of(Role.BUYER));
        setSecurityContext(bannedUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerSecurityService.isActiveBuyer()
        );

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-BUYER-05: User DELETED cố gọi isActiveBuyer() -> Ném lỗi ACCOUNT_DELETED (HTTP 403)")
    void isActiveBuyer_ThrowsException_WhenUserDeleted() {
        User deletedUser = new User();
        deletedUser.setId(UUID.randomUUID());
        deletedUser.setEmail("deleted@techhub.com");
        deletedUser.setStatus(UserStatus.DELETED);
        deletedUser.setRoles(Set.of(Role.BUYER));
        setSecurityContext(deletedUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerSecurityService.isActiveBuyer()
        );

        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
    }
}
