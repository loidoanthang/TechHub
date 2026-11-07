package com.techhub.service.recentlyviewed;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.RecentlyViewedResponse;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.RecentlyViewedProduct;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.repository.RecentlyViewedProductRepository;
import com.techhub.service.recentlyviewed.impl.RecentlyViewedServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho RecentlyViewedServiceImpl (POST /api/products/{id}/view, GET /api/products/recently-viewed)")
class RecentlyViewedServiceImplTest {

    @Mock
    private RecentlyViewedProductRepository recentlyViewedRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private RecentlyViewedServiceImpl recentlyViewedService;

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
                .name("Keychron Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        product = Product.builder()
                .id(productId)
                .name("Bàn phím cơ Keychron K2 Pro")
                .slug("ban-phim-co-keychron-k2-pro")
                .shop(shop)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("TC-01: Ghi nhận lượt xem thành công cho sản phẩm hợp lệ chưa từng xem (Dưới ngưỡng 10)")
    void recordProductView_Success_NewProduct_UnderCapacity() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.upsertProductView(any(UUID.class), eq(userId), eq(productId))).thenReturn(1);
        when(recentlyViewedRepository.evictExcessRecentlyViewed(userId)).thenReturn(0);

        assertDoesNotThrow(() -> recentlyViewedService.recordProductView(userId, productId));

        verify(productRepository, times(1)).findById(productId);
        verify(recentlyViewedRepository, times(1)).upsertProductView(any(UUID.class), eq(userId), eq(productId));
        verify(recentlyViewedRepository, times(1)).evictExcessRecentlyViewed(userId);
    }

    @Test
    @DisplayName("TC-02: Ghi nhận lượt xem thành công khi xem lại sản phẩm đã có (Cập nhật viewed_at qua Upsert)")
    void recordProductView_Success_ExistingProduct_UpdatesTimestamp() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.upsertProductView(any(UUID.class), eq(userId), eq(productId))).thenReturn(1);
        when(recentlyViewedRepository.evictExcessRecentlyViewed(userId)).thenReturn(0);

        assertDoesNotThrow(() -> recentlyViewedService.recordProductView(userId, productId));

        verify(recentlyViewedRepository, times(1)).upsertProductView(any(UUID.class), eq(userId), eq(productId));
        verify(recentlyViewedRepository, times(1)).evictExcessRecentlyViewed(userId);
    }

    @Test
    @DisplayName("TC-03: Ghi nhận lượt xem thành công và kích hoạt FIFO Eviction khi vượt quá 10 sản phẩm")
    void recordProductView_Success_EvictsOldest_WhenExceedsCapacity() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.upsertProductView(any(UUID.class), eq(userId), eq(productId))).thenReturn(1);
        when(recentlyViewedRepository.evictExcessRecentlyViewed(userId)).thenReturn(1); // 1 bản ghi cũ nhất bị xóa

        assertDoesNotThrow(() -> recentlyViewedService.recordProductView(userId, productId));

        verify(recentlyViewedRepository, times(1)).upsertProductView(any(UUID.class), eq(userId), eq(productId));
        verify(recentlyViewedRepository, times(1)).evictExcessRecentlyViewed(userId);
    }

    @Test
    @DisplayName("TC-04: Ghi nhận lượt xem thành công với sản phẩm không gán shop (shop null)")
    void recordProductView_Success_ProductWithNullShop() {
        product.setShop(null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.upsertProductView(any(UUID.class), eq(userId), eq(productId))).thenReturn(1);

        assertDoesNotThrow(() -> recentlyViewedService.recordProductView(userId, productId));

        verify(recentlyViewedRepository, times(1)).upsertProductView(any(UUID.class), eq(userId), eq(productId));
        verify(recentlyViewedRepository, times(1)).evictExcessRecentlyViewed(userId);
    }

    @Test
    @DisplayName("TC-05: Ném PRODUCT_NOT_FOUND khi sản phẩm không tồn tại trong DB")
    void recordProductView_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-06: Ném PRODUCT_NOT_FOUND khi sản phẩm đã bị xóa mềm (is_deleted = true)")
    void recordProductView_ProductSoftDeleted() {
        product.setDeleted(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-07: Ném PRODUCT_NOT_FOUND khi trạng thái hiển thị không phải PUBLISHED (ví dụ UNPUBLISHED)")
    void recordProductView_ProductNotPublished() {
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-08: Ném PRODUCT_NOT_APPROVED khi sản phẩm chưa được kiểm duyệt (PENDING_APPROVAL / DRAFT / REJECTED)")
    void recordProductView_ProductNotApproved() {
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-09: Ném PRODUCT_SUSPENDED khi sản phẩm bị đình chỉ kinh doanh")
    void recordProductView_ProductSuspended() {
        product.setSuspended(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.PRODUCT_SUSPENDED, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-10: Ném SHOP_NOT_ACTIVE khi Shop sở hữu sản phẩm bị cấm vĩnh viễn (BANNED)")
    void recordProductView_ShopBanned() {
        shop.setStatus(ShopStatus.BANNED);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
        verify(recentlyViewedRepository, never()).evictExcessRecentlyViewed(any());
    }

    @Test
    @DisplayName("TC-11: Fast-fail ném INVALID_INPUT_DATA khi productId null hoặc Nil UUID 00000000-0000-0000-0000-000000000000")
    void recordProductView_FastFail_NullOrNilUUID() {
        // Test null
        BusinessException exNull = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, null));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, exNull.getErrorCode());

        // Test Nil UUID
        UUID nilUUID = new UUID(0L, 0L);
        BusinessException exNil = assertThrows(BusinessException.class,
                () -> recentlyViewedService.recordProductView(userId, nilUUID));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, exNil.getErrorCode());

        verify(productRepository, never()).findById(any());
        verify(recentlyViewedRepository, never()).upsertProductView(any(), any(), any());
    }

    // =========================================================================
    // TESTS FOR GET /api/products/recently-viewed
    // =========================================================================

    @Test
    @DisplayName("TC-01: Lấy danh sách sản phẩm đã xem thành công khi có dưới 10 sản phẩm")
    void getRecentlyViewed_Success_UnderTenItems() {
        RecentlyViewedProduct item1 = RecentlyViewedProduct.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .viewedAt(LocalDateTime.now())
                .build();

        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(item1));

        // Mock batch metrics: [productId, maxPrice, minPrice, avgPrice, totalStock]
        Object[] metric = new Object[]{productId, new BigDecimal("2500000.00"), new BigDecimal("1890000.00"), new BigDecimal("2000000.00"), 15L};
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.singletonList(metric));

        // Mock batch cover image: [productId, imageUrl]
        Object[] img = new Object[]{productId, "https://cdn.techhub.com/cover.jpg"};
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.singletonList(img));

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        RecentlyViewedResponse res = result.get(0);
        assertEquals(productId, res.getProductId());
        assertEquals("Bàn phím cơ Keychron K2 Pro", res.getProductName());
        assertEquals(new BigDecimal("1890000.00"), res.getMinPrice());
        assertTrue(res.getInStock());
        assertFalse(res.getIsShopPaused());
        assertEquals("https://cdn.techhub.com/cover.jpg", res.getCoverImageUrl());
    }

    @Test
    @DisplayName("TC-02: Lấy danh sách sản phẩm đã xem thành công khi đủ 10 sản phẩm, đúng thứ tự xem")
    void getRecentlyViewed_Success_FullTenItems_Ordered() {
        LocalDateTime baseTime = LocalDateTime.now();
        List<RecentlyViewedProduct> items = new java.util.ArrayList<>();
        List<UUID> productIds = new java.util.ArrayList<>();

        for (int i = 0; i < 10; i++) {
            UUID pId = UUID.randomUUID();
            productIds.add(pId);
            Product p = Product.builder()
                    .id(pId)
                    .name("Product " + i)
                    .slug("product-" + i)
                    .shop(shop)
                    .build();
            items.add(RecentlyViewedProduct.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .product(p)
                    .viewedAt(baseTime.minusMinutes(i))
                    .build());
        }

        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(items);
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(productIds))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(productIds))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertEquals(10, result.size());
        assertEquals("Product 0", result.get(0).getProductName());
        assertEquals("Product 9", result.get(9).getProductName());
    }

    @Test
    @DisplayName("TC-03: Trả về danh sách rỗng khi user chưa xem sản phẩm nào (Thoát sớm không query phụ)")
    void getRecentlyViewed_EmptyList_ReturnsEmpty() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productVariantRepository, never()).aggregateActiveVariantMetricsByProductIds(any());
        verify(productImageRepository, never()).findCoverImagesByProductIds(any());
    }

    @Test
    @DisplayName("TC-04: Lọc bỏ sản phẩm bị xóa mềm (Được loại trừ ở tầng repository query)")
    void getRecentlyViewed_FiltersOut_SoftDeletedProduct() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(recentlyViewedRepository).findActiveRecentlyViewedByUserId(eq(userId), any());
    }

    @Test
    @DisplayName("TC-05: Lọc bỏ sản phẩm chưa xuất bản UNPUBLISHED (Được loại trừ ở tầng repository query)")
    void getRecentlyViewed_FiltersOut_UnpublishedProduct() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-06: Lọc bỏ sản phẩm chưa kiểm duyệt PENDING_APPROVAL (Được loại trừ ở tầng repository query)")
    void getRecentlyViewed_FiltersOut_NotApprovedProduct() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-07: Lọc bỏ sản phẩm bị đình chỉ isSuspended (Được loại trừ ở tầng repository query)")
    void getRecentlyViewed_FiltersOut_SuspendedProduct() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-08: Lọc bỏ sản phẩm của shop bị cấm BANNED (Được loại trừ ở tầng repository query)")
    void getRecentlyViewed_FiltersOut_BannedShop() {
        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-09: Fallback an toàn khi sản phẩm không có biến thể hoạt động (minPrice = 0.00, inStock = false)")
    void getRecentlyViewed_FallbackMetrics_WhenNoActiveVariants() {
        RecentlyViewedProduct item = RecentlyViewedProduct.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .viewedAt(LocalDateTime.now())
                .build();

        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(item));
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).getMinPrice());
        assertFalse(result.get(0).getInStock());
    }

    @Test
    @DisplayName("TC-10: Fallback an toàn khi sản phẩm không có ảnh đại diện (coverImageUrl = null)")
    void getRecentlyViewed_FallbackImage_WhenNoCoverImage() {
        RecentlyViewedProduct item = RecentlyViewedProduct.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .viewedAt(LocalDateTime.now())
                .build();

        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(item));
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getCoverImageUrl());
    }

    @Test
    @DisplayName("TC-11: Cảnh báo Shop tạm nghỉ bán được phản ánh chính xác (isShopPaused = true)")
    void getRecentlyViewed_ShopPaused_ReflectedInResponse() {
        shop.setStatus(ShopStatus.PAUSED);
        RecentlyViewedProduct item = RecentlyViewedProduct.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .product(product)
                .viewedAt(LocalDateTime.now())
                .build();

        when(recentlyViewedRepository.findActiveRecentlyViewedByUserId(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(item));
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());
        when(productImageRepository.findCoverImagesByProductIds(List.of(productId)))
                .thenReturn(Collections.emptyList());

        List<RecentlyViewedResponse> result = recentlyViewedService.getRecentlyViewedProducts(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsShopPaused());
    }
}
