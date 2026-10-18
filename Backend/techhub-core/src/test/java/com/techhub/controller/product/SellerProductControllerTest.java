package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateProductImageRequest;
import com.techhub.model.dto.request.CreateProductRequest;
import com.techhub.model.dto.request.CreateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductRequest;
import com.techhub.model.dto.request.UpdateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductVisibilityRequest;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.dto.response.SellerProductSummaryResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVariantStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.product.SellerProductService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductController (POST /api/seller/products)")
class SellerProductControllerTest {

    @Mock
    private SellerProductService sellerProductService;

    @InjectMocks
    private SellerProductController sellerProductController;

    private Validator validator;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("seller@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.SELLER));
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-18: controller_createProduct_Success - Gọi service và trả về HTTP 201 Created")
    void controller_createProduct_Success() throws NoSuchMethodException {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("2500000.00"), 100, null, null
        );
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Bàn phím cơ Apple", "Apple", "Mô tả",
                Map.of("Kết nối", "Bluetooth"), List.of(img), List.of(variant)
        );

        ProductDetailResponse mockResponse = new ProductDetailResponse(
                productId, shopId, categoryId, "Bàn phím", "Apple",
                "Bàn phím cơ Apple", "ban-phim-co-apple-abc123", "Mô tả",
                Map.of("Kết nối", "Bluetooth"),
                ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false,
                List.of(new ProductImageResponse(UUID.randomUUID(), "https://cdn.techhub.vn/img1.jpg", 0)),
                List.of(new ProductVariantResponse(UUID.randomUUID(), "APL-KB-01", "Tiêu chuẩn",
                        new BigDecimal("2500000.00"), 100, null, null, ProductVariantStatus.ACTIVE)),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(sellerProductService.createProduct(userId, request)).thenReturn(mockResponse);

        // Act
        ApiResponse<ProductDetailResponse> response = sellerProductController.createProduct(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product draft created successfully", response.getMessage());
        assertEquals(productId, response.getData().id());
        assertEquals(ProductApprovalStatus.DRAFT, response.getData().approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.getData().visibilityStatus());
        verify(sellerProductService).createProduct(userId, request);

        // Verify @ResponseStatus(HttpStatus.CREATED)
        ResponseStatus responseStatus = SellerProductController.class.getMethod("createProduct", CreateProductRequest.class)
                .getAnnotation(ResponseStatus.class);
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.CREATED, responseStatus.value());
    }

    @Test
    @DisplayName("TC-19: controller_createProduct_HasPreAuthorizeSeller - Kiểm tra bảo mật @PreAuthorize hasRole SELLER")
    void controller_createProduct_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("createProduct", CreateProductRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method createProduct phải được đánh dấu @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-20: validation_createProduct_Fail_BlankName - Bean Validation phát hiện tên rỗng")
    void validation_createProduct_Fail_BlankName() {
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("2500000.00"), 100, null, null
        );
        CreateProductRequest request = new CreateProductRequest(
                UUID.randomUUID(), "", "Apple", "Mô tả",
                Map.of("Kết nối", "Bluetooth"), List.of(img), List.of(variant)
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TC-21: validation_createProduct_Fail_NullCategoryId - Bean Validation phát hiện categoryId null")
    void validation_createProduct_Fail_NullCategoryId() {
        CreateProductImageRequest img = new CreateProductImageRequest("https://cdn.techhub.vn/img1.jpg", 0);
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("2500000.00"), 100, null, null
        );
        CreateProductRequest request = new CreateProductRequest(
                null, "Bàn phím cơ Apple", "Apple", "Mô tả",
                Map.of("Kết nối", "Bluetooth"), List.of(img), List.of(variant)
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("categoryId")));
    }

    @Test
    @DisplayName("TC-22: validation_createProduct_Fail_EmptyImages - Bean Validation phát hiện danh sách ảnh rỗng")
    void validation_createProduct_Fail_EmptyImages() {
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("2500000.00"), 100, null, null
        );
        CreateProductRequest request = new CreateProductRequest(
                UUID.randomUUID(), "Bàn phím cơ Apple", "Apple", "Mô tả",
                Map.of("Kết nối", "Bluetooth"), Collections.emptyList(), List.of(variant)
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("images")));
    }

    @Test
    @DisplayName("TC-23: validation_createProduct_Fail_VariantInvalidPrice - Bean Validation phát hiện giá variant <= 0")
    void validation_createProduct_Fail_VariantInvalidPrice() {
        CreateProductVariantRequest variant = new CreateProductVariantRequest(
                "APL-KB-01", "Tiêu chuẩn", "Mô tả",
                new BigDecimal("0.00"), 100, null, null
        );

        Set<ConstraintViolation<CreateProductVariantRequest>> violations = validator.validate(variant);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    @DisplayName("TC-24: getProducts_Success_Returns200Ok - Controller trả về HTTP 200 và PageResponse")
    void getProducts_Success_Returns200Ok() {
        SellerProductSummaryResponse summary = new SellerProductSummaryResponse(
                UUID.randomUUID(), "Bàn phím cơ", "ban-phim-co-1234", "Akko",
                UUID.randomUUID(), "Bàn phím", "https://cdn.techhub.com/cover.jpg",
                new BigDecimal("100000"), new BigDecimal("200000"), 50L, 2L,
                ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED,
                false, false, LocalDateTime.now(), LocalDateTime.now()
        );
        PageResponse<SellerProductSummaryResponse> pageResponse = new PageResponse<>(
                List.of(summary), 0, 20, 1, 1, true
        );

        when(sellerProductService.getProducts(
                eq(userId), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(0), eq(20), eq("createdAt"), eq("desc")
        )).thenReturn(pageResponse);

        ApiResponse<PageResponse<SellerProductSummaryResponse>> response = sellerProductController.getProducts(
                null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product list retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals("Bàn phím cơ", response.getData().content().get(0).name());
    }

    @Test
    @DisplayName("TC-25: getProducts_HasPreAuthorizeSeller - Endpoint có annotation bảo vệ Role SELLER")
    void getProducts_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("getProducts",
                String.class, UUID.class, ProductApprovalStatus.class, ProductVisibilityStatus.class,
                Boolean.class, Boolean.class, int.class, int.class, String.class, String.class);

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('SELLER')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-26: controller_getProductDetail_Success - Gọi service và trả về HTTP 200 OK")
    void controller_getProductDetail_Success() throws NoSuchMethodException {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ProductDetailResponse mockResponse = new ProductDetailResponse(
                productId, shopId, categoryId, "Bàn phím", "Apple",
                "Bàn phím cơ Apple", "ban-phim-co-apple-abc123", "Mô tả",
                Map.of("Kết nối", "Bluetooth"),
                ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false,
                List.of(new ProductImageResponse(UUID.randomUUID(), "https://cdn.techhub.vn/img1.jpg", 0)),
                List.of(new ProductVariantResponse(UUID.randomUUID(), "APL-KB-01", "Tiêu chuẩn",
                        new BigDecimal("2500000.00"), 100, null, null, ProductVariantStatus.ACTIVE)),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(sellerProductService.getProductDetail(userId, productId)).thenReturn(mockResponse);

        ApiResponse<ProductDetailResponse> response = sellerProductController.getProductDetail(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product detail retrieved successfully", response.getMessage());
        assertEquals(productId, response.getData().id());
        assertEquals("Bàn phím cơ Apple", response.getData().name());

        Method method = SellerProductController.class.getMethod("getProductDetail", UUID.class);
        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('SELLER')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-27: controller_getProductDetail_ProductNotFound - Service ném PRODUCT_NOT_FOUND")
    void controller_getProductDetail_ProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(sellerProductService.getProductDetail(userId, productId))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.getProductDetail(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-28: controller_updateProduct_Success - Gọi service và trả về HTTP 200 OK")
    void controller_updateProduct_Success() throws NoSuchMethodException {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        UpdateProductRequest request = new UpdateProductRequest(
                categoryId,
                "iPhone 15 Pro Max Updated",
                "Apple",
                "Mô tả mới",
                Map.of("RAM", "8GB"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.vn/cover.jpg", 0))
        );

        ProductDetailResponse mockResponse = new ProductDetailResponse(
                productId, shopId, categoryId, "Điện thoại", "Apple",
                "iPhone 15 Pro Max Updated", "iphone-15-pro-max-updated-abc123", "Mô tả mới",
                Map.of("RAM", "8GB"),
                ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false,
                List.of(new ProductImageResponse(UUID.randomUUID(), "https://cdn.techhub.vn/cover.jpg", 0)),
                List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(sellerProductService.updateProduct(userId, productId, request)).thenReturn(mockResponse);

        ApiResponse<ProductDetailResponse> response = sellerProductController.updateProduct(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product updated successfully", response.getMessage());
        assertEquals(productId, response.getData().id());
        assertEquals("iPhone 15 Pro Max Updated", response.getData().name());

        Method method = SellerProductController.class.getMethod("updateProduct", UUID.class, UpdateProductRequest.class);
        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('SELLER')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-29: controller_updateProduct_ValidationFail_MissingName - Validation bắt lỗi name rỗng")
    void controller_updateProduct_ValidationFail_MissingName() {
        UpdateProductRequest invalidRequest = new UpdateProductRequest(
                UUID.randomUUID(),
                "", // Blank name
                "Apple",
                "Mô tả",
                Map.of("RAM", "8GB"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.vn/cover.jpg", 0))
        );

        Set<ConstraintViolation<UpdateProductRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TC-30: controller_updateProduct_ProductNotFound - Service ném PRODUCT_NOT_FOUND")
    void controller_updateProduct_ProductNotFound() {
        UUID productId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                UUID.randomUUID(),
                "iPhone 15 Pro Max Updated",
                "Apple",
                "Mô tả",
                Map.of("RAM", "8GB"),
                List.of(new CreateProductImageRequest("https://cdn.techhub.vn/cover.jpg", 0))
        );

        when(sellerProductService.updateProduct(userId, productId, request))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.updateProduct(productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-31: controller_deleteProduct_Success - Xóa mềm sản phẩm thành công trả về 200 OK")
    void controller_deleteProduct_Success() {
        UUID productId = UUID.randomUUID();
        doNothing().when(sellerProductService).deleteProduct(userId, productId);

        ApiResponse<Void> response = sellerProductController.deleteProduct(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product deleted successfully", response.getMessage());
        assertNull(response.getData());
        verify(sellerProductService, times(1)).deleteProduct(userId, productId);
    }

    @Test
    @DisplayName("TC-32: controller_deleteProduct_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_deleteProduct_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("deleteProduct", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-33: controller_deleteProduct_ProductNotFound - Service ném PRODUCT_NOT_FOUND khi xóa")
    void controller_deleteProduct_ProductNotFound() {
        UUID productId = UUID.randomUUID();
        doThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .when(sellerProductService).deleteProduct(userId, productId);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.deleteProduct(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(sellerProductService, times(1)).deleteProduct(userId, productId);
    }

    @Test
    @DisplayName("TC-34: controller_addProductVariant_Success - Gọi service và trả về HTTP 201 Created")
    void controller_addProductVariant_Success() throws NoSuchMethodException {
        UUID productId = UUID.randomUUID();
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-512-NAT",
                "Titan Tự Nhiên 512GB",
                "Mô tả",
                new BigDecimal("34990000.00"),
                30,
                "https://cdn.techhub.vn/variant.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên")
        );

        UUID variantId = UUID.randomUUID();
        ProductVariantResponse mockResponse = new ProductVariantResponse(
                variantId,
                "IP15PM-512-NAT",
                "Titan Tự Nhiên 512GB",
                new BigDecimal("34990000.00"),
                30,
                "https://cdn.techhub.vn/variant.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerProductService.addProductVariant(userId, productId, request)).thenReturn(mockResponse);

        ApiResponse<ProductVariantResponse> response = sellerProductController.addProductVariant(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product variant created successfully", response.getMessage());
        assertEquals(variantId, response.getData().id());
        assertEquals("IP15PM-512-NAT", response.getData().sku());

        Method method = SellerProductController.class.getMethod("addProductVariant", UUID.class, CreateProductVariantRequest.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.CREATED, responseStatus.value());

        verify(sellerProductService, times(1)).addProductVariant(userId, productId, request);
    }

    @Test
    @DisplayName("TC-35: controller_addProductVariant_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_addProductVariant_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("addProductVariant", UUID.class, CreateProductVariantRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-36: controller_addProductVariant_ValidationFail_MissingSku - Validation lỗi khi thiếu SKU")
    void controller_addProductVariant_ValidationFail_MissingSku() {
        CreateProductVariantRequest invalidRequest = new CreateProductVariantRequest(
                "", // Blank SKU
                "Titan Tự Nhiên 512GB",
                "Mô tả",
                new BigDecimal("34990000.00"),
                30,
                null,
                null
        );

        Set<ConstraintViolation<CreateProductVariantRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sku")));
    }

    @Test
    @DisplayName("TC-37: controller_addProductVariant_ProductNotFound - Service ném PRODUCT_NOT_FOUND khi thêm biến thể")
    void controller_addProductVariant_ProductNotFound() {
        UUID productId = UUID.randomUUID();
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-512-NAT",
                "Titan Tự Nhiên 512GB",
                "Mô tả",
                new BigDecimal("34990000.00"),
                30,
                null,
                null
        );

        when(sellerProductService.addProductVariant(userId, productId, request))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.addProductVariant(productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(sellerProductService, times(1)).addProductVariant(userId, productId, request);
    }

    @Test
    @DisplayName("TC-38: controller_updateProductVariant_Success - Cập nhật biến thể thành công trả về 200 OK")
    void controller_updateProductVariant_Success() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Mô tả cập nhật",
                new BigDecimal("29990000.00"),
                50,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên"),
                ProductVariantStatus.ACTIVE
        );

        ProductVariantResponse mockResponse = new ProductVariantResponse(
                variantId,
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                new BigDecimal("29990000.00"),
                50,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerProductService.updateProductVariant(userId, productId, variantId, request))
                .thenReturn(mockResponse);

        ApiResponse<ProductVariantResponse> response = sellerProductController.updateProductVariant(
                productId, variantId, request
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product variant updated successfully", response.getMessage());
        assertEquals(variantId, response.getData().id());
        assertEquals("IP15PM-256-NAT", response.getData().sku());
        assertEquals(50, response.getData().stockQuantity());

        verify(sellerProductService, times(1)).updateProductVariant(userId, productId, variantId, request);
    }

    @Test
    @DisplayName("TC-39: controller_updateProductVariant_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_updateProductVariant_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("updateProductVariant", UUID.class, UUID.class, UpdateProductVariantRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-40: controller_updateProductVariant_ValidationFail_MissingPrice - Bean Validation phát hiện thiếu price")
    void controller_updateProductVariant_ValidationFail_MissingPrice() {
        UpdateProductVariantRequest invalidRequest = new UpdateProductVariantRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Mô tả",
                null, // Missing price
                30,
                null,
                null,
                ProductVariantStatus.ACTIVE
        );

        Set<ConstraintViolation<UpdateProductVariantRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    @DisplayName("TC-41: controller_updateProductVariant_ProductNotFound - Service ném PRODUCT_NOT_FOUND khi cập nhật")
    void controller_updateProductVariant_ProductNotFound() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Mô tả",
                new BigDecimal("29990000.00"),
                30,
                null,
                null,
                ProductVariantStatus.ACTIVE
        );

        when(sellerProductService.updateProductVariant(userId, productId, variantId, request))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.updateProductVariant(productId, variantId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(sellerProductService, times(1)).updateProductVariant(userId, productId, variantId, request);
    }

    @Test
    @DisplayName("TC-42: controller_deleteProductVariant_Success - Gọi thành công xóa biến thể trả về HTTP 200 OK")
    void controller_deleteProductVariant_Success() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        doNothing().when(sellerProductService).deleteProductVariant(userId, productId, variantId);

        ApiResponse<Void> response = sellerProductController.deleteProductVariant(productId, variantId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product variant deleted successfully", response.getMessage());
        assertNull(response.getData());

        verify(sellerProductService, times(1)).deleteProductVariant(userId, productId, variantId);
    }

    @Test
    @DisplayName("TC-43: controller_deleteProductVariant_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_deleteProductVariant_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("deleteProductVariant", UUID.class, UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-44: controller_deleteProductVariant_ProductNotFound - Service ném PRODUCT_NOT_FOUND khi xóa")
    void controller_deleteProductVariant_ProductNotFound() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        doThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .when(sellerProductService).deleteProductVariant(userId, productId, variantId);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.deleteProductVariant(productId, variantId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(sellerProductService, times(1)).deleteProductVariant(userId, productId, variantId);
    }

    @Test
    @DisplayName("TC-45: controller_deleteProductVariant_CannotDeleteLastVariant - Service ném CANNOT_DELETE_LAST_VARIANT")
    void controller_deleteProductVariant_CannotDeleteLastVariant() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        doThrow(new BusinessException(ErrorCode.CANNOT_DELETE_LAST_VARIANT))
                .when(sellerProductService).deleteProductVariant(userId, productId, variantId);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.deleteProductVariant(productId, variantId)
        );

        assertEquals(ErrorCode.CANNOT_DELETE_LAST_VARIANT, ex.getErrorCode());
        verify(sellerProductService, times(1)).deleteProductVariant(userId, productId, variantId);
    }

    @Test
    @DisplayName("TC-46: controller_submitProductForApproval_Success - Gọi thành công submit approval trả về HTTP 200 OK")
    void controller_submitProductForApproval_Success() {
        UUID productId = UUID.randomUUID();
        ProductDetailResponse mockResponse = new ProductDetailResponse(
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Điện thoại thông minh",
                "Apple",
                "iPhone 15 Pro Max",
                "iphone-15-pro-max-k8f2a1",
                "Mô tả",
                Map.of("RAM", "8GB"),
                ProductApprovalStatus.PENDING_APPROVAL,
                ProductVisibilityStatus.UNPUBLISHED,
                false,
                false,
                List.of(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(sellerProductService.submitProductForApproval(userId, productId)).thenReturn(mockResponse);

        ApiResponse<ProductDetailResponse> response = sellerProductController.submitProductForApproval(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product submitted for approval successfully", response.getMessage());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.getData().approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.getData().visibilityStatus());

        verify(sellerProductService, times(1)).submitProductForApproval(userId, productId);
    }

    @Test
    @DisplayName("TC-47: controller_submitProductForApproval_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_submitProductForApproval_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("submitProductForApproval", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-48: controller_submitProductForApproval_ProductNotFound - Service ném PRODUCT_NOT_FOUND")
    void controller_submitProductForApproval_ProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(sellerProductService.submitProductForApproval(userId, productId))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.submitProductForApproval(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(sellerProductService, times(1)).submitProductForApproval(userId, productId);
    }

    @Test
    @DisplayName("TC-49: controller_submitProductForApproval_InvalidStatusTransition - Service ném INVALID_STATUS_TRANSITION")
    void controller_submitProductForApproval_InvalidStatusTransition() {
        UUID productId = UUID.randomUUID();

        when(sellerProductService.submitProductForApproval(userId, productId))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.submitProductForApproval(productId)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(sellerProductService, times(1)).submitProductForApproval(userId, productId);
    }

    @Test
    @DisplayName("TC-50: controller_updateProductVisibility_Success - Mở bán thành công trả về 200 OK")
    void controller_updateProductVisibility_Success() {
        UUID productId = UUID.randomUUID();
        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);

        ProductDetailResponse mockResponse = new ProductDetailResponse(
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Điện thoại thông minh",
                "Apple",
                "iPhone 15 Pro Max",
                "iphone-15-pro-max-k8f2a1",
                "Mô tả",
                Map.of("RAM", "8GB"),
                ProductApprovalStatus.APPROVED,
                ProductVisibilityStatus.PUBLISHED,
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(sellerProductService.updateProductVisibility(userId, productId, request)).thenReturn(mockResponse);

        ApiResponse<ProductDetailResponse> response = sellerProductController.updateProductVisibility(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product visibility updated successfully", response.getMessage());
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.getData().visibilityStatus());
        verify(sellerProductService, times(1)).updateProductVisibility(userId, productId, request);
    }

    @Test
    @DisplayName("TC-51: controller_updateProductVisibility_HasPreAuthorizeSeller - Kiểm tra @PreAuthorize hasRole SELLER")
    void controller_updateProductVisibility_HasPreAuthorizeSeller() throws NoSuchMethodException {
        Method method = SellerProductController.class.getMethod("updateProductVisibility", UUID.class, UpdateProductVisibilityRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-52: controller_updateProductVisibility_InvalidBody_ThrowsBadRequest - Request body null visibilityStatus")
    void controller_updateProductVisibility_InvalidBody_ThrowsBadRequest() {
        UpdateProductVisibilityRequest invalidRequest = new UpdateProductVisibilityRequest(null);
        Set<ConstraintViolation<UpdateProductVisibilityRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Visibility status is required")));
    }

    @Test
    @DisplayName("TC-53: controller_updateProductVisibility_ProductNotApproved_ThrowsBadRequest - Service ném PRODUCT_NOT_APPROVED")
    void controller_updateProductVisibility_ProductNotApproved_ThrowsBadRequest() {
        UUID productId = UUID.randomUUID();
        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);

        when(sellerProductService.updateProductVisibility(userId, productId, request))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductController.updateProductVisibility(productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
        verify(sellerProductService, times(1)).updateProductVisibility(userId, productId, request);
    }
}
