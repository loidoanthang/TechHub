package com.techhub.service.wishlist;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.WishlistResponse;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.WishlistItem;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.repository.WishlistRepository;
import com.techhub.service.wishlist.impl.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho WishlistService (POST /api/wishlist/{productId})")
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private UUID userId;
    private UUID productId;
    private Shop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        shop = Shop.builder()
                .id(UUID.randomUUID())
                .name("TechZone Official")
                .status(ShopStatus.ACTIVE)
                .build();

        product = Product.builder()
                .id(productId)
                .name("Bàn phím cơ không dây Keychron K2 Pro")
                .slug("ban-phim-co-keychron-k2-pro-a1b2c3")
                .shop(shop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("TC-01: Thêm sản phẩm vào Wishlist thành công khi hợp lệ")
    void testAddToWishlist_Success_NewItem() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.countByUserId(userId)).thenReturn(5L);

        UUID wishlistItemId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        WishlistItem savedItem = WishlistItem.builder()
                .id(wishlistItemId)
                .userId(userId)
                .product(product)
                .createdAt(now)
                .build();

        when(wishlistRepository.save(any(WishlistItem.class))).thenReturn(savedItem);

        // Mock metrics: [productId, count, minPrice, maxPrice, sumStock]
        List<Object[]> metrics = Collections.singletonList(new Object[]{
                productId, 2L, new BigDecimal("1890000.00"), new BigDecimal("2190000.00"), 25L
        });
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(metrics);

        // Mock cover image: [productId, imageUrl]
        List<Object[]> coverImages = Collections.singletonList(new Object[]{
                productId, "https://cdn.techhub.com/k2pro-cover.jpg"
        });
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(coverImages);

        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        assertEquals(wishlistItemId, response.getId());
        assertEquals(productId, response.getProductId());
        assertEquals("Bàn phím cơ không dây Keychron K2 Pro", response.getProductName());
        assertEquals("ban-phim-co-keychron-k2-pro-a1b2c3", response.getSlug());
        assertEquals("https://cdn.techhub.com/k2pro-cover.jpg", response.getCoverImageUrl());
        assertEquals(new BigDecimal("1890000.00"), response.getMinPrice());
        assertTrue(response.getInStock());
        assertFalse(response.getIsShopPaused());
        assertEquals(now, response.getCreatedAt());

        verify(wishlistRepository, times(1)).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("TC-02: Tính lũy thừa khi sản phẩm đã có sẵn trong Wishlist (Idempotency)")
    void testAddToWishlist_Idempotent_AlreadyExists() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UUID existingId = UUID.randomUUID();
        LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(2);
        WishlistItem existingItem = WishlistItem.builder()
                .id(existingId)
                .userId(userId)
                .product(product)
                .createdAt(existingCreatedAt)
                .build();

        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingItem));
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        assertEquals(existingId, response.getId());
        assertEquals(productId, response.getProductId());
        assertEquals(existingCreatedAt, response.getCreatedAt());

        // Đảm bảo không gọi count và không gọi save
        verify(wishlistRepository, never()).countByUserId(any());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Thất bại khi Wishlist đã đạt tối đa 20 sản phẩm (BR-05B)")
    void testAddToWishlist_LimitExceeded_Exactly20() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.countByUserId(userId)).thenReturn(20L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.WISHLIST_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Thành công tại giá trị biên 19 lên 20 sản phẩm")
    void testAddToWishlist_Boundary_19To20() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.countByUserId(userId)).thenReturn(19L);

        WishlistItem savedItem = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        when(wishlistRepository.save(any())).thenReturn(savedItem);
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        verify(wishlistRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-05: Thất bại khi sản phẩm không tồn tại")
    void testAddToWishlist_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(wishlistRepository, never()).findByUserIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("TC-06: Thất bại khi sản phẩm đã bị xóa mềm (isDeleted = true)")
    void testAddToWishlist_ProductSoftDeleted() {
        product.setDeleted(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(wishlistRepository, never()).findByUserIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("TC-07: Thất bại khi sản phẩm chưa được duyệt (DRAFT)")
    void testAddToWishlist_ProductNotApproved_Draft() {
        product.setApprovalStatus(ProductApprovalStatus.DRAFT);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08: Thất bại khi sản phẩm bị Admin đình chỉ (isSuspended = true)")
    void testAddToWishlist_ProductSuspendedByAdmin() {
        product.setSuspended(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_SUSPENDED, ex.getErrorCode());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09: Thất bại khi Shop sở hữu bị khóa vĩnh viễn (BANNED)")
    void testAddToWishlist_ShopBanned() {
        shop.setStatus(ShopStatus.BANNED);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                wishlistService.addToWishlist(userId, productId)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10: Vẫn cho phép thêm khi Shop đang tạm nghỉ (PAUSED), cờ isShopPaused = true")
    void testAddToWishlist_ShopPaused_Allowed() {
        shop.setStatus(ShopStatus.PAUSED);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.countByUserId(userId)).thenReturn(3L);

        WishlistItem savedItem = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        when(wishlistRepository.save(any())).thenReturn(savedItem);
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        assertTrue(response.getIsShopPaused(), "Cờ isShopPaused phải là true khi shop tạm nghỉ");
    }

    @Test
    @DisplayName("TC-11: Xử lý an toàn khi sản phẩm chưa có biến thể active hoặc chưa có ảnh cover")
    void testAddToWishlist_ProductWithNoVariantsOrImages() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.countByUserId(userId)).thenReturn(0L);

        WishlistItem savedItem = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        when(wishlistRepository.save(any())).thenReturn(savedItem);

        // Trả về rỗng cho variants và images
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getMinPrice());
        assertFalse(response.getInStock());
        assertNull(response.getCoverImageUrl());
    }

    @Test
    @DisplayName("TC-12: Khôi phục mượt mà khi xảy ra Race Condition vi phạm Unique Constraint")
    void testAddToWishlist_ConcurrentDuplicate_RaceCondition() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        // Lần đầu check thấy chưa có
        when(wishlistRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.empty()) // Lần 1: check trước save
                .thenReturn(Optional.of(WishlistItem.builder() // Lần 2: fallback sau khi dính race condition
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .product(product)
                        .createdAt(LocalDateTime.now())
                        .build()));

        when(wishlistRepository.countByUserId(userId)).thenReturn(5L);

        // Bắn lỗi đụng độ Unique Key từ Database
        when(wishlistRepository.save(any())).thenThrow(new DataIntegrityViolationException("Unique constraint violation uk_wishlist_user_product"));

        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        // Phải xử lý êm đẹp, không để văng lỗi 500 ra ngoài
        WishlistResponse response = wishlistService.addToWishlist(userId, productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        verify(wishlistRepository, times(2)).findByUserIdAndProductId(userId, productId);
    }

    // =========================================================================
    // TESTS FOR DELETE /api/wishlist/{productId} (removeFromWishlist)
    // =========================================================================

    @Test
    @DisplayName("Case 1 (Remove): Xóa thành công sản phẩm đang tồn tại trong Wishlist")
    void testRemoveFromWishlist_Success_ExistingItem() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(1);

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(userId, productId));

        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Case 2 (Remove): Xóa lũy thừa khi sản phẩm không tồn tại trong Wishlist (không ném lỗi)")
    void testRemoveFromWishlist_Idempotent_ItemNotExists() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(0);

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(userId, productId));

        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Case 3 (Remove): Gọi xóa lặp lại nhiều lần (Repeated / Retry Call) vẫn an toàn")
    void testRemoveFromWishlist_ItemAlreadyDeleted_RepeatedCall() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(0);

        assertDoesNotThrow(() -> {
            wishlistService.removeFromWishlist(userId, productId);
            wishlistService.removeFromWishlist(userId, productId);
        });

        verify(wishlistRepository, times(2)).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Case 4 (Remove): Cho phép xóa ngay cả khi sản phẩm đã bị xóa mềm (is_deleted = true)")
    void testRemoveFromWishlist_ProductIsSoftDeleted() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(1);

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(userId, productId));

        // Không áp dụng Gatekeeper hay nạp Product entity khi xóa
        verify(productRepository, never()).findById(any());
        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Case 5 (Remove): Cho phép xóa khi Shop sở hữu bị cấm (BANNED)")
    void testRemoveFromWishlist_ProductShopIsBanned() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(1);

        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(userId, productId));

        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Case 6 (Remove): Chống IDOR - Chỉ xóa đúng userId của Buyer, không ảnh hưởng user khác")
    void testRemoveFromWishlist_IDOR_CannotDeleteOtherUserItem() {
        UUID otherUserId = UUID.randomUUID();
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(1);

        wishlistService.removeFromWishlist(userId, productId);

        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
        verify(wishlistRepository, never()).deleteByUserIdAndProductId(eq(otherUserId), any());
    }

    @Test
    @DisplayName("Case 7 (Remove): Fast-fail ném INVALID_INPUT_DATA khi productId null")
    void testRemoveFromWishlist_NullProductId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> wishlistService.removeFromWishlist(userId, null));

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
        verify(wishlistRepository, never()).deleteByUserIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("Case 8 (Remove): Đảm bảo thực thi nguyên tử 1 câu query duy nhất (Atomic Execution)")
    void testRemoveFromWishlist_AtomicQueryExecution() {
        when(wishlistRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(1);

        wishlistService.removeFromWishlist(userId, productId);

        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
        verifyNoMoreInteractions(wishlistRepository);
        verifyNoInteractions(productRepository, productVariantRepository, productImageRepository);
    }

    // =========================================================================
    // TESTS FOR GET /api/wishlist (getWishlist)
    // =========================================================================

    @Test
    @DisplayName("Case 1 (Get): Lấy danh sách thành công trang đầu tiên (Page 0) có phân trang và map DTO")
    void testGetWishlist_Success_FirstPage() {
        WishlistItem item1 = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        Product prod2 = Product.builder()
                .id(UUID.randomUUID())
                .name("Chuột Logitech MX Master 3S")
                .slug("chuot-logitech-mx-master-3s")
                .shop(shop)
                .build();

        WishlistItem item2 = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(prod2)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<WishlistItem> mockPage = new PageImpl<>(List.of(item1, item2), pageable, 2);

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(mockPage);

        // Mock batch metrics cho 2 products
        Object[] metric1 = new Object[]{productId, 2L, new BigDecimal("1890000.00"), new BigDecimal("2190000.00"), 15L};
        Object[] metric2 = new Object[]{prod2.getId(), 1L, new BigDecimal("2490000.00"), new BigDecimal("2490000.00"), 0L}; // hết hàng
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId, prod2.getId())))
                .thenReturn(List.of(metric1, metric2));

        // Mock batch cover images
        Object[] img1 = new Object[]{productId, "https://cdn.techhub.com/cover1.jpg"};
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId, prod2.getId())))
                .thenReturn(Collections.singletonList(img1));

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 0, 10);

        assertNotNull(response);
        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(2, response.content().size());

        // Kiểm tra item 1: còn hàng, có ảnh cover
        WishlistResponse res1 = response.content().get(0);
        assertEquals(productId, res1.getProductId());
        assertEquals("Bàn phím cơ không dây Keychron K2 Pro", res1.getProductName());
        assertEquals(new BigDecimal("1890000.00"), res1.getMinPrice());
        assertTrue(res1.getInStock());
        assertEquals("https://cdn.techhub.com/cover1.jpg", res1.getCoverImageUrl());
        assertFalse(res1.getIsShopPaused());

        // Kiểm tra item 2: hết hàng, không có ảnh cover
        WishlistResponse res2 = response.content().get(1);
        assertEquals(prod2.getId(), res2.getProductId());
        assertEquals("Chuột Logitech MX Master 3S", res2.getProductName());
        assertEquals(new BigDecimal("2490000.00"), res2.getMinPrice());
        assertFalse(res2.getInStock());
        assertNull(res2.getCoverImageUrl());
    }

    @Test
    @DisplayName("Case 2 (Get): Lấy danh sách trang 2 (Page 1) khi có tổng cộng 15 sản phẩm")
    void testGetWishlist_Success_Pagination_PageTwo() {
        WishlistItem item = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(1, 10);
        Page<WishlistItem> mockPage = new PageImpl<>(List.of(item), pageable, 11);

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(mockPage);
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId))).thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId))).thenReturn(Collections.emptyList());

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 1, 10);

        assertNotNull(response);
        assertEquals(1, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertEquals(11, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals(1, response.content().size());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("Case 3 (Get): Wishlist rỗng (0 món) trả về PageResponse rỗng và không gọi batch queries")
    void testGetWishlist_EmptyWishlist() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WishlistItem> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(emptyPage);

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 0, 10);

        assertNotNull(response);
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.content().isEmpty());

        // Đảm bảo không gọi truy vấn phụ khi danh sách rỗng
        verifyNoInteractions(productVariantRepository, productImageRepository);
    }

    @Test
    @DisplayName("Case 4 (Get): Yêu cầu số trang vượt quá tổng số trang (Page out of bounds) trả về list rỗng")
    void testGetWishlist_PageOutOfBounds_ReturnsEmptyList() {
        Pageable pageable = PageRequest.of(5, 10);
        Page<WishlistItem> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 3);

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(emptyPage);

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 5, 10);

        assertNotNull(response);
        assertEquals(5, response.pageNumber());
        assertEquals(3, response.totalElements());
        assertTrue(response.content().isEmpty());

        verifyNoInteractions(productVariantRepository, productImageRepository);
    }

    @Test
    @DisplayName("Case 5 (Get): Sản phẩm không có biến thể active fallback minPrice = 0.00 và inStock = false")
    void testGetWishlist_ProductWithNoActiveVariants() {
        WishlistItem item = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        // Mock trả về rỗng từ variant repository
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        WishlistResponse res = response.content().get(0);
        assertEquals(BigDecimal.ZERO, res.getMinPrice());
        assertFalse(res.getInStock());
    }

    @Test
    @DisplayName("Case 6 (Get): Sản phẩm không có ảnh cover trả về coverImageUrl = null an toàn")
    void testGetWishlist_ProductWithNoCoverImage() {
        WishlistItem item = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 0, 10);

        assertNotNull(response);
        assertNull(response.content().get(0).getCoverImageUrl());
    }

    @Test
    @DisplayName("Case 7 (Get): Shop có trạng thái PAUSED trả về isShopPaused = true")
    void testGetWishlist_ProductWithShopPaused() {
        Shop pausedShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Tech Store Nghỉ Lễ")
                .status(ShopStatus.PAUSED)
                .build();

        Product prodWithPausedShop = Product.builder()
                .id(UUID.randomUUID())
                .name("RAM Corsair Vengeance 32GB")
                .slug("ram-corsair-vengeance-32gb")
                .shop(pausedShop)
                .build();

        WishlistItem item = WishlistItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(prodWithPausedShop)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(prodWithPausedShop.getId())))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(prodWithPausedShop.getId())))
                .thenReturn(Collections.emptyList());

        PageResponse<WishlistResponse> response = wishlistService.getWishlist(userId, 0, 10);

        assertNotNull(response);
        assertTrue(response.content().get(0).getIsShopPaused());
    }

    @Test
    @DisplayName("Case 8 (Get): Batch queries cho metrics và ảnh được gọi đúng 1 lần cho cả trang (Zero N+1)")
    void testGetWishlist_BatchQueriesExecutedOnce() {
        Product p1 = Product.builder().id(UUID.randomUUID()).name("P1").slug("p1").shop(shop).build();
        Product p2 = Product.builder().id(UUID.randomUUID()).name("P2").slug("p2").shop(shop).build();
        Product p3 = Product.builder().id(UUID.randomUUID()).name("P3").slug("p3").shop(shop).build();

        WishlistItem item1 = WishlistItem.builder().id(UUID.randomUUID()).userId(userId).product(p1).build();
        WishlistItem item2 = WishlistItem.builder().id(UUID.randomUUID()).userId(userId).product(p2).build();
        WishlistItem item3 = WishlistItem.builder().id(UUID.randomUUID()).userId(userId).product(p3).build();

        Pageable pageable = PageRequest.of(0, 10);
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(item1, item2, item3), pageable, 3));

        List<UUID> expectedIds = List.of(p1.getId(), p2.getId(), p3.getId());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(expectedIds))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(expectedIds))
                .thenReturn(Collections.emptyList());

        wishlistService.getWishlist(userId, 0, 10);

        // Xác minh chỉ bắn đúng 1 query gom lô cho cả 3 sản phẩm (Zero N+1)
        verify(productVariantRepository, times(1)).aggregateActiveVariantMetricsByProductIds(expectedIds);
        verify(productImageRepository, times(1)).findCoverImagesByProductIds(expectedIds);
    }
}
