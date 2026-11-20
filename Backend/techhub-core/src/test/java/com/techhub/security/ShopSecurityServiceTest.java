package com.techhub.security;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.enums.Role;
import com.techhub.modules.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShopSecurityService - Kiểm soát hàng rào bảo mật SpEL kênh Seller/Shop")
class ShopSecurityServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopSecurityService shopSecurityService;

    private User sellerUser;
    private Seller seller;
    private Shop activeShop;
    private Shop suspendedShop;
    private Shop bannedShop;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        sellerUser = new User();
        sellerUser.setId(userId);
        sellerUser.setEmail("seller@techhub.com");
        sellerUser.setStatus(UserStatus.ACTIVE);
        sellerUser.setRoles(Set.of(Role.SELLER, Role.BUYER));

        seller = Seller.builder()
                .id(sellerId)
                .user(sellerUser)
                .sellerName("techhub-official")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        activeShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("TechHub Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        suspendedShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("TechHub Suspended Store")
                .status(ShopStatus.SUSPENDED)
                .build();

        bannedShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("TechHub Banned Store")
                .status(ShopStatus.BANNED)
                .build();
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
    @DisplayName("TC-SHOP-01: Shop ACTIVE -> Hợp lệ, trả về true")
    void isActiveShop_Success_WhenShopActive() {
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(activeShop));

        boolean result = shopSecurityService.isActiveShop();

        assertTrue(result, "Shop ACTIVE phải được phép vượt qua hàng rào bảo mật Seller");
        verify(sellerRepository, times(1)).findByUserId(sellerUser.getId());
        verify(shopRepository, times(1)).findBySellerId(seller.getId());
    }

    @Test
    @DisplayName("TC-SHOP-02: Shop SUSPENDED -> Ném lỗi SHOP_NOT_ACTIVE (HTTP 400/403)")
    void isActiveShop_ThrowsException_WhenShopSuspended() {
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(suspendedShop));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, exception.getErrorCode(),
                "Shop SUSPENDED phải bị chặn thao tác ghi với mã lỗi SHOP_NOT_ACTIVE");
    }

    @Test
    @DisplayName("TC-SHOP-03: Shop BANNED -> Ném lỗi SHOP_NOT_ACTIVE (HTTP 400/403)")
    void isActiveShop_ThrowsException_WhenShopBanned() {
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(bannedShop));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, exception.getErrorCode(),
                "Shop BANNED phải bị chặn tuyệt đối");
    }

    @Test
    @DisplayName("TC-SHOP-04: User chưa đăng ký Seller -> Ném lỗi NOT_A_SELLER")
    void isActiveShop_ThrowsException_WhenNotSeller() {
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.NOT_A_SELLER, exception.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC-SHOP-05: Seller chưa có Shop trong DB -> Ném lỗi SHOP_NOT_FOUND")
    void isActiveShop_ThrowsException_WhenShopNotFound() {
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-SHOP-06: Phân lập Role (BR-28) - User bị SUSPENDED nhưng Shop ACTIVE -> Vẫn trả về true")
    void isActiveShop_Success_WhenUserSuspended_ButShopActive() {
        // Giả lập tài khoản User cá nhân bị SUSPENDED (ví dụ: bị phạt ở kênh Buyer)
        sellerUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContext(sellerUser);

        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(activeShop));

        // ShopSecurityService chỉ quan tâm ShopStatus, không chặn kênh bán hàng nếu Shop vẫn ACTIVE
        boolean result = shopSecurityService.isActiveShop();

        assertTrue(result, "Theo BR-28: UserStatus.SUSPENDED không được phép chặn thao tác bán hàng của Shop ACTIVE");
    }

    @Test
    @DisplayName("TC-SHOP-07: Cascade Ban (BR-28) - User bị BANNED -> Chặn tuyệt đối, ném ACCOUNT_BANNED")
    void isActiveShop_ThrowsException_WhenUserBanned() {
        sellerUser.setStatus(UserStatus.BANNED);
        setSecurityContext(sellerUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verifyNoInteractions(sellerRepository);
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-SHOP-08: Cascade Delete (BR-28) - User bị DELETED -> Chặn tuyệt đối, ném ACCOUNT_DELETED")
    void isActiveShop_ThrowsException_WhenUserDeleted() {
        sellerUser.setStatus(UserStatus.DELETED);
        setSecurityContext(sellerUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
        verifyNoInteractions(sellerRepository);
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-SHOP-09: Seller chưa được duyệt (PENDING/REJECTED) -> Ném lỗi SELLER_NOT_VERIFIED")
    void isActiveShop_ThrowsException_WhenSellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        setSecurityContext(sellerUser);
        when(sellerRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(seller));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                shopSecurityService.isActiveShop()
        );

        assertEquals(ErrorCode.SELLER_NOT_VERIFIED, exception.getErrorCode(),
                "Seller chưa APPROVED phải bị chặn ngay tại ShopSecurityService");
        verifyNoInteractions(shopRepository);
    }
}
