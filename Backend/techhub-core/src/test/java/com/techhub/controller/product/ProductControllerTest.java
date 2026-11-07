package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.ProductSummaryResponse;
import com.techhub.service.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicProductDetailResponse;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.RecentlyViewedResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ProductVariantStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.recentlyviewed.RecentlyViewedService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ProductController (GET /api/products, GET /api/products/{slug}, POST /api/products/{id}/view)")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private RecentlyViewedService recentlyViewedService;

    @InjectMocks
    private ProductController productController;

    @Test
    @DisplayName("TC-22: getPublicProducts_PublicAccess_NoAuth_Success")
    void getPublicProducts_PublicAccess_NoAuth_Success() {
        UUID prodId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        ProductSummaryResponse item = new ProductSummaryResponse(
                prodId,
                "MacBook Air M3",
                "macbook-air-m3",
                "Apple",
                catId,
                "Laptop",
                shopId,
                "Apple Store",
                false,
                "https://cdn.techhub.com/cover.jpg",
                new BigDecimal("27490000"),
                true,
                LocalDateTime.now()
        );

        PageResponse<ProductSummaryResponse> pageResponse = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true
        );

        when(productService.getPublicProducts(null, null, null, null, null, false, "newest", 0, 20))
                .thenReturn(pageResponse);

        ApiResponse<PageResponse<ProductSummaryResponse>> response = productController.getProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched marketplace products successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals("MacBook Air M3", response.getData().content().get(0).name());
    }

    @Test
    @DisplayName("TC-23: getPublicProducts_WithAuthToken_Success")
    void getPublicProducts_WithAuthToken_Success() {
        PageResponse<ProductSummaryResponse> emptyPage = new PageResponse<>(
                List.of(), 0, 20, 0, 0, true
        );
        when(productService.getPublicProducts(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        ApiResponse<PageResponse<ProductSummaryResponse>> response = productController.getProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().content().isEmpty());
    }

    @Test
    @DisplayName("TC-24: getPublicProducts_WithFullQueryParams_BindsCorrectly")
    void getPublicProducts_WithFullQueryParams_BindsCorrectly() {
        UUID catId = UUID.randomUUID();
        BigDecimal minPrice = new BigDecimal("10000000");
        BigDecimal maxPrice = new BigDecimal("30000000");

        PageResponse<ProductSummaryResponse> pageResponse = new PageResponse<>(
                List.of(), 1, 10, 0, 0, true
        );

        when(productService.getPublicProducts(
                "laptop", catId, "Dell", minPrice, maxPrice, true, "price_asc", 1, 10
        )).thenReturn(pageResponse);

        ApiResponse<PageResponse<ProductSummaryResponse>> response = productController.getProducts(
                "laptop", catId, "Dell", minPrice, maxPrice, true, "price_asc", 1, 10
        );

        assertNotNull(response);
        verify(productService).getPublicProducts(
                "laptop", catId, "Dell", minPrice, maxPrice, true, "price_asc", 1, 10
        );
    }

    @Test
    @DisplayName("TC-25: getPublicProducts_PageClamping_SizeAboveMax_Sanitized")
    void getPublicProducts_PageClamping_SizeAboveMax_Sanitized() {
        PageResponse<ProductSummaryResponse> pageResponse = new PageResponse<>(
                List.of(), 0, 100, 0, 0, true
        );

        when(productService.getPublicProducts(
                null, null, null, null, null, false, "newest", -1, 500
        )).thenReturn(pageResponse);

        ApiResponse<PageResponse<ProductSummaryResponse>> response = productController.getProducts(
                null, null, null, null, null, false, "newest", -1, 500
        );

        assertNotNull(response);
        verify(productService).getPublicProducts(
                null, null, null, null, null, false, "newest", -1, 500
        );
    }

    @Test
    @DisplayName("TC-26: getPublicProducts_PublicAccess_NoPreAuthorizeAnnotation")
    void getPublicProducts_PublicAccess_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        Class<ProductController> clazz = ProductController.class;

        // Class-level annotations
        assertTrue(clazz.isAnnotationPresent(RestController.class));
        assertTrue(clazz.isAnnotationPresent(RequestMapping.class));
        assertArrayEquals(new String[]{"/api/products"}, clazz.getAnnotation(RequestMapping.class).value());
        assertFalse(clazz.isAnnotationPresent(PreAuthorize.class), "Controller must NOT have @PreAuthorize");

        // Method-level annotations
        Method method = clazz.getMethod("getProducts", String.class, UUID.class, String.class, BigDecimal.class, BigDecimal.class, Boolean.class, String.class, int.class, int.class);
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        assertFalse(method.isAnnotationPresent(PreAuthorize.class), "getProducts method must NOT have @PreAuthorize for public access");
    }

    @Test
    @DisplayName("TC-27: getPublicProducts_ResponseStructureMatchesContract")
    void getPublicProducts_ResponseStructureMatchesContract() {
        UUID prodId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ProductSummaryResponse item = new ProductSummaryResponse(
                prodId,
                "Keyboard Q1 Pro",
                "keyboard-q1-pro",
                "Keychron",
                catId,
                "Bàn phím",
                shopId,
                "Gear Store",
                true,
                "https://cdn.techhub.com/q1.jpg",
                new BigDecimal("4290000"),
                false,
                now
        );

        assertEquals(prodId, item.id());
        assertEquals("Keyboard Q1 Pro", item.name());
        assertEquals("keyboard-q1-pro", item.slug());
        assertEquals("Keychron", item.brandName());
        assertEquals(catId, item.categoryId());
        assertEquals("Bàn phím", item.categoryName());
        assertEquals(shopId, item.shopId());
        assertEquals("Gear Store", item.shopName());
        assertTrue(item.isShopPaused());
        assertEquals("https://cdn.techhub.com/q1.jpg", item.coverImageUrl());
        assertEquals(new BigDecimal("4290000"), item.price());
        assertFalse(item.inStock());
        assertEquals(now, item.createdAt());
    }

    @Test
    @DisplayName("TC-23: controller_getProductBySlug_Success_Returns200")
    void controller_getProductBySlug_Success_Returns200() {
        UUID prodId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID varId = UUID.randomUUID();

        PublicProductDetailResponse mockResponse = new PublicProductDetailResponse(
                prodId,
                "MacBook Air M3",
                "macbook-air-m3",
                "Apple",
                "Mô tả chi tiết",
                catId,
                "Laptop",
                shopId,
                "Apple Store",
                "https://cdn.techhub.com/avatar.png",
                false,
                Map.of("cpu", "M3"),
                List.of(new ProductImageResponse(UUID.randomUUID(), "https://cdn.techhub.com/cover.jpg", 0)),
                List.of(new ProductVariantResponse(varId, "SKU-1", "Space Gray", new BigDecimal("27490000"), 10, null, null, ProductVariantStatus.ACTIVE)),
                varId,
                new BigDecimal("27490000"),
                new BigDecimal("27490000"),
                10,
                true,
                LocalDateTime.now()
        );

        when(productService.getPublicProductBySlug("macbook-air-m3")).thenReturn(mockResponse);

        ApiResponse<PublicProductDetailResponse> res = productController.getProductBySlug("macbook-air-m3");

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals("Fetched product details successfully", res.getMessage());
        assertNotNull(res.getData());
        assertEquals("macbook-air-m3", res.getData().slug());
        assertEquals(varId, res.getData().defaultVariantId());
    }

    @Test
    @DisplayName("TC-24: controller_getProductBySlug_NotFound_Returns404")
    void controller_getProductBySlug_NotFound_Returns404() {
        when(productService.getPublicProductBySlug("unknown-product"))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productController.getProductBySlug("unknown-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-25: controller_getProductBySlug_PublicAccess_NoPreAuthorize")
    void controller_getProductBySlug_PublicAccess_NoPreAuthorize() throws NoSuchMethodException {
        Method method = ProductController.class.getMethod("getProductBySlug", String.class);
        assertFalse(method.isAnnotationPresent(PreAuthorize.class), "Endpoint must not have @PreAuthorize");
        assertTrue(method.isAnnotationPresent(GetMapping.class), "Endpoint must have @GetMapping");
    }

    @Test
    @DisplayName("TC-26: controller_getProductBySlug_UrlEncodedSlugHandled")
    void controller_getProductBySlug_UrlEncodedSlugHandled() {
        PublicProductDetailResponse mockResponse = mock(PublicProductDetailResponse.class);
        when(productService.getPublicProductBySlug("dell-xps-13-9340")).thenReturn(mockResponse);

        ApiResponse<PublicProductDetailResponse> res = productController.getProductBySlug("dell-xps-13-9340");

        assertNotNull(res);
        assertTrue(res.isSuccess());
        verify(productService).getPublicProductBySlug("dell-xps-13-9340");
    }

    // =========================================================================
    // TESTS FOR POST /api/products/{id}/view (ProductController)
    // =========================================================================

    @Test
    @DisplayName("TC-27: testController_RecordProductView_SecurityAndMethodAnnotations - Kiểm tra @PostMapping và @PreAuthorize")
    void testController_RecordProductView_SecurityAndMethodAnnotations() throws NoSuchMethodException {
        Method method = ProductController.class.getMethod("recordProductView", UUID.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "Method recordProductView phải có annotation @PostMapping");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/{id}/view"}, postMapping.value(),
                "PostMapping path phải là /{id}/view");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method recordProductView phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter[] parameters = method.getParameters();
        assertEquals(1, parameters.length);
        assertTrue(parameters[0].isAnnotationPresent(PathVariable.class),
                "Tham số id phải có annotation @PathVariable");
    }

    @Test
    @DisplayName("TC-28: testController_RecordProductView_Success_Returns200 - Ghi nhận lượt xem thành công trả về 200 OK")
    void testController_RecordProductView_Success_Returns200() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("buyer@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            doNothing().when(recentlyViewedService).recordProductView(userId, productId);

            ApiResponse<Void> response = productController.recordProductView(productId);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Ghi nhận lượt xem sản phẩm thành công", response.getMessage());
            assertNull(response.getData());

            verify(recentlyViewedService, times(1)).recordProductView(userId, productId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("TC-29: testController_RecordProductView_ServiceException_Propagates - Ngoại lệ từ service được truyền đạt chính xác")
    void testController_RecordProductView_ServiceException_Propagates() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("buyer@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            doThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                    .when(recentlyViewedService).recordProductView(userId, productId);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> productController.recordProductView(productId));

            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
            verify(recentlyViewedService, times(1)).recordProductView(userId, productId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // TESTS FOR GET /api/products/recently-viewed (ProductController)
    // =========================================================================

    @Test
    @DisplayName("TC-30: controller_getRecentlyViewed_SecurityAndAnnotations - Kiểm tra @GetMapping và @PreAuthorize")
    void controller_getRecentlyViewed_SecurityAndAnnotations() throws NoSuchMethodException {
        Method method = ProductController.class.getMethod("getRecentlyViewedProducts");

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getRecentlyViewedProducts phải có annotation @GetMapping");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/recently-viewed"}, getMapping.value(),
                "GetMapping path phải là /recently-viewed");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method getRecentlyViewedProducts phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-31: controller_getRecentlyViewed_Success_Returns200 - Trả về 200 OK kèm danh sách gần đây")
    void controller_getRecentlyViewed_Success_Returns200() {
        UUID userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("buyer@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            RecentlyViewedResponse item = RecentlyViewedResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(UUID.randomUUID())
                    .productName("MacBook Air M3")
                    .slug("macbook-air-m3")
                    .coverImageUrl("https://cdn.techhub.com/cover.jpg")
                    .minPrice(new BigDecimal("32990000.00"))
                    .inStock(true)
                    .isShopPaused(false)
                    .viewedAt(LocalDateTime.now())
                    .build();

            when(recentlyViewedService.getRecentlyViewedProducts(userId))
                    .thenReturn(List.of(item));

            ApiResponse<List<RecentlyViewedResponse>> response = productController.getRecentlyViewedProducts();

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Lấy danh sách sản phẩm đã xem gần đây thành công", response.getMessage());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("MacBook Air M3", response.getData().get(0).getProductName());

            verify(recentlyViewedService, times(1)).getRecentlyViewedProducts(userId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("TC-32: controller_getRecentlyViewed_LiteralRoute_DoesNotConflictWithSlug - Literal match ưu tiên phân biệt")
    void controller_getRecentlyViewed_LiteralRoute_DoesNotConflictWithSlug() throws NoSuchMethodException {
        Method recentlyViewedMethod = ProductController.class.getMethod("getRecentlyViewedProducts");
        Method slugMethod = ProductController.class.getMethod("getProductBySlug", String.class);

        assertNotNull(recentlyViewedMethod);
        assertNotNull(slugMethod);

        GetMapping rvMapping = recentlyViewedMethod.getAnnotation(GetMapping.class);
        GetMapping slugMapping = slugMethod.getAnnotation(GetMapping.class);

        assertArrayEquals(new String[]{"/recently-viewed"}, rvMapping.value());
        assertArrayEquals(new String[]{"/{slug}"}, slugMapping.value());
    }

    // =========================================================================
    // TESTS FOR GET /api/products/{id}/similar (ProductController)
    // =========================================================================

    @Test
    @DisplayName("TC-33: controller_getSimilarProducts_PublicAccess_NoPreAuthorize - Endpoint công khai không có @PreAuthorize")
    void controller_getSimilarProducts_PublicAccess_NoPreAuthorize() throws NoSuchMethodException {
        Method method = ProductController.class.getMethod("getSimilarProducts", UUID.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getSimilarProducts phải có annotation @GetMapping");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}/similar"}, getMapping.value(),
                "GetMapping path phải là /{id}/similar");

        assertFalse(method.isAnnotationPresent(PreAuthorize.class),
                "Method getSimilarProducts là public access, không được có @PreAuthorize");
    }

    @Test
    @DisplayName("TC-34: controller_getSimilarProducts_Success_Returns200 - Trả về 200 OK kèm danh sách 6 sản phẩm tương tự")
    void controller_getSimilarProducts_Success_Returns200() {
        UUID productId = UUID.randomUUID();
        ProductSummaryResponse sim = new ProductSummaryResponse(
                UUID.randomUUID(),
                "MacBook Pro M3",
                "macbook-pro-m3",
                "Apple",
                UUID.randomUUID(),
                "Laptop",
                UUID.randomUUID(),
                "Apple Store",
                false,
                "https://cdn.techhub.com/mbp.jpg",
                new BigDecimal("49990000.00"),
                true,
                LocalDateTime.now()
        );

        when(productService.getSimilarProducts(productId)).thenReturn(List.of(sim));

        ApiResponse<List<ProductSummaryResponse>> response = productController.getSimilarProducts(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched similar products successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("MacBook Pro M3", response.getData().get(0).name());

        verify(productService, times(1)).getSimilarProducts(productId);
    }
}
