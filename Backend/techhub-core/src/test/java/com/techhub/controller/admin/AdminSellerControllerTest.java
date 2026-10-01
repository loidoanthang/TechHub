package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.SellerApprovalRequest;
import com.techhub.model.dto.response.AdminSellerApprovalResponse;
import com.techhub.model.dto.response.AdminSellerDetailResponse;
import com.techhub.model.dto.response.AdminSellerResponse;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.service.admin.AdminSellerService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminSellerController (GET /api/admin/sellers)")
class AdminSellerControllerTest {

    @Mock
    private AdminSellerService adminSellerService;

    @InjectMocks
    private AdminSellerController adminSellerController;

    @Test
    @DisplayName("TC-17: controller_getSellers_Success - Controller gọi service đúng tham số và trả về ApiResponse")
    void controller_getSellers_Success() {
        AdminSellerResponse sellerResponse = new AdminSellerResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nguyen Van A",
                "seller@techhub.vn",
                "0987654321",
                UserStatus.ACTIVE,
                UUID.randomUUID(),
                "Apple Official Store",
                ShopStatus.SUSPENDED,
                "apple-official-store",
                SellerVerificationStatus.PENDING,
                null,
                "Vietcombank",
                "0071001234567",
                "NGUYEN VAN A",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        PageResponse<AdminSellerResponse> mockPageResponse = new PageResponse<>(
                List.of(sellerResponse), 0, 20, 1, 1, true
        );

        when(adminSellerService.getSellers("apple", SellerVerificationStatus.PENDING, 0, 20, "createdAt", "desc"))
                .thenReturn(mockPageResponse);

        ApiResponse<PageResponse<AdminSellerResponse>> response = adminSellerController.getSellers(
                "apple", SellerVerificationStatus.PENDING, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Seller list retrieved successfully.", response.getMessage());
        assertEquals(mockPageResponse, response.getData());
        verify(adminSellerService, times(1))
                .getSellers("apple", SellerVerificationStatus.PENDING, 0, 20, "createdAt", "desc");
    }

    @Test
    @DisplayName("TC-18: controller_getSellers_HasPreAuthorizeAdmin - Kiểm tra phân quyền @PreAuthorize(\"hasRole('ADMIN')\")")
    void controller_getSellers_HasPreAuthorizeAdmin() {
        PreAuthorize classPreAuth = AdminSellerController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classPreAuth, "AdminSellerController phải được gắn @PreAuthorize ở class-level");
        assertEquals("hasRole('ADMIN')", classPreAuth.value(), "PreAuthorize expression phải là hasRole('ADMIN')");
    }

    @Test
    @DisplayName("TC-19: security_getSellers_BuyerRole_Forbidden - Xác minh Role BUYER không thỏa mãn hasRole('ADMIN')")
    void security_getSellers_BuyerRole_Forbidden() {
        PreAuthorize classPreAuth = AdminSellerController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classPreAuth);
        String requiredRole = classPreAuth.value();

        assertFalse(requiredRole.contains("BUYER"), "Expression không được cấp quyền cho BUYER");
        assertTrue(requiredRole.contains("hasRole('ADMIN')"), "Expression chỉ chấp nhận ADMIN");
    }

    @Test
    @DisplayName("TC-20: security_getSellers_SellerRole_Forbidden - Xác minh Role SELLER không thỏa mãn hasRole('ADMIN')")
    void security_getSellers_SellerRole_Forbidden() {
        PreAuthorize classPreAuth = AdminSellerController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classPreAuth);
        String requiredRole = classPreAuth.value();

        assertFalse(requiredRole.contains("SELLER"), "Expression không được cấp quyền cho SELLER");
        assertTrue(requiredRole.contains("hasRole('ADMIN')"), "Expression chỉ chấp nhận ADMIN");
    }

    @Test
    @DisplayName("TC-21: controller_getSellerDetail_Success - Gọi getSellerDetail thành công trả về ApiResponse với đầy đủ thông tin")
    void controller_getSellerDetail_Success() {
        UUID sellerId = UUID.randomUUID();
        AdminSellerDetailResponse mockResponse = new AdminSellerDetailResponse(
                sellerId,
                "apple-official-store",
                SellerVerificationStatus.PENDING,
                null,
                "Vietcombank",
                "0071001234567",
                "NGUYEN VAN A",
                LocalDateTime.now(),
                LocalDateTime.now(),
                new AdminSellerDetailResponse.UserDetailResponse(
                        UUID.randomUUID(),
                        "Van A",
                        "Nguyen",
                        "Nguyen Van A",
                        "seller@techhub.vn",
                        true,
                        "0987654321",
                        "https://avatar.png",
                        UserStatus.ACTIVE,
                        Set.of(Role.BUYER),
                        LocalDateTime.now()
                ),
                new AdminSellerDetailResponse.ShopDetailResponse(
                        UUID.randomUUID(),
                        "Apple Official Store",
                        "Mô tả chi tiết shop",
                        "https://logo.png",
                        "https://banner.png",
                        ShopStatus.SUSPENDED,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                new AdminSellerDetailResponse.WarehouseAddressResponse(
                        UUID.randomUUID(),
                        "Nguyen Van Kho",
                        "0901234567",
                        "TP. Hồ Chí Minh",
                        "Quận 1",
                        "Phường Bến Nghé",
                        "Số 123 Lê Lợi",
                        "Số 123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                new AdminSellerDetailResponse.ShippingOptionResponse(
                        UUID.randomUUID(),
                        "Standard Delivery",
                        new BigDecimal("25000.00"),
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );

        when(adminSellerService.getSellerDetail(sellerId)).thenReturn(mockResponse);

        ApiResponse<AdminSellerDetailResponse> response = adminSellerController.getSellerDetail(sellerId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Seller details retrieved successfully.", response.getMessage());
        assertEquals(mockResponse, response.getData());
        verify(adminSellerService, times(1)).getSellerDetail(sellerId);
    }

    @Test
    @DisplayName("TC-22: controller_getSellerDetail_NotFound_ThrowsException - Khi không tìm thấy Seller ném ngoại lệ")
    void controller_getSellerDetail_NotFound_ThrowsException() {
        UUID notFoundId = UUID.randomUUID();
        when(adminSellerService.getSellerDetail(notFoundId)).thenThrow(new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminSellerController.getSellerDetail(notFoundId)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, exception.getErrorCode());
        verify(adminSellerService, times(1)).getSellerDetail(notFoundId);
    }

    // =========================================================================
    // TESTS FOR PATCH /api/admin/sellers/{id}/approval (updateSellerApproval)
    // =========================================================================

    @Test
    @DisplayName("TC-14: controller_updateSellerApproval_Success_Approve - Controller xử lý duyệt thành công")
    void controller_updateSellerApproval_Success_Approve() {
        UUID sellerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.APPROVED, null);

        AdminSellerApprovalResponse mockResponse = new AdminSellerApprovalResponse(
                sellerId,
                userId,
                shopId,
                "techhub-store",
                "TechHub Store",
                SellerVerificationStatus.APPROVED,
                ShopStatus.ACTIVE,
                null,
                Set.of(Role.BUYER, Role.SELLER),
                LocalDateTime.now()
        );

        when(adminSellerService.updateSellerApproval(sellerId, request)).thenReturn(mockResponse);

        ApiResponse<AdminSellerApprovalResponse> response = adminSellerController.updateSellerApproval(sellerId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Seller approval status updated successfully.", response.getMessage());
        assertEquals(mockResponse, response.getData());
        assertEquals(SellerVerificationStatus.APPROVED, response.getData().verificationStatus());
        assertEquals(ShopStatus.ACTIVE, response.getData().shopStatus());
        verify(adminSellerService, times(1)).updateSellerApproval(sellerId, request);
    }

    @Test
    @DisplayName("TC-15: controller_updateSellerApproval_Success_Reject - Controller xử lý từ chối thành công")
    void controller_updateSellerApproval_Success_Reject() {
        UUID sellerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        SellerApprovalRequest request = new SellerApprovalRequest(
                SellerVerificationStatus.REJECTED,
                "Hình ảnh giấy phép kinh doanh bị mờ"
        );

        AdminSellerApprovalResponse mockResponse = new AdminSellerApprovalResponse(
                sellerId,
                userId,
                shopId,
                "techhub-store",
                "TechHub Store",
                SellerVerificationStatus.REJECTED,
                ShopStatus.SUSPENDED,
                "Hình ảnh giấy phép kinh doanh bị mờ",
                Set.of(Role.BUYER),
                LocalDateTime.now()
        );

        when(adminSellerService.updateSellerApproval(sellerId, request)).thenReturn(mockResponse);

        ApiResponse<AdminSellerApprovalResponse> response = adminSellerController.updateSellerApproval(sellerId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Seller approval status updated successfully.", response.getMessage());
        assertEquals(mockResponse, response.getData());
        assertEquals(SellerVerificationStatus.REJECTED, response.getData().verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.getData().shopStatus());
        assertEquals("Hình ảnh giấy phép kinh doanh bị mờ", response.getData().rejectionReason());
        verify(adminSellerService, times(1)).updateSellerApproval(sellerId, request);
    }

    @Test
    @DisplayName("TC-16: controller_updateSellerApproval_Validation_NullStatus - Validation bắt buộc status không được null")
    void controller_updateSellerApproval_Validation_NullStatus() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        SellerApprovalRequest invalidRequest = new SellerApprovalRequest(null, "Lý do từ chối");

        Set<ConstraintViolation<SellerApprovalRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Approval status is required")));
    }

    @Test
    @DisplayName("TC-17: controller_updateSellerApproval_Validation_ReasonExceeds1000Chars - Validation bắt lỗi lý do dài quá 1000 ký tự")
    void controller_updateSellerApproval_Validation_ReasonExceeds1000Chars() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String longReason = "a".repeat(1001);
        SellerApprovalRequest invalidRequest = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, longReason);

        Set<ConstraintViolation<SellerApprovalRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("1000 characters")));
    }

    @Test
    @DisplayName("TC-18: controller_updateSellerApproval_InvalidStatusTransition_PropagatesException - Service ném lỗi được lan truyền qua Controller")
    void controller_updateSellerApproval_InvalidStatusTransition_PropagatesException() {
        UUID sellerId = UUID.randomUUID();
        SellerApprovalRequest request = new SellerApprovalRequest(SellerVerificationStatus.REJECTED, "Reason");

        when(adminSellerService.updateSellerApproval(sellerId, request))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSellerController.updateSellerApproval(sellerId, request));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(adminSellerService, times(1)).updateSellerApproval(sellerId, request);
    }
}

