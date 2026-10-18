package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.AdminProductDetailResponse;
import com.techhub.model.dto.response.AdminProductSummaryResponse;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.enums.*;
import com.techhub.service.product.AdminProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.techhub.model.dto.request.UpdateProductApprovalRequest;
import com.techhub.model.dto.request.UpdateProductSuspendRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminProductController (GET /api/admin/products, PATCH /api/admin/products/{id}/approval)")
class AdminProductControllerTest {

    @Mock
    private AdminProductService adminProductService;

    @InjectMocks
    private AdminProductController adminProductController;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TC-13: controller_getProducts_Success_Returns200Ok - Controller trả về HTTP 200 và PageResponse")
    void controller_getProducts_Success_Returns200Ok() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        AdminProductSummaryResponse item = new AdminProductSummaryResponse(
                productId,
                shopId,
                "TechZone Store",
                "techzone",
                "Laptop Dell XPS 15",
                "laptop-dell-xps-15-a1b2c3",
                "Dell",
                categoryId,
                "Laptop & Máy tính",
                "https://cdn.techhub.vn/dell-xps.jpg",
                new BigDecimal("35000000"),
                new BigDecimal("45000000"),
                20L,
                2L,
                ProductApprovalStatus.PENDING_APPROVAL,
                ProductVisibilityStatus.UNPUBLISHED,
                false,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        PageResponse<AdminProductSummaryResponse> pageResponse = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true
        );

        when(adminProductService.getProducts(
                any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), anyString(), anyString()
        )).thenReturn(pageResponse);

        ApiResponse<PageResponse<AdminProductSummaryResponse>> response = adminProductController.getProducts(
                shopId, categoryId, "Dell", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED,
                false, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched admin products successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals(productId, response.getData().content().get(0).id());
        assertEquals("TechZone Store", response.getData().content().get(0).shopName());
        assertEquals("techzone", response.getData().content().get(0).sellerName());
        assertEquals("Laptop Dell XPS 15", response.getData().content().get(0).name());
    }

    @Test
    @DisplayName("TC-14: controller_getProducts_HasPreAuthorizeAdmin - Endpoint có annotation bảo vệ Role ADMIN")
    void controller_getProducts_HasPreAuthorizeAdmin() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("getProducts",
                UUID.class, UUID.class, String.class, ProductApprovalStatus.class, ProductVisibilityStatus.class,
                Boolean.class, Boolean.class, int.class, int.class, String.class, String.class);

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-15: controller_getProducts_ForwardsAllParametersToService - Chuyển tiếp đúng các tham số bộ lọc tới Service")
    void controller_getProducts_ForwardsAllParametersToService() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        PageResponse<AdminProductSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0, true);

        when(adminProductService.getProducts(
                eq(shopId), eq(categoryId), eq("MacBook"), eq(ProductApprovalStatus.PENDING_APPROVAL),
                eq(ProductVisibilityStatus.UNPUBLISHED), eq(true), eq(false), eq(1), eq(50), eq("updatedAt"), eq("asc")
        )).thenReturn(emptyPage);

        ApiResponse<PageResponse<AdminProductSummaryResponse>> response = adminProductController.getProducts(
                shopId, categoryId, "MacBook", ProductApprovalStatus.PENDING_APPROVAL,
                ProductVisibilityStatus.UNPUBLISHED, true, false, 1, 50, "updatedAt", "asc"
        );

        assertNotNull(response);
        verify(adminProductService).getProducts(
                eq(shopId), eq(categoryId), eq("MacBook"), eq(ProductApprovalStatus.PENDING_APPROVAL),
                eq(ProductVisibilityStatus.UNPUBLISHED), eq(true), eq(false), eq(1), eq(50), eq("updatedAt"), eq("asc")
        );
    }

    @Test
    @DisplayName("TC-16: controller_getProducts_DefaultParametersForwarded - Giá trị mặc định được chuyển tiếp chính xác khi không truyền")
    void controller_getProducts_DefaultParametersForwarded() {
        PageResponse<AdminProductSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0, true);

        when(adminProductService.getProducts(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(0), eq(20), eq("createdAt"), eq("desc")
        )).thenReturn(emptyPage);

        ApiResponse<PageResponse<AdminProductSummaryResponse>> response = adminProductController.getProducts(
                null, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        verify(adminProductService).getProducts(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(0), eq(20), eq("createdAt"), eq("desc")
        );
    }

    @Test
    @DisplayName("TC-17: controller_getProducts_ClassLevelAnnotations - Kiểm tra class annotations @RestController, @RequestMapping, @Validated")
    void controller_getProducts_ClassLevelAnnotations() {
        Class<AdminProductController> clazz = AdminProductController.class;

        assertTrue(clazz.isAnnotationPresent(RestController.class));
        assertTrue(clazz.isAnnotationPresent(RequestMapping.class));
        assertTrue(clazz.isAnnotationPresent(Validated.class));

        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/admin/products"}, requestMapping.value());
    }

    // ==========================================
    // TESTS FOR getProductDetail (API 11)
    // ==========================================

    @Test
    @DisplayName("TC-10: controller_getProductDetail_Success_Returns200Ok - Admin lấy chi tiết sản phẩm thành công trả về HTTP 200 OK")
    void controller_getProductDetail_Success_Returns200Ok() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        AdminProductDetailResponse mockDetail = new AdminProductDetailResponse(
                productId,
                shopId,
                "Apple Flagship Store",
                "apple-vietnam",
                ShopStatus.ACTIVE,
                categoryId,
                "Điện thoại & Tablet",
                CategoryStatus.ACTIVE,
                "Apple",
                "iPhone 15 Pro Max",
                "iphone-15-pro-max-k8f2a1",
                "Mô tả sản phẩm",
                Map.of("RAM", "8GB"),
                ProductApprovalStatus.PENDING_APPROVAL,
                ProductVisibilityStatus.UNPUBLISHED,
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminProductService.getProductDetail(productId)).thenReturn(mockDetail);

        ApiResponse<AdminProductDetailResponse> response = adminProductController.getProductDetail(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched admin product detail successfully", response.getMessage());
        assertEquals(productId, response.getData().id());
        assertEquals("Apple Flagship Store", response.getData().shopName());
        assertEquals(ShopStatus.ACTIVE, response.getData().shopStatus());
        assertEquals(CategoryStatus.ACTIVE, response.getData().categoryStatus());
        verify(adminProductService, times(1)).getProductDetail(productId);
    }

    @Test
    @DisplayName("TC-11: controller_getProductDetail_HasPreAuthorizeAdmin - Endpoint getProductDetail có annotation bảo vệ Role ADMIN")
    void controller_getProductDetail_HasPreAuthorizeAdmin() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("getProductDetail", UUID.class);

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-12: controller_getProductDetail_HasGetMappingWithId - Endpoint có annotation @GetMapping(\"/{id}\") và @PathVariable")
    void controller_getProductDetail_HasGetMappingWithId() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("getProductDetail", UUID.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/{id}"}, method.getAnnotation(GetMapping.class).value());

        Parameter[] params = method.getParameters();
        assertEquals(1, params.length);
        assertTrue(params[0].isAnnotationPresent(PathVariable.class));
    }

    @Test
    @DisplayName("TC-13: controller_getProductDetail_ForwardsIdToService - Chuyển tiếp đúng productId tới Service")
    void controller_getProductDetail_ForwardsIdToService() {
        UUID productId = UUID.randomUUID();
        when(adminProductService.getProductDetail(productId)).thenReturn(null);

        adminProductController.getProductDetail(productId);

        verify(adminProductService, times(1)).getProductDetail(productId);
    }

    @Test
    @DisplayName("TC-14: controller_getProductDetail_NotFound_PropagatesException - Ngoại lệ PRODUCT_NOT_FOUND từ Service được ném ra ngoài")
    void controller_getProductDetail_NotFound_PropagatesException() {
        UUID productId = UUID.randomUUID();
        when(adminProductService.getProductDetail(productId))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductController.getProductDetail(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(adminProductService, times(1)).getProductDetail(productId);
    }

    // ==========================================
    // TESTS FOR updateProductApproval (API 12)
    // ==========================================

    @Test
    @DisplayName("TC-17: controller_updateProductApproval_Success_Approved - Admin phê duyệt sản phẩm thành công trả về HTTP 200 OK")
    void controller_updateProductApproval_Success_Approved() {
        UUID productId = UUID.randomUUID();
        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        AdminProductDetailResponse mockDetail = new AdminProductDetailResponse(
                productId,
                UUID.randomUUID(),
                "Shop Name",
                "shop-slug",
                ShopStatus.ACTIVE,
                UUID.randomUUID(),
                "Category Name",
                CategoryStatus.ACTIVE,
                "Brand Name",
                "Product Name",
                "product-name-k8f2a1",
                "Description",
                Map.of(),
                ProductApprovalStatus.APPROVED,
                ProductVisibilityStatus.UNPUBLISHED,
                null,
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminProductService.updateProductApproval(productId, request)).thenReturn(mockDetail);

        ApiResponse<AdminProductDetailResponse> response = adminProductController.updateProductApproval(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product approved successfully", response.getMessage());
        assertEquals(ProductApprovalStatus.APPROVED, response.getData().approvalStatus());
        verify(adminProductService, times(1)).updateProductApproval(productId, request);
    }

    @Test
    @DisplayName("TC-18: controller_updateProductApproval_Success_Rejected - Admin từ chối duyệt sản phẩm thành công trả về HTTP 200 OK")
    void controller_updateProductApproval_Success_Rejected() {
        UUID productId = UUID.randomUUID();
        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, "Vi phạm tiêu chuẩn cộng đồng");

        AdminProductDetailResponse mockDetail = new AdminProductDetailResponse(
                productId,
                UUID.randomUUID(),
                "Shop Name",
                "shop-slug",
                ShopStatus.ACTIVE,
                UUID.randomUUID(),
                "Category Name",
                CategoryStatus.ACTIVE,
                "Brand Name",
                "Product Name",
                "product-name-k8f2a1",
                "Description",
                Map.of(),
                ProductApprovalStatus.REJECTED,
                ProductVisibilityStatus.UNPUBLISHED,
                "Vi phạm tiêu chuẩn cộng đồng",
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminProductService.updateProductApproval(productId, request)).thenReturn(mockDetail);

        ApiResponse<AdminProductDetailResponse> response = adminProductController.updateProductApproval(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product rejected successfully", response.getMessage());
        assertEquals(ProductApprovalStatus.REJECTED, response.getData().approvalStatus());
        assertEquals("Vi phạm tiêu chuẩn cộng đồng", response.getData().rejectionReason());
        verify(adminProductService, times(1)).updateProductApproval(productId, request);
    }

    @Test
    @DisplayName("TC-19: controller_updateProductApproval_HasPreAuthorizeAdmin - Endpoint có annotation bảo vệ Role ADMIN")
    void controller_updateProductApproval_HasPreAuthorizeAdmin() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("updateProductApproval", UUID.class, UpdateProductApprovalRequest.class);

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-20: controller_updateProductApproval_HasPatchMapping - Endpoint có mapping @PatchMapping(\"/{id}/approval\") và @PathVariable")
    void controller_updateProductApproval_HasPatchMapping() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("updateProductApproval", UUID.class, UpdateProductApprovalRequest.class);

        assertTrue(method.isAnnotationPresent(PatchMapping.class));
        assertArrayEquals(new String[]{"/{id}/approval"}, method.getAnnotation(PatchMapping.class).value());

        Parameter[] params = method.getParameters();
        assertEquals(2, params.length);
        assertTrue(params[0].isAnnotationPresent(PathVariable.class));
        assertTrue(params[1].isAnnotationPresent(Valid.class));
        assertTrue(params[1].isAnnotationPresent(RequestBody.class));
    }

    @Test
    @DisplayName("TC-21: controller_updateProductApproval_Validation_NullStatus - Xử lý lỗi validation khi status là null trong Request Body")
    void controller_updateProductApproval_Validation_NullStatus() {
        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(null, "Lý do");
        Set<ConstraintViolation<UpdateProductApprovalRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("TC-22: controller_updateProductApproval_Validation_RejectionReasonTooLong - Xử lý lỗi validation khi rejectionReason vượt quá 1000 ký tự")
    void controller_updateProductApproval_Validation_RejectionReasonTooLong() {
        String tooLongReason = "A".repeat(1001);
        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, tooLongReason);
        Set<ConstraintViolation<UpdateProductApprovalRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rejectionReason")));
    }

    // ==========================================
    // TESTS FOR updateProductSuspend (API 13)
    // ==========================================

    @Test
    @DisplayName("TC-11-Suspend: controller_updateProductSuspend_Success_Suspend - Admin đình chỉ sản phẩm thành công trả về HTTP 200 OK")
    void controller_updateProductSuspend_Success_Suspend() {
        UUID productId = UUID.randomUUID();
        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        AdminProductDetailResponse mockDetail = new AdminProductDetailResponse(
                productId,
                UUID.randomUUID(),
                "Shop Name",
                "shop-slug",
                ShopStatus.ACTIVE,
                UUID.randomUUID(),
                "Category Name",
                CategoryStatus.ACTIVE,
                "Brand Name",
                "Product Name",
                "product-name-k8f2a1",
                "Description",
                Map.of(),
                ProductApprovalStatus.APPROVED,
                ProductVisibilityStatus.PUBLISHED,
                null,
                true,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminProductService.updateProductSuspend(productId, request)).thenReturn(mockDetail);

        ApiResponse<AdminProductDetailResponse> response = adminProductController.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product suspended successfully", response.getMessage());
        assertTrue(response.getData().isSuspended());
        verify(adminProductService, times(1)).updateProductSuspend(productId, request);
    }

    @Test
    @DisplayName("TC-12-Suspend: controller_updateProductSuspend_Success_Unsuspend - Admin mở khóa phục hồi sản phẩm thành công trả về HTTP 200 OK")
    void controller_updateProductSuspend_Success_Unsuspend() {
        UUID productId = UUID.randomUUID();
        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(false);

        AdminProductDetailResponse mockDetail = new AdminProductDetailResponse(
                productId,
                UUID.randomUUID(),
                "Shop Name",
                "shop-slug",
                ShopStatus.ACTIVE,
                UUID.randomUUID(),
                "Category Name",
                CategoryStatus.ACTIVE,
                "Brand Name",
                "Product Name",
                "product-name-k8f2a1",
                "Description",
                Map.of(),
                ProductApprovalStatus.APPROVED,
                ProductVisibilityStatus.PUBLISHED,
                null,
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminProductService.updateProductSuspend(productId, request)).thenReturn(mockDetail);

        ApiResponse<AdminProductDetailResponse> response = adminProductController.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product unsuspended successfully", response.getMessage());
        assertFalse(response.getData().isSuspended());
        verify(adminProductService, times(1)).updateProductSuspend(productId, request);
    }

    @Test
    @DisplayName("TC-13-Suspend: controller_updateProductSuspend_HasPreAuthorizeAdmin - Endpoint có annotation bảo vệ Role ADMIN")
    void controller_updateProductSuspend_HasPreAuthorizeAdmin() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("updateProductSuspend", UUID.class, UpdateProductSuspendRequest.class);

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    @DisplayName("TC-14-Suspend: controller_updateProductSuspend_HasPatchMapping - Endpoint có mapping @PatchMapping(\"/{id}/suspend\") và @PathVariable")
    void controller_updateProductSuspend_HasPatchMapping() throws NoSuchMethodException {
        Method method = AdminProductController.class.getMethod("updateProductSuspend", UUID.class, UpdateProductSuspendRequest.class);

        assertTrue(method.isAnnotationPresent(PatchMapping.class));
        assertArrayEquals(new String[]{"/{id}/suspend"}, method.getAnnotation(PatchMapping.class).value());

        Parameter[] params = method.getParameters();
        assertEquals(2, params.length);
        assertTrue(params[0].isAnnotationPresent(PathVariable.class));
        assertTrue(params[1].isAnnotationPresent(Valid.class));
        assertTrue(params[1].isAnnotationPresent(RequestBody.class));
    }

    @Test
    @DisplayName("TC-15-Suspend: controller_updateProductSuspend_Validation_NullIsSuspended - Xử lý lỗi validation khi isSuspended là null")
    void controller_updateProductSuspend_Validation_NullIsSuspended() {
        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(null);
        Set<ConstraintViolation<UpdateProductSuspendRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isSuspended")));
    }
}
