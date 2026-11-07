package com.techhub.service.shop;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.AdminUpdateShopStatusRequest;
import com.techhub.model.dto.response.AdminShopDetailResponse;
import com.techhub.model.dto.response.AdminShopResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.ShippingOption;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ShippingOptionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.shop.impl.AdminShopServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminShopService (GET /api/admin/shops & GET /api/admin/shops/{id})")
class AdminShopServiceImplTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ShippingOptionRepository shippingOptionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminShopServiceImpl adminShopService;

    private User sampleUser;
    private Seller sampleSeller;
    private Shop sampleShop;
    private Address sampleAddress;
    private ShippingOption sampleShippingOption;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setFirstName("Văn An");
        sampleUser.setLastName("Nguyễn");
        sampleUser.setEmail("an.nguyen@techstore.vn");
        sampleUser.setPhone("0987654321");
        sampleUser.setStatus(UserStatus.ACTIVE);

        sampleSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("tech_official")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0011001234567")
                .bankAccountHolder("NGUYEN VAN AN")
                .balance(new BigDecimal("154200000.00"))
                .build();

        sampleShop = Shop.builder()
                .id(shopId)
                .name("Official Tech Store")
                .description("Cửa hàng công nghệ chính hãng")
                .avatarUrl("https://cdn.techhub.com/avatar.png")
                .bannerUrl("https://cdn.techhub.com/banner.png")
                .status(ShopStatus.ACTIVE)
                .seller(sampleSeller)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        sampleAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .recipientName("Nguyễn Văn An")
                .phone("0987654321")
                .province("Thành phố Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng Hậu")
                .streetAddress("Số 12 Duy Tân")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(30))
                .build();

        sampleShippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(sampleShop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(30))
                .build();
    }

    // ==========================================
    // TESTS CHO GET /api/admin/shops (API 7)
    // ==========================================

    @Test
    @DisplayName("TC-01: Lấy danh sách mặc định thành công (Default parameters)")
    void getShops_defaultParameters_success() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId)))
                .thenReturn(List.<Object[]>of(new Object[]{shopId, 48L}));

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());

        AdminShopResponse item = response.content().get(0);
        assertEquals(shopId, item.id());
        assertEquals("Official Tech Store", item.name());
        assertEquals(ShopStatus.ACTIVE, item.status());
        assertEquals("tech_official", item.sellerName());
        assertEquals(SellerVerificationStatus.APPROVED, item.sellerVerificationStatus());
        assertEquals("Văn An Nguyễn", item.ownerName());
        assertEquals("an.nguyen@techstore.vn", item.ownerEmail());
        assertEquals("0987654321", item.ownerPhone());
        assertEquals(new BigDecimal("154200000.00"), item.balance());
        assertEquals(48L, item.productCount());
    }

    @Test
    @DisplayName("TC-02: Phân trang hợp lệ (Paging test)")
    void getShops_pagination_success() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId)))
                .thenReturn(List.of());

        adminShopService.getShops(null, null, 1, 5, "createdAt", "desc");

        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());
    }

    @Test
    @DisplayName("TC-03: Lọc theo trạng thái ACTIVE")
    void getShops_filterByStatus_active() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                null, ShopStatus.ACTIVE, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.content().get(0).status());
        verify(shopRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-04: Lọc theo trạng thái SUSPENDED")
    void getShops_filterByStatus_suspended() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                null, ShopStatus.SUSPENDED, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(ShopStatus.SUSPENDED, response.content().get(0).status());
    }

    @Test
    @DisplayName("TC-05: Tìm kiếm theo tên gian hàng (Shop Name)")
    void getShops_searchByKeyword_shopName() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                "tech", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().get(0).name().toLowerCase().contains("tech"));
    }

    @Test
    @DisplayName("TC-06: Tìm kiếm theo username Người bán (sellerName)")
    void getShops_searchByKeyword_sellerName() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                "tech_official", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals("tech_official", response.content().get(0).sellerName());
    }

    @Test
    @DisplayName("TC-07: Tìm kiếm theo email chủ sở hữu")
    void getShops_searchByKeyword_ownerEmail() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                "an.nguyen@techstore.vn", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals("an.nguyen@techstore.vn", response.content().get(0).ownerEmail());
    }

    @Test
    @DisplayName("TC-08: Tìm kiếm kết hợp vừa lọc status vừa keyword")
    void getShops_filterCombinedKeywordAndStatus() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                "official", ShopStatus.ACTIVE, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.content().get(0).status());
        verify(shopRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-09: Xử lý an toàn ký tự đặc biệt SQL trong keyword (%, _)")
    void getShops_keywordWithSpecialCharacters_escapedProperly() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        assertDoesNotThrow(() -> adminShopService.getShops(
                "100%_cotton", null, 0, 20, "createdAt", "desc"
        ));
    }

    @Test
    @DisplayName("TC-10: Sắp xếp theo tên Shop tăng dần")
    void getShops_sortByShopNameAsc() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        adminShopService.getShops(null, null, 0, 20, "name", "asc");

        Pageable captured = pageableCaptor.getValue();
        Sort.Order order = captured.getSort().getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("TC-11: Sắp xếp theo trạng thái Shop")
    void getShops_sortByStatus() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        adminShopService.getShops(null, null, 0, 20, "status", "desc");

        Pageable captured = pageableCaptor.getValue();
        Sort.Order order = captured.getSort().getOrderFor("status");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("TC-12: SortBy nằm ngoài whitelist -> Fallback về createdAt desc")
    void getShops_sortByInvalidField_fallbacksToCreatedAt() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        adminShopService.getShops(null, null, 0, 20, "hackedField;DROP TABLE", "desc");

        Pageable captured = pageableCaptor.getValue();
        Sort.Order order = captured.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("TC-13: Chuẩn hóa tham số page âm -> Chuyển về 0")
    void getShops_negativePage_normalizedToZero() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        adminShopService.getShops(null, null, -5, 20, "createdAt", "desc");

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
    }

    @Test
    @DisplayName("TC-14: Chuẩn hóa tham số size vượt biên (size=500 -> 100, size=-10 -> 1)")
    void getShops_sizeBounds_clampedProperly() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId))).thenReturn(List.of());

        // Test max clamp
        adminShopService.getShops(null, null, 0, 500, "createdAt", "desc");
        assertEquals(100, pageableCaptor.getValue().getPageSize());

        // Test min clamp
        adminShopService.getShops(null, null, 0, -10, "createdAt", "desc");
        assertEquals(1, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("TC-15: Đếm sản phẩm chính xác cho Shop")
    void getShops_countProducts_mappedCorrectly() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId)))
                .thenReturn(List.<Object[]>of(new Object[]{shopId, 15L}));

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(15L, response.content().get(0).productCount());
    }

    @Test
    @DisplayName("TC-16: Xử lý an toàn khi Shop chưa có sản phẩm (productCount = 0)")
    void getShops_shopWithoutProducts_productCountZero() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(shopId)))
                .thenReturn(List.of());

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(0L, response.content().get(0).productCount());
    }

    @Test
    @DisplayName("TC-17: Tìm kiếm không có kết quả phù hợp (Empty result)")
    void getShops_emptyResult_returnsEmptyPageResponseWithoutQueryingProducts() {
        Page<Shop> emptyPage = Page.empty();
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminShopResponse> response = adminShopService.getShops(
                "NonExistent", null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(productRepository, never()).countTotalProductsByShopIdIn(any());
    }

    @Test
    @DisplayName("TC-18: Null-Safety khi Shop không có Seller hoặc User")
    void getShops_nullSellerAndUser_nullSafeMapping() {
        Shop danglingShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Dangling Shop")
                .status(ShopStatus.PAUSED)
                .seller(null)
                .build();

        Page<Shop> page = new PageImpl<>(List.of(danglingShop));
        when(shopRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productRepository.countTotalProductsByShopIdIn(List.of(danglingShop.getId()))).thenReturn(List.of());

        PageResponse<AdminShopResponse> response = assertDoesNotThrow(() ->
                adminShopService.getShops(null, null, 0, 20, "createdAt", "desc")
        );

        assertNotNull(response);
        AdminShopResponse item = response.content().get(0);
        assertNull(item.sellerId());
        assertNull(item.sellerName());
        assertNull(item.ownerId());
        assertNull(item.ownerName());
        assertNull(item.ownerEmail());
        assertEquals(BigDecimal.ZERO, item.balance());
        assertEquals(0L, item.productCount());
    }

    // ==========================================
    // TESTS CHO GET /api/admin/shops/{id} (API 8)
    // ==========================================

    @Test
    @DisplayName("TC-19 (API-08): Lấy chi tiết Shop thành công đầy đủ thông tin (Happy path)")
    void getShopDetail_happyPath_success() {
        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleAddress));
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleShippingOption));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(48L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(42L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(320L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(new BigDecimal("890500000.00"));

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);

        // 1. Shop Commercial
        assertEquals(shopId, response.shop().id());
        assertEquals("Official Tech Store", response.shop().name());
        assertEquals(ShopStatus.ACTIVE, response.shop().status());

        // 2. Seller Profile
        assertNotNull(response.seller());
        assertEquals("tech_official", response.seller().sellerName());
        assertEquals(SellerVerificationStatus.APPROVED, response.seller().verificationStatus());
        assertEquals("Vietcombank", response.seller().bankName());
        assertEquals("0011001234567", response.seller().bankAccountNumber());
        assertEquals(new BigDecimal("154200000.00"), response.seller().balance());

        // 3. Owner Account
        assertNotNull(response.owner());
        assertEquals("Văn An Nguyễn", response.owner().fullName());
        assertEquals("an.nguyen@techstore.vn", response.owner().email());
        assertEquals("0987654321", response.owner().phone());

        // 4. Warehouse Address
        assertNotNull(response.warehouseAddress());
        assertEquals("Nguyễn Văn An", response.warehouseAddress().contactName());
        assertEquals("Thành phố Hà Nội", response.warehouseAddress().province());
        assertEquals("Số 12 Duy Tân, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Thành phố Hà Nội",
                response.warehouseAddress().fullAddress());

        // 5. Shipping Option
        assertNotNull(response.shippingOption());
        assertEquals("Standard Delivery", response.shippingOption().name());
        assertEquals(new BigDecimal("25000.00"), response.shippingOption().shippingFee());
        assertTrue(response.shippingOption().isActive());

        // 6. Metrics
        assertNotNull(response.metrics());
        assertEquals(48L, response.metrics().totalProducts());
        assertEquals(42L, response.metrics().activeProducts());
        assertEquals(320L, response.metrics().totalCompletedOrders());
        assertEquals(new BigDecimal("890500000.00"), response.metrics().cumulativeRevenue());
    }

    @Test
    @DisplayName("TC-20 (API-08): Lấy chi tiết Shop mới chưa có kho hàng (warehouseAddress = null)")
    void getShopDetail_noWarehouseAddress_success() {
        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleShippingOption));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(0L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(0L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(0L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(BigDecimal.ZERO);

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);
        assertNull(response.warehouseAddress());
        assertNotNull(response.shippingOption());
        assertEquals(0L, response.metrics().totalProducts());
    }

    @Test
    @DisplayName("TC-21 (API-08): Lấy chi tiết Shop chưa có phương thức vận chuyển (shippingOption = null)")
    void getShopDetail_noShippingOption_success() {
        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleAddress));
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(5L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(5L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(1L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(new BigDecimal("50000.00"));

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);
        assertNotNull(response.warehouseAddress());
        assertNull(response.shippingOption());
    }

    @Test
    @DisplayName("TC-22 (API-08): Lấy chi tiết Shop chưa có đơn hàng hoàn tất (completedOrders = 0, revenue = 0)")
    void getShopDetail_noOrders_success() {
        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleAddress));
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleShippingOption));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(10L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(10L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(0L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(null);

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);
        assertEquals(0L, response.metrics().totalCompletedOrders());
        assertEquals(BigDecimal.ZERO, response.metrics().cumulativeRevenue());
    }

    @Test
    @DisplayName("TC-23 (API-08): Shop không tồn tại trong CSDL -> Ném SHOP_NOT_FOUND (404)")
    void getShopDetail_shopNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(shopRepository.findWithDetailsById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.getShopDetail(nonExistentId)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-24 (API-08): ID truyền vào là null -> Ném SHOP_NOT_FOUND (404)")
    void getShopDetail_nullId_throwsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.getShopDetail(null)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-25 (API-08): Null-Safety khi Shop thiếu liên kết Seller hoặc User")
    void getShopDetail_nullSellerAndUser_success() {
        Shop danglingShop = Shop.builder()
                .id(shopId)
                .name("Dangling Shop")
                .status(ShopStatus.SUSPENDED)
                .seller(null)
                .build();

        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(danglingShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(0L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(0L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(0L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(BigDecimal.ZERO);

        AdminShopDetailResponse response = assertDoesNotThrow(() ->
                adminShopService.getShopDetail(shopId)
        );

        assertNotNull(response);
        assertNull(response.seller());
        assertNull(response.owner());
    }

    @Test
    @DisplayName("TC-26 (API-08): Ghép họ tên khi User chỉ có một phần tên (chỉ firstName hoặc chỉ lastName)")
    void getShopDetail_partialNameFormatting_success() {
        sampleUser.setFirstName("Văn An");
        sampleUser.setLastName(null);

        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(0L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(0L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(0L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(BigDecimal.ZERO);

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);
        assertEquals("Văn An", response.owner().fullName());
    }

    @Test
    @DisplayName("TC-27 (API-08): Ghép địa chỉ kho khi một số trường bị null (bỏ qua phần tử null)")
    void getShopDetail_partialAddressFormatting_success() {
        sampleAddress.setStreetAddress(null);
        sampleAddress.setWard(null);

        when(shopRepository.findWithDetailsById(shopId)).thenReturn(Optional.of(sampleShop));
        when(addressRepository.findByShopId(shopId)).thenReturn(Optional.of(sampleAddress));
        when(shippingOptionRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(0L);
        when(productRepository.countActiveProductsByShopId(shopId)).thenReturn(0L);
        when(orderRepository.countCompletedOrdersByShopId(shopId)).thenReturn(0L);
        when(orderRepository.calculateCumulativeRevenueByShopId(shopId)).thenReturn(BigDecimal.ZERO);

        AdminShopDetailResponse response = adminShopService.getShopDetail(shopId);

        assertNotNull(response);
        assertEquals("Quận Cầu Giấy, Thành phố Hà Nội", response.warehouseAddress().fullAddress());
    }

    // =========================================================================
    // API-09: PATCH /api/admin/shops/{id}/status - Moderate Shop Status Tests
    // =========================================================================

    @Test
    @DisplayName("TC-28 (API-09): Admin chuyển Shop từ ACTIVE sang SUSPENDED thành công kèm lý do")
    void updateShopStatus_activeToSuspended_success() {
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Tạm khóa kiểm tra nghi vấn vi phạm"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(25L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.SUSPENDED, response.status());
        assertEquals(25L, response.productCount());
        verify(shopRepository).save(sampleShop);
        assertEquals(ShopStatus.SUSPENDED, sampleShop.getStatus());
    }

    @Test
    @DisplayName("TC-29 (API-09): Admin chuyển Shop từ ACTIVE sang BANNED thành công kèm lý do")
    void updateShopStatus_activeToBanned_success() {
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.BANNED,
                "Cấm vĩnh viễn do lừa đảo"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(10L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.BANNED, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-30 (API-09): Admin phục hồi Shop từ SUSPENDED về ACTIVE thành công khi Seller đã APPROVED")
    void updateShopStatus_suspendedToActive_success() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        sampleSeller.setVerificationStatus(SellerVerificationStatus.APPROVED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                "Kháng cáo thành công"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(30L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-31 (API-09): Admin nâng mức kỷ luật Shop từ SUSPENDED sang BANNED")
    void updateShopStatus_suspendedToBanned_success() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.BANNED,
                "Kết luận thanh tra: Cấm vĩnh viễn"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(5L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.BANNED, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-32 (API-09): Admin chuyển Shop từ ACTIVE sang PAUSED")
    void updateShopStatus_activeToPaused_success() {
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.PAUSED,
                "Hỗ trợ kiểm kê kho hàng"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(15L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.PAUSED, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-33 (API-09): Admin chuyển Shop từ PAUSED về ACTIVE")
    void updateShopStatus_pausedToActive_success() {
        sampleShop.setStatus(ShopStatus.PAUSED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                null
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(15L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-34 (API-09): Admin chuyển Shop từ PAUSED sang SUSPENDED")
    void updateShopStatus_pausedToSuspended_success() {
        sampleShop.setStatus(ShopStatus.PAUSED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Phát hiện vi phạm khi đang tạm nghỉ"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(15L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.SUSPENDED, response.status());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    @DisplayName("TC-35 (API-09): Idempotency khi cập nhật trạng thái trùng với trạng thái hiện tại (ACTIVE -> ACTIVE)")
    void updateShopStatus_identicalStatus_returnsImmediatelyWithoutSave() {
        sampleShop.setStatus(ShopStatus.ACTIVE);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                "Không có thay đổi"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(20L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.status());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-36 (API-09): Idempotency khi cập nhật trạng thái trùng với trạng thái hiện tại (SUSPENDED -> SUSPENDED)")
    void updateShopStatus_identicalSuspended_returnsImmediatelyWithoutSave() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Tiếp tục khóa"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));
        when(productRepository.countByShopIdAndIsDeletedFalse(shopId)).thenReturn(20L);

        AdminShopResponse response = adminShopService.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertEquals(ShopStatus.SUSPENDED, response.status());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-37 (API-09): Chặn chuyển đổi trạng thái khi Shop đã bị BANNED (BANNED -> ACTIVE)")
    void updateShopStatus_bannedToActive_throwsException() {
        sampleShop.setStatus(ShopStatus.BANNED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                "Cố gắng unban"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, request)
        );

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-38 (API-09): Chặn chuyển đổi trạng thái khi Shop đã bị BANNED (BANNED -> SUSPENDED)")
    void updateShopStatus_bannedToSuspended_throwsException() {
        sampleShop.setStatus(ShopStatus.BANNED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Cố gắng hạ mức phạt"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, request)
        );

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-39 (API-09): Chặn kích hoạt ACTIVE khi Seller đang PENDING duyệt hồ sơ")
    void updateShopStatus_sellerPending_cannotActivate() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        sampleSeller.setVerificationStatus(SellerVerificationStatus.PENDING);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                "Kích hoạt shop"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, request)
        );

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-40 (API-09): Chặn kích hoạt ACTIVE khi Seller bị REJECTED hồ sơ")
    void updateShopStatus_sellerRejected_cannotActivate() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        sampleSeller.setVerificationStatus(SellerVerificationStatus.REJECTED);

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.ACTIVE,
                "Kích hoạt shop"
        );

        when(shopRepository.findByIdForUpdate(shopId)).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, request)
        );

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-41 (API-09): Shop ID không tồn tại trong CSDL -> Ném SHOP_NOT_FOUND (404)")
    void updateShopStatus_shopNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Tạm khóa"
        );

        when(shopRepository.findByIdForUpdate(nonExistentId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(nonExistentId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-42 (API-09): Shop ID truyền vào là null -> Ném SHOP_NOT_FOUND (404)")
    void updateShopStatus_nullId_throwsException() {
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Tạm khóa"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(null, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("TC-43 (API-09): Request là null hoặc status là null -> Ném INVALID_SHOP_STATUS_TRANSITION (400)")
    void updateShopStatus_nullRequestOrStatus_throwsException() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, null)
        );
        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex1.getErrorCode());

        AdminUpdateShopStatusRequest nullStatusRequest = new AdminUpdateShopStatusRequest(null, "Lý do");
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminShopService.updateShopStatus(shopId, nullStatusRequest)
        );
        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex2.getErrorCode());

        verify(shopRepository, never()).findByIdForUpdate(any());
        verify(shopRepository, never()).save(any(Shop.class));
    }
}
