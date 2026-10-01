package com.techhub.service.admin;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.SellerApprovalRequest;
import com.techhub.model.dto.response.AdminSellerApprovalResponse;
import com.techhub.model.dto.response.AdminSellerDetailResponse;
import com.techhub.model.dto.response.AdminSellerResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.ShippingOption;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShippingOptionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.admin.impl.AdminSellerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminSellerService (GET /api/admin/sellers)")
class AdminSellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ShippingOptionRepository shippingOptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminSellerServiceImpl adminSellerService;

    private User user1;
    private User user2;
    private User user3;

    private Seller seller1;
    private Seller seller2;
    private Seller seller3;

    private Shop shop1;
    private Shop shop2;
    private Shop shop3;

    private Address address1;
    private ShippingOption shippingOption1;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setFirstName("Van A");
        user1.setLastName("Nguyen");
        user1.setEmail("vana@techhub.vn");
        user1.setPhone("0987654321");
        user1.setStatus(UserStatus.ACTIVE);
        user1.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        user1.setEmailVerified(true);
        user1.setCreatedAt(LocalDateTime.now().minusDays(10));

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setFirstName("Thi B");
        user2.setLastName("Tran");
        user2.setEmail("thib@techhub.vn");
        user2.setPhone("0912345678");
        user2.setStatus(UserStatus.ACTIVE);
        user2.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
        user2.setEmailVerified(true);
        user2.setCreatedAt(LocalDateTime.now().minusDays(20));

        user3 = new User();
        user3.setId(UUID.randomUUID());
        user3.setFirstName("Van C");
        user3.setLastName("Le");
        user3.setEmail("vanc@techhub.vn");
        user3.setPhone("0933333333");
        user3.setStatus(UserStatus.ACTIVE);
        user3.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        user3.setEmailVerified(true);
        user3.setCreatedAt(LocalDateTime.now().minusDays(5));

        seller1 = Seller.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .rejectionReason(null)
                .bankName("Vietcombank")
                .bankAccountNumber("0071001234567")
                .bankAccountHolder("NGUYEN VAN A")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        seller2 = Seller.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .sellerName("samsung-flagship")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .rejectionReason(null)
                .bankName("Techcombank")
                .bankAccountNumber("1903123456789")
                .bankAccountHolder("TRAN THI B")
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        seller3 = Seller.builder()
                .id(UUID.randomUUID())
                .user(user3)
                .sellerName("xiaomi-mall")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason("Giấy phép kinh doanh không hợp lệ")
                .bankName("MBBank")
                .bankAccountNumber("098111222333")
                .bankAccountHolder("LE VAN C")
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        shop1 = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller1)
                .name("Apple Official Store")
                .status(ShopStatus.SUSPENDED)
                .build();

        shop2 = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller2)
                .name("Samsung Flagship")
                .status(ShopStatus.ACTIVE)
                .build();

        shop3 = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller3)
                .name("Xiaomi Mall")
                .description("Xiaomi Official Store Description")
                .avatarUrl("https://cdn.techhub.vn/avatar3.png")
                .bannerUrl("https://cdn.techhub.vn/banner3.png")
                .status(ShopStatus.SUSPENDED)
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();

        shop1.setDescription("Apple Official Store Description");
        shop1.setAvatarUrl("https://cdn.techhub.vn/avatar1.png");
        shop1.setBannerUrl("https://cdn.techhub.vn/banner1.png");
        shop1.setCreatedAt(LocalDateTime.now().minusDays(1));
        shop1.setUpdatedAt(LocalDateTime.now().minusDays(1));

        shop2.setDescription("Samsung Flagship Store Description");
        shop2.setAvatarUrl("https://cdn.techhub.vn/avatar2.png");
        shop2.setBannerUrl("https://cdn.techhub.vn/banner2.png");
        shop2.setCreatedAt(LocalDateTime.now().minusDays(2));
        shop2.setUpdatedAt(LocalDateTime.now().minusDays(2));

        address1 = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop1.getId())
                .recipientName("Nguyen Van Kho")
                .phone("0901234567")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Số 123 Lê Lợi")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        shippingOption1 = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop1)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("TC-01: getSellers_Success_DefaultPagination - Lấy danh sách mặc định có phân trang")
    void getSellers_Success_DefaultPagination() {
        List<Seller> sellers = List.of(seller1, seller2, seller3);
        Page<Seller> sellerPage = new PageImpl<>(sellers);

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1, shop2, shop3));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(3, result.content().size());
        assertEquals("apple-official-store", result.content().get(0).sellerName());
        assertEquals("Apple Official Store", result.content().get(0).shopName());
        assertEquals("Van A Nguyen", result.content().get(0).ownerName().trim());
        verify(sellerRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(shopRepository, times(1)).findBySellerIdIn(any());
    }

    @Test
    @DisplayName("TC-02: getSellers_Success_FilterByStatus_Pending - Lọc theo status PENDING")
    void getSellers_Success_FilterByStatus_Pending() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, SellerVerificationStatus.PENDING, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(SellerVerificationStatus.PENDING, result.content().get(0).verificationStatus());
    }

    @Test
    @DisplayName("TC-03: getSellers_Success_FilterByStatus_Approved - Lọc theo status APPROVED")
    void getSellers_Success_FilterByStatus_Approved() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller2));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop2));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, SellerVerificationStatus.APPROVED, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(SellerVerificationStatus.APPROVED, result.content().get(0).verificationStatus());
        assertEquals(ShopStatus.ACTIVE, result.content().get(0).shopStatus());
    }

    @Test
    @DisplayName("TC-04: getSellers_Success_FilterByStatus_Rejected - Lọc theo status REJECTED và có lý do")
    void getSellers_Success_FilterByStatus_Rejected() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller3));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop3));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, SellerVerificationStatus.REJECTED, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(SellerVerificationStatus.REJECTED, result.content().get(0).verificationStatus());
        assertEquals("Giấy phép kinh doanh không hợp lệ", result.content().get(0).rejectionReason());
    }

    @Test
    @DisplayName("TC-05: getSellers_Success_SearchBySellerNameSlug - Tìm kiếm theo slug")
    void getSellers_Success_SearchBySellerNameSlug() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "apple-official", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("apple-official-store", result.content().get(0).sellerName());
    }

    @Test
    @DisplayName("TC-06: getSellers_Success_SearchByShopName - Tìm kiếm theo tên Shop")
    void getSellers_Success_SearchByShopName() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller2));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop2));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "Samsung", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Samsung Flagship", result.content().get(0).shopName());
    }

    @Test
    @DisplayName("TC-07: getSellers_Success_SearchByOwnerEmail - Tìm kiếm theo email chủ sở hữu")
    void getSellers_Success_SearchByOwnerEmail() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "vana@techhub.vn", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("vana@techhub.vn", result.content().get(0).ownerEmail());
    }

    @Test
    @DisplayName("TC-08: getSellers_Success_SearchByOwnerPhone - Tìm kiếm theo SĐT chủ sở hữu")
    void getSellers_Success_SearchByOwnerPhone() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller2));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop2));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "0912345678", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("0912345678", result.content().get(0).ownerPhone());
    }

    @Test
    @DisplayName("TC-09: getSellers_Success_SearchByOwnerFullName - Tìm kiếm theo họ tên chủ shop")
    void getSellers_Success_SearchByOwnerFullName() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller3));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop3));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "Van C", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0).ownerName().contains("Van C"));
    }

    @Test
    @DisplayName("TC-10: getSellers_Success_EmptyResult - Kết quả tìm kiếm rỗng, không gọi query Shop")
    void getSellers_Success_EmptyResult() {
        Page<Seller> emptyPage = Page.empty();

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                "nonexistentxyz", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        verify(shopRepository, never()).findBySellerIdIn(any());
    }

    @Test
    @DisplayName("TC-11: getSellers_Success_SortBySellerName_Asc - Sắp xếp theo sellerName ASC")
    void getSellers_Success_SortBySellerName_Asc() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1, seller2));

        when(sellerRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1, shop2));

        adminSellerService.getSellers(null, null, 0, 20, "sellerName", "asc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable.getSort().getOrderFor("sellerName"));
        assertEquals(Sort.Direction.ASC, capturedPageable.getSort().getOrderFor("sellerName").getDirection());
    }

    @Test
    @DisplayName("TC-12: getSellers_Success_InvalidSortBy_FallsBackToCreatedAt - Fallback về createdAt khi sortBy không hợp lệ")
    void getSellers_Success_InvalidSortBy_FallsBackToCreatedAt() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        adminSellerService.getSellers(null, null, 0, 20, "hackerField", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable.getSort().getOrderFor("createdAt"));
        assertNull(capturedPageable.getSort().getOrderFor("hackerField"));
    }

    @Test
    @DisplayName("TC-13: getSellers_Success_SanitizeNegativePage - Sanitize số trang âm về 0")
    void getSellers_Success_SanitizeNegativePage() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        adminSellerService.getSellers(null, null, -5, 20, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
    }

    @Test
    @DisplayName("TC-14: getSellers_Success_SanitizeExceededPageSize - Sanitize kích thước trang vượt trần về 100")
    void getSellers_Success_SanitizeExceededPageSize() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        adminSellerService.getSellers(null, null, 0, 500, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(100, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("TC-15: getSellers_Success_OrphanedShop_GracefulFallback - Xử lý an toàn khi Shop bị thiếu bản ghi")
    void getSellers_Success_OrphanedShop_GracefulFallback() {
        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of()); // Không có Shop

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        AdminSellerResponse response = result.content().get(0);
        assertNull(response.shopId());
        assertNull(response.shopName());
        assertNull(response.shopStatus());
    }

    @Test
    @DisplayName("TC-16: getSellers_Success_NullOwnerNames_HandlesSafely - Xử lý an toàn khi tên User bị null")
    void getSellers_Success_NullOwnerNames_HandlesSafely() {
        user1.setFirstName(null);
        user1.setLastName(null);

        Page<Seller> sellerPage = new PageImpl<>(List.of(seller1));

        when(sellerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(sellerPage);
        when(shopRepository.findBySellerIdIn(any())).thenReturn(List.of(shop1));

        PageResponse<AdminSellerResponse> result = adminSellerService.getSellers(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("", result.content().get(0).ownerName());
    }

    // ==========================================
    // UNIT TESTS CHO GET /api/admin/sellers/{id}
    // ==========================================

    @Test
    @DisplayName("TC-DETAIL-01: getSellerDetail_Success_PendingSeller - Thẩm định hồ sơ PENDING đầy đủ 5 thực thể")
    void getSellerDetail_Success_PendingSeller() {
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));
        when(addressRepository.findByShopId(shop1.getId())).thenReturn(Optional.of(address1));
        when(shippingOptionRepository.findByShopId(shop1.getId())).thenReturn(Optional.of(shippingOption1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertEquals(seller1.getId(), result.id());
        assertEquals("apple-official-store", result.sellerName());
        assertEquals(SellerVerificationStatus.PENDING, result.verificationStatus());
        assertNull(result.rejectionReason());
        assertEquals("Vietcombank", result.bankName());
        assertEquals("0071001234567", result.bankAccountNumber());
        assertEquals("NGUYEN VAN A", result.bankAccountHolder());

        // User info
        assertNotNull(result.user());
        assertEquals(user1.getId(), result.user().id());
        assertEquals("Van A Nguyen", result.user().fullName());
        assertEquals("vana@techhub.vn", result.user().email());
        assertEquals(UserStatus.ACTIVE, result.user().status());
        assertTrue(result.user().roles().contains(Role.BUYER));

        // Shop info
        assertNotNull(result.shop());
        assertEquals(shop1.getId(), result.shop().id());
        assertEquals("Apple Official Store", result.shop().name());
        assertEquals("Apple Official Store Description", result.shop().description());
        assertEquals(ShopStatus.SUSPENDED, result.shop().status());

        // Warehouse info
        assertNotNull(result.warehouseAddress());
        assertEquals(address1.getId(), result.warehouseAddress().id());
        assertEquals("Nguyen Van Kho", result.warehouseAddress().contactName());
        assertEquals("0901234567", result.warehouseAddress().phone());
        assertEquals("TP. Hồ Chí Minh", result.warehouseAddress().province());
        assertEquals("Quận 1", result.warehouseAddress().district());
        assertEquals("Phường Bến Nghé", result.warehouseAddress().ward());
        assertEquals("Số 123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", result.warehouseAddress().fullAddress());

        // Shipping Option info
        assertNotNull(result.shippingOption());
        assertEquals(shippingOption1.getId(), result.shippingOption().id());
        assertEquals("Standard Delivery", result.shippingOption().name());
        assertEquals(new BigDecimal("25000.00"), result.shippingOption().shippingFee());
        assertTrue(result.shippingOption().isActive());

        verify(sellerRepository, times(1)).findWithUserById(seller1.getId());
        verify(shopRepository, times(1)).findBySellerId(seller1.getId());
        verify(addressRepository, times(1)).findByShopId(shop1.getId());
        verify(shippingOptionRepository, times(1)).findByShopId(shop1.getId());
    }

    @Test
    @DisplayName("TC-DETAIL-02: getSellerDetail_Success_ApprovedSeller - Xem chi tiết hồ sơ APPROVED")
    void getSellerDetail_Success_ApprovedSeller() {
        when(sellerRepository.findWithUserById(seller2.getId())).thenReturn(Optional.of(seller2));
        when(shopRepository.findBySellerId(seller2.getId())).thenReturn(Optional.of(shop2));
        when(addressRepository.findByShopId(shop2.getId())).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shop2.getId())).thenReturn(Optional.empty());

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller2.getId());

        assertNotNull(result);
        assertEquals(seller2.getId(), result.id());
        assertEquals(SellerVerificationStatus.APPROVED, result.verificationStatus());
        assertNotNull(result.user());
        assertTrue(result.user().roles().contains(Role.SELLER));
        assertTrue(result.user().roles().contains(Role.BUYER));
        assertNotNull(result.shop());
        assertEquals(ShopStatus.ACTIVE, result.shop().status());
    }

    @Test
    @DisplayName("TC-DETAIL-03: getSellerDetail_Success_RejectedSeller - Xem chi tiết hồ sơ REJECTED kèm lý do")
    void getSellerDetail_Success_RejectedSeller() {
        when(sellerRepository.findWithUserById(seller3.getId())).thenReturn(Optional.of(seller3));
        when(shopRepository.findBySellerId(seller3.getId())).thenReturn(Optional.of(shop3));
        when(addressRepository.findByShopId(shop3.getId())).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shop3.getId())).thenReturn(Optional.empty());

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller3.getId());

        assertNotNull(result);
        assertEquals(SellerVerificationStatus.REJECTED, result.verificationStatus());
        assertEquals("Giấy phép kinh doanh không hợp lệ", result.rejectionReason());
    }

    @Test
    @DisplayName("TC-DETAIL-04: getSellerDetail_Success_PausedShop - Shop đang ở trạng thái PAUSED")
    void getSellerDetail_Success_PausedShop() {
        shop1.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.shop());
        assertEquals(ShopStatus.PAUSED, result.shop().status());
    }

    @Test
    @DisplayName("TC-DETAIL-05: getSellerDetail_Success_BannedShop - Shop đang ở trạng thái BANNED")
    void getSellerDetail_Success_BannedShop() {
        shop1.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.shop());
        assertEquals(ShopStatus.BANNED, result.shop().status());
    }

    @Test
    @DisplayName("TC-DETAIL-06: getSellerDetail_SellerNotFound_Throws404 - Không tìm thấy Seller ném lỗi SELLER_NOT_FOUND")
    void getSellerDetail_SellerNotFound_Throws404() {
        UUID notFoundId = UUID.randomUUID();
        when(sellerRepository.findWithUserById(notFoundId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminSellerService.getSellerDetail(notFoundId)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, exception.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC-DETAIL-07: getSellerDetail_Defensive_WhenShopMissing - Xử lý an toàn khi Shop bị thiếu bản ghi")
    void getSellerDetail_Defensive_WhenShopMissing() {
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.empty());

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNull(result.shop());
        assertNull(result.warehouseAddress());
        assertNull(result.shippingOption());
        verify(addressRepository, never()).findByShopId(any());
        verify(shippingOptionRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("TC-DETAIL-08: getSellerDetail_Defensive_WhenAddressMissing - Xử lý an toàn khi Address bị thiếu")
    void getSellerDetail_Defensive_WhenAddressMissing() {
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));
        when(addressRepository.findByShopId(shop1.getId())).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shop1.getId())).thenReturn(Optional.of(shippingOption1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.shop());
        assertNull(result.warehouseAddress());
        assertNotNull(result.shippingOption());
    }

    @Test
    @DisplayName("TC-DETAIL-09: getSellerDetail_Defensive_WhenShippingOptionMissing - Xử lý an toàn khi ShippingOption bị thiếu")
    void getSellerDetail_Defensive_WhenShippingOptionMissing() {
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));
        when(addressRepository.findByShopId(shop1.getId())).thenReturn(Optional.of(address1));
        when(shippingOptionRepository.findByShopId(shop1.getId())).thenReturn(Optional.empty());

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.shop());
        assertNotNull(result.warehouseAddress());
        assertNull(result.shippingOption());
    }

    @Test
    @DisplayName("TC-DETAIL-10: getSellerDetail_Defensive_NullSafeFullAddress - Ghép địa chỉ null-safe khi thiếu trường")
    void getSellerDetail_Defensive_NullSafeFullAddress() {
        address1.setStreetAddress(null);
        address1.setWard(null);
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));
        when(addressRepository.findByShopId(shop1.getId())).thenReturn(Optional.of(address1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.warehouseAddress());
        assertFalse(result.warehouseAddress().fullAddress().contains("null"));
        assertEquals("Quận 1, TP. Hồ Chí Minh", result.warehouseAddress().fullAddress());
    }

    @Test
    @DisplayName("TC-DETAIL-11: getSellerDetail_Defensive_NullSafeFullName - Ghép họ tên null-safe khi User thiếu tên")
    void getSellerDetail_Defensive_NullSafeFullName() {
        user1.setFirstName(null);
        user1.setLastName(null);
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNotNull(result.user());
        assertEquals("", result.user().fullName());
    }

    @Test
    @DisplayName("TC-DETAIL-12: getSellerDetail_Defensive_NullUser - Xử lý an toàn khi User bị null")
    void getSellerDetail_Defensive_NullUser() {
        seller1.setUser(null);
        when(sellerRepository.findWithUserById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        AdminSellerDetailResponse result = adminSellerService.getSellerDetail(seller1.getId());

        assertNotNull(result);
        assertNull(result.user());
    }

    // =========================================================================
    // TESTS FOR PATCH /api/admin/sellers/{id}/approval (updateSellerApproval)
    // =========================================================================

    @Test
    @DisplayName("TC-01: updateSellerApproval_Approve_PendingSeller_Success - Duyệt hồ sơ PENDING: cấp SELLER role, shop ACTIVE, xóa reason")
    void updateSellerApproval_Approve_PendingSeller_Success() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller1.getId(), request);

        assertNotNull(response);
        assertEquals(seller1.getId(), response.sellerId());
        assertEquals(user1.getId(), response.userId());
        assertEquals(shop1.getId(), response.shopId());
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals(ShopStatus.ACTIVE, response.shopStatus());
        assertNull(response.rejectionReason());
        assertTrue(response.roles().contains(Role.SELLER));
        assertTrue(response.roles().contains(Role.BUYER));

        assertEquals(SellerVerificationStatus.APPROVED, seller1.getVerificationStatus());
        assertNull(seller1.getRejectionReason());
        assertEquals(ShopStatus.ACTIVE, shop1.getStatus());
        assertTrue(user1.getRoles().contains(Role.SELLER));

        verify(sellerRepository).save(seller1);
        verify(userRepository).save(user1);
        verify(shopRepository).save(shop1);
    }

    @Test
    @DisplayName("TC-02: updateSellerApproval_Reject_PendingSeller_Success - Từ chối hồ sơ PENDING: lưu reason, shop SUSPENDED, không cấp role")
    void updateSellerApproval_Reject_PendingSeller_Success() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "Ảnh CCCD bị mờ");

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller1.getId(), request);

        assertNotNull(response);
        assertEquals(SellerVerificationStatus.REJECTED, response.verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());
        assertEquals("Ảnh CCCD bị mờ", response.rejectionReason());
        assertFalse(response.roles().contains(Role.SELLER));
        assertTrue(response.roles().contains(Role.BUYER));

        assertEquals(SellerVerificationStatus.REJECTED, seller1.getVerificationStatus());
        assertEquals("Ảnh CCCD bị mờ", seller1.getRejectionReason());
        assertEquals(ShopStatus.SUSPENDED, shop1.getStatus());

        verify(sellerRepository).save(seller1);
        verify(userRepository).save(user1);
        verify(shopRepository).save(shop1);
    }

    @Test
    @DisplayName("TC-03: updateSellerApproval_Approve_RejectedSeller_Success - Duyệt lại hồ sơ REJECTED: đổi APPROVED, cấp role SELLER, shop ACTIVE")
    void updateSellerApproval_Approve_RejectedSeller_Success() {
        when(sellerRepository.findWithUserForUpdateById(seller3.getId())).thenReturn(Optional.of(seller3));
        when(shopRepository.findBySellerId(seller3.getId())).thenReturn(Optional.of(shop3));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller3.getId(), request);

        assertNotNull(response);
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals(ShopStatus.ACTIVE, response.shopStatus());
        assertNull(response.rejectionReason());
        assertTrue(response.roles().contains(Role.SELLER));

        assertEquals(SellerVerificationStatus.APPROVED, seller3.getVerificationStatus());
        assertNull(seller3.getRejectionReason());
        assertEquals(ShopStatus.ACTIVE, shop3.getStatus());
        assertTrue(user3.getRoles().contains(Role.SELLER));

        verify(sellerRepository).save(seller3);
        verify(userRepository).save(user3);
        verify(shopRepository).save(shop3);
    }

    @Test
    @DisplayName("TC-04: updateSellerApproval_Reject_RejectedSeller_UpdateReason_Success - Cập nhật lý do từ chối mới cho hồ sơ đã REJECTED")
    void updateSellerApproval_Reject_RejectedSeller_UpdateReason_Success() {
        when(sellerRepository.findWithUserForUpdateById(seller3.getId())).thenReturn(Optional.of(seller3));
        when(shopRepository.findBySellerId(seller3.getId())).thenReturn(Optional.of(shop3));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "Giấy tờ sai định dạng PDF");

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller3.getId(), request);

        assertNotNull(response);
        assertEquals(SellerVerificationStatus.REJECTED, response.verificationStatus());
        assertEquals("Giấy tờ sai định dạng PDF", response.rejectionReason());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());

        assertEquals("Giấy tờ sai định dạng PDF", seller3.getRejectionReason());

        verify(sellerRepository).save(seller3);
        verify(userRepository).save(user3);
        verify(shopRepository).save(shop3);
    }

    @Test
    @DisplayName("TC-05: updateSellerApproval_Approve_AlreadyApproved_Idempotent - Gửi lại request duyệt cho Seller đã APPROVED: idempotent")
    void updateSellerApproval_Approve_AlreadyApproved_Idempotent() {
        when(sellerRepository.findWithUserForUpdateById(seller2.getId())).thenReturn(Optional.of(seller2));
        when(shopRepository.findBySellerId(seller2.getId())).thenReturn(Optional.of(shop2));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller2.getId(), request);

        assertNotNull(response);
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals(ShopStatus.ACTIVE, response.shopStatus());
        assertTrue(response.roles().contains(Role.SELLER));
        assertTrue(response.roles().contains(Role.BUYER));

        verify(sellerRepository).save(seller2);
        verify(userRepository).save(user2);
        verify(shopRepository).save(shop2);
    }

    @Test
    @DisplayName("TC-06: updateSellerApproval_Reject_ApprovedSeller_ThrowsInvalidStatusTransition - Cố tình reject Seller đã APPROVED: ném lỗi")
    void updateSellerApproval_Reject_ApprovedSeller_ThrowsInvalidStatusTransition() {
        when(sellerRepository.findWithUserForUpdateById(seller2.getId())).thenReturn(Optional.of(seller2));
        when(shopRepository.findBySellerId(seller2.getId())).thenReturn(Optional.of(shop2));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "Lý do vi phạm");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller2.getId(), request));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-07: updateSellerApproval_SetPending_ThrowsInvalidStatusTransition - Admin gửi status = PENDING: ném lỗi")
    void updateSellerApproval_SetPending_ThrowsInvalidStatusTransition() {
        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.PENDING, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(sellerRepository, never()).findWithUserForUpdateById(any());
    }

    @Test
    @DisplayName("TC-08: updateSellerApproval_Reject_MissingReason_ThrowsException - Từ chối nhưng rejectionReason null: ném REJECTION_REASON_REQUIRED")
    void updateSellerApproval_Reject_MissingReason_ThrowsException() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09: updateSellerApproval_Reject_BlankReason_ThrowsException - Từ chối nhưng rejectionReason chỉ có whitespace: ném lỗi")
    void updateSellerApproval_Reject_BlankReason_ThrowsException() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "    ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10: updateSellerApproval_SellerNotFound_Throws404 - ID Seller không tồn tại: ném SELLER_NOT_FOUND")
    void updateSellerApproval_SellerNotFound_Throws404() {
        UUID randomId = UUID.randomUUID();
        when(sellerRepository.findWithUserForUpdateById(randomId)).thenReturn(Optional.empty());

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(randomId, request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: updateSellerApproval_ShopNotFound_Throws404 - Không tìm thấy Shop liên kết: ném SHOP_NOT_FOUND")
    void updateSellerApproval_ShopNotFound_Throws404() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.empty());

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: updateSellerApproval_UserNotFound_Throws404 - User liên kết với Seller bị null: ném USER_NOT_FOUND")
    void updateSellerApproval_UserNotFound_Throws404() {
        seller1.setUser(null);
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: updateSellerApproval_Reject_TrimReason_Success - Trim khoảng trắng đầu/cuối của rejectionReason")
    void updateSellerApproval_Reject_TrimReason_Success() {
        when(sellerRepository.findWithUserForUpdateById(seller1.getId())).thenReturn(Optional.of(seller1));
        when(shopRepository.findBySellerId(seller1.getId())).thenReturn(Optional.of(shop1));

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "   Ảnh mờ cần chụp lại   ");

        AdminSellerApprovalResponse response = adminSellerService.updateSellerApproval(seller1.getId(), request);

        assertNotNull(response);
        assertEquals("Ảnh mờ cần chụp lại", response.rejectionReason());
        assertEquals("Ảnh mờ cần chụp lại", seller1.getRejectionReason());
    }

    @Test
    @DisplayName("TC-13B: updateSellerApproval_Defensive_NullStatus_ThrowsInvalidStatusTransition - Request có status null: ném INVALID_STATUS_TRANSITION")
    void updateSellerApproval_Defensive_NullStatus_ThrowsInvalidStatusTransition() {
        SellerApprovalRequest request = new SellerApprovalRequest(null, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerService.updateSellerApproval(seller1.getId(), request));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(sellerRepository, never()).findWithUserForUpdateById(any());
    }
}



