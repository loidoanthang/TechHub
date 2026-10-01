package com.techhub.service.shop;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.shop.impl.ShopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShopService - Xem trang thông tin gian hàng công khai (GET /api/shops/{sellerName})")
class ShopServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private ShopServiceImpl shopService;

    private User sampleUser;
    private Seller sampleSeller;
    private Shop sampleShop;
    private Address sampleAddress;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setFirstName("Thang");
        sampleUser.setLastName("Loi");
        sampleUser.setEmail("seller@techhub.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setRoles(new HashSet<>(Set.of(Role.SELLER)));
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);

        sampleSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("LOI DOAN THANG")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        sampleShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(sampleSeller)
                .name("Apple Store VN")
                .description("Cửa hàng phân phối sản phẩm Apple chính hãng tại Việt Nam.")
                .avatarUrl("https://cdn.techhub.com/avatar.png")
                .bannerUrl("https://cdn.techhub.com/banner.png")
                .status(ShopStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        sampleAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(sampleShop.getId())
                .recipientName("Nguyen Van Kho")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Duẩn")
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    @DisplayName("getPublicShopProfile: Lấy thông tin Shop đang hoạt động (ACTIVE) thành công")
    void getPublicShopProfile_Success_ActiveShop() {
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertEquals(sampleShop.getId(), response.id());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals("Apple Store VN", response.name());
        assertEquals(sampleShop.getDescription(), response.description());
        assertEquals(sampleShop.getAvatarUrl(), response.avatarUrl());
        assertEquals(sampleShop.getBannerUrl(), response.bannerUrl());
        assertEquals(ShopStatus.ACTIVE, response.status());
        assertEquals("TP. Hồ Chí Minh", response.warehouseProvince());
        assertEquals("Quận 1", response.warehouseDistrict());
        assertEquals(sampleShop.getCreatedAt(), response.createdAt());

        verify(sellerRepository, times(1)).findBySellerName("apple-store-vn");
        verify(shopRepository, times(1)).findBySellerId(sampleSeller.getId());
        verify(addressRepository, times(1)).findByShopId(sampleShop.getId());
    }

    @Test
    @DisplayName("getPublicShopProfile: Lấy thông tin Shop đang tạm nghỉ (PAUSED) thành công với status = PAUSED")
    void getPublicShopProfile_Success_PausedShop() {
        sampleShop.setStatus(ShopStatus.PAUSED);

        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertEquals(ShopStatus.PAUSED, response.status());
    }

    @Test
    @DisplayName("getPublicShopProfile: Tự động chuẩn hóa chuỗi hoa/thường (Case-Insensitive) thành công")
    void getPublicShopProfile_Success_CaseInsensitiveSlug() {
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("Apple-Store-VN");

        assertNotNull(response);
        assertEquals("apple-store-vn", response.sellerName());
        verify(sellerRepository, times(1)).findBySellerName("apple-store-vn");
    }

    @Test
    @DisplayName("getPublicShopProfile: Tự động trim khoảng trắng đầu cuối của slug thành công")
    void getPublicShopProfile_Success_TrimWhitespace() {
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("  apple-store-vn  ");

        assertNotNull(response);
        assertEquals("apple-store-vn", response.sellerName());
        verify(sellerRepository, times(1)).findBySellerName("apple-store-vn");
    }

    @Test
    @DisplayName("getPublicShopProfile: Slug đạt độ dài tối thiểu 3 ký tự (Boundary Min) thành công")
    void getPublicShopProfile_Success_BoundaryMinLength() {
        sampleSeller.setSellerName("abc");
        when(sellerRepository.findBySellerName("abc")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("abc");

        assertNotNull(response);
        assertEquals("abc", response.sellerName());
    }

    @Test
    @DisplayName("getPublicShopProfile: Slug đạt độ dài tối đa 50 ký tự (Boundary Max) thành công")
    void getPublicShopProfile_Success_BoundaryMaxLength() {
        String longSlug = "a".repeat(50);
        sampleSeller.setSellerName(longSlug);
        when(sellerRepository.findBySellerName(longSlug)).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile(longSlug);

        assertNotNull(response);
        assertEquals(longSlug, response.sellerName());
    }

    @Test
    @DisplayName("getPublicShopProfile: Địa chỉ kho có warehouseDistrict là null -> trả về DTO với warehouseDistrict = null")
    void getPublicShopProfile_Success_WithoutWarehouseDistrict() {
        sampleAddress.setDistrict(null);

        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertEquals("TP. Hồ Chí Minh", response.warehouseProvince());
        assertNull(response.warehouseDistrict());
    }

    @Test
    @DisplayName("getPublicShopProfile: Không tìm thấy Seller theo slug -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_SellerNotFound_ThrowsException() {
        when(sellerRepository.findBySellerName("non-existent-shop")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("non-existent-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Hồ sơ Seller đang PENDING duyệt -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_SellerPending_ThrowsException() {
        sampleSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findBySellerName("pending-shop")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("pending-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Hồ sơ Seller bị REJECTED -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_SellerRejected_ThrowsException() {
        sampleSeller.setVerificationStatus(SellerVerificationStatus.REJECTED);
        when(sellerRepository.findBySellerName("rejected-shop")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("rejected-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: User của chủ shop bị BANNED -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("apple-store-vn"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: User của chủ shop bị DELETED -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("apple-store-vn"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Không tìm thấy bản ghi Shop tương ứng của Seller -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_ShopNotFound_ThrowsException() {
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("apple-store-vn"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Shop đang ở trạng thái SUSPENDED -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_ShopSuspended_ThrowsException() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findBySellerName("suspended-shop")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("suspended-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Shop đang ở trạng thái BANNED -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getPublicShopProfile_ShopBanned_ThrowsException() {
        sampleShop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findBySellerName("banned-shop")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopService.getPublicShopProfile("banned-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("getPublicShopProfile: Khuyết thiếu bản ghi địa chỉ kho hàng -> Graceful fallback trả về null thay vì ném NPE")
    void getPublicShopProfile_MissingAddress_GracefulFallback() {
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.empty());

        PublicShopResponse response = shopService.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertNull(response.warehouseProvince());
        assertNull(response.warehouseDistrict());
    }

    @Test
    @DisplayName("getPublicShopProfile: Shop không có mô tả (description = null) -> trả về DTO với description = null")
    void getPublicShopProfile_DescriptionNull_Allowed() {
        sampleShop.setDescription(null);
        when(sellerRepository.findBySellerName("apple-store-vn")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(sampleShop.getId())).thenReturn(Optional.of(sampleAddress));

        PublicShopResponse response = shopService.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertNull(response.description());
    }
}
