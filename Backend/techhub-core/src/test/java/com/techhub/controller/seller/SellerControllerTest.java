package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.RegisterSellerRequest;
import com.techhub.model.dto.request.UpdateSellerProfileRequest;
import com.techhub.model.dto.request.UpdateSellerShopRequest;
import com.techhub.model.dto.request.UpdateShippingOptionRequest;
import com.techhub.model.dto.request.UpdateShopStatusRequest;
import com.techhub.model.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.model.dto.response.SellerProfileResponse;
import com.techhub.model.dto.response.SellerRegistrationResponse;
import com.techhub.model.dto.response.SellerShippingOptionResponse;
import com.techhub.model.dto.response.SellerShopResponse;
import com.techhub.model.dto.response.SellerVerificationStatusResponse;
import com.techhub.model.dto.response.SellerWarehouseAddressResponse;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.service.seller.SellerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerController")
class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @InjectMocks
    private SellerController sellerController;

    @Test
    @DisplayName("registerSeller: Đăng ký mới thành công trả về HTTP 201 Created")
    void registerSeller_NewApplication_ReturnsCreatedResponse() {
        RegisterSellerRequest request = new RegisterSellerRequest(
                "apple-store-vn",
                "Apple Store VN",
                "Mô tả",
                null,
                null,
                "Vietcombank",
                "0123456789",
                "DOAN THANG LOI",
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn",
                new BigDecimal("30000.00")
        );

        SellerRegistrationResponse responseDto = new SellerRegistrationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                SellerVerificationStatus.PENDING,
                ShopStatus.SUSPENDED,
                "Vietcombank",
                "0123456789",
                "DOAN THANG LOI",
                "123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                new BigDecimal("30000.00"),
                LocalDateTime.now(),
                false
        );

        when(sellerService.registerSeller(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerRegistrationResponse>> response = sellerController.registerSeller(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller registration submitted successfully. Please wait for administrator approval.", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        verify(sellerService, times(1)).registerSeller(request);
    }

    @Test
    @DisplayName("registerSeller: Re-submit thành công trả về HTTP 200 OK")
    void registerSeller_Resubmit_ReturnsOkResponse() {
        RegisterSellerRequest request = new RegisterSellerRequest(
                "apple-store-vn",
                "Apple Store VN",
                "Mô tả",
                null,
                null,
                "Vietcombank",
                "0123456789",
                "DOAN THANG LOI",
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn",
                new BigDecimal("30000.00")
        );

        SellerRegistrationResponse responseDto = new SellerRegistrationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                SellerVerificationStatus.PENDING,
                ShopStatus.SUSPENDED,
                "Vietcombank",
                "0123456789",
                "DOAN THANG LOI",
                "123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                new BigDecimal("30000.00"),
                LocalDateTime.now(),
                true
        );

        when(sellerService.registerSeller(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerRegistrationResponse>> response = sellerController.registerSeller(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller application re-submitted successfully. Status reset to PENDING.", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        verify(sellerService, times(1)).registerSeller(request);
    }

    @Test
    @DisplayName("getVerificationStatus: Chưa đăng ký bán hàng -> trả về HTTP 200 OK (hasApplied = false)")
    void getVerificationStatus_UserNotApplied_ReturnsOkResponse() {
        SellerVerificationStatusResponse responseDto = SellerVerificationStatusResponse.notApplied();
        when(sellerService.getVerificationStatus()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerVerificationStatusResponse>> response = sellerController.getVerificationStatus();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User has not submitted a seller registration", response.getBody().getMessage());
        assertFalse(response.getBody().getData().hasApplied());
        verify(sellerService, times(1)).getVerificationStatus();
    }

    @Test
    @DisplayName("getVerificationStatus: Đang chờ duyệt PENDING -> trả về HTTP 200 OK")
    void getVerificationStatus_Pending_ReturnsOkResponse() {
        SellerVerificationStatusResponse responseDto = new SellerVerificationStatusResponse(
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                SellerVerificationStatus.PENDING,
                ShopStatus.SUSPENDED,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(sellerService.getVerificationStatus()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerVerificationStatusResponse>> response = sellerController.getVerificationStatus();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller application is pending review", response.getBody().getMessage());
        assertTrue(response.getBody().getData().hasApplied());
        assertEquals(SellerVerificationStatus.PENDING, response.getBody().getData().verificationStatus());
    }

    @Test
    @DisplayName("getVerificationStatus: Bị từ chối REJECTED -> trả về HTTP 200 OK kèm message rejected")
    void getVerificationStatus_Rejected_ReturnsOkResponse() {
        SellerVerificationStatusResponse responseDto = new SellerVerificationStatusResponse(
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                SellerVerificationStatus.REJECTED,
                ShopStatus.SUSPENDED,
                "Sai số tài khoản",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(sellerService.getVerificationStatus()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerVerificationStatusResponse>> response = sellerController.getVerificationStatus();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller application was rejected", response.getBody().getMessage());
        assertEquals("Sai số tài khoản", response.getBody().getData().rejectionReason());
    }

    @Test
    @DisplayName("getVerificationStatus: Đã duyệt APPROVED -> trả về HTTP 200 OK kèm message approved")
    void getVerificationStatus_Approved_ReturnsOkResponse() {
        SellerVerificationStatusResponse responseDto = new SellerVerificationStatusResponse(
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                SellerVerificationStatus.APPROVED,
                ShopStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(sellerService.getVerificationStatus()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerVerificationStatusResponse>> response = sellerController.getVerificationStatus();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller application is approved", response.getBody().getMessage());
        assertEquals(ShopStatus.ACTIVE, response.getBody().getData().shopStatus());
    }

    // =========================================================================
    // GET /api/seller/profile TESTS
    // =========================================================================

    @Test
    @DisplayName("getSellerProfile: Lấy thông tin hồ sơ Người bán thành công -> trả về HTTP 200 OK")
    void getSellerProfile_Success_ReturnsOkResponse() {
        SellerProfileResponse responseDto = new SellerProfileResponse(
                UUID.randomUUID(),
                "apple-flagship-store",
                SellerVerificationStatus.APPROVED,
                "Vietcombank",
                "0123456789",
                "NGUYEN VAN A",
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1)
        );
        when(sellerService.getSellerProfile()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerProfileResponse>> response = sellerController.getSellerProfile();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller profile retrieved successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).getSellerProfile();
    }

    @Test
    @DisplayName("getSellerProfile: Service ném BusinessException -> Controller propagate ngoại lệ")
    void getSellerProfile_ServiceThrowsException_PropagatesException() {
        when(sellerService.getSellerProfile())
                .thenThrow(new BusinessException(ErrorCode.NOT_A_SELLER));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.getSellerProfile());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(sellerService, times(1)).getSellerProfile();
    }

    @Test
    @DisplayName("getSellerProfile: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void getSellerProfile_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("getSellerProfile");
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getSellerProfile phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    // =========================================================================
    // PUT /api/seller/profile TESTS
    // =========================================================================

    @Test
    @DisplayName("updateSellerProfile: Cập nhật thành công -> trả về HTTP 200 OK")
    void updateSellerProfile_Success_ReturnsOkResponse() {
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store",
                "Vietcombank",
                "9876543210",
                "NGUYEN VAN A"
        );

        SellerProfileResponse responseDto = new SellerProfileResponse(
                UUID.randomUUID(),
                "apple-official-store",
                SellerVerificationStatus.APPROVED,
                "Vietcombank",
                "9876543210",
                "NGUYEN VAN A",
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now()
        );

        when(sellerService.updateSellerProfile(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerProfileResponse>> response = sellerController.updateSellerProfile(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Seller profile updated successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).updateSellerProfile(request);
    }

    @Test
    @DisplayName("updateSellerProfile: Service ném BusinessException (ví dụ trùng slug) -> Controller propagate ngoại lệ")
    void updateSellerProfile_ServiceThrowsException_PropagatesException() {
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "samsung-store",
                "Vietcombank",
                "9876543210",
                "NGUYEN VAN A"
        );

        when(sellerService.updateSellerProfile(request))
                .thenThrow(new BusinessException(ErrorCode.SELLER_NAME_ALREADY_EXISTS));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.updateSellerProfile(request));

        assertEquals(ErrorCode.SELLER_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(sellerService, times(1)).updateSellerProfile(request);
    }

    @Test
    @DisplayName("updateSellerProfile: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void updateSellerProfile_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("updateSellerProfile", UpdateSellerProfileRequest.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateSellerProfile phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    // ==========================================
    // GET /api/seller/shop: Lấy thông tin Shop cá nhân
    // ==========================================

    @Test
    @DisplayName("getShopInfo: Lấy thông tin Shop thành công trả về HTTP 200 OK")
    void getShopInfo_Success_ReturnsOkResponse() {
        SellerShopResponse responseDto = new SellerShopResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-flagship-store",
                "Apple Flagship Store VN",
                "Cửa hàng chính hãng",
                "https://techhub.vn/avatar.png",
                "https://techhub.vn/banner.png",
                ShopStatus.ACTIVE,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(5)
        );

        when(sellerService.getShopInfo()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerShopResponse>> response = sellerController.getShopInfo();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shop details retrieved successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).getShopInfo();
    }

    @Test
    @DisplayName("getShopInfo: Service ném BusinessException (SHOP_NOT_FOUND) -> Controller propagate ngoại lệ")
    void getShopInfo_ServiceThrowsException_PropagatesException() {
        when(sellerService.getShopInfo()).thenThrow(new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.getShopInfo());

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(sellerService, times(1)).getShopInfo();
    }

    @Test
    @DisplayName("getShopInfo: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void getShopInfo_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("getShopInfo");
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getShopInfo phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    // ==========================================
    // PUT /api/seller/shop: Cập nhật thông tin Shop cá nhân
    // ==========================================

    @Test
    @DisplayName("updateShopInfo: Cập nhật thông tin Shop thành công trả về HTTP 200 OK")
    void updateShopInfo_Success_ReturnsOkResponse() {
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Apple Flagship Store VN",
                "Mô tả mới",
                "https://techhub.vn/new_avatar.png",
                "https://techhub.vn/new_banner.png"
        );

        SellerShopResponse responseDto = new SellerShopResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-flagship-store",
                "Apple Flagship Store VN",
                "Mô tả mới",
                "https://techhub.vn/new_avatar.png",
                "https://techhub.vn/new_banner.png",
                ShopStatus.ACTIVE,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        when(sellerService.updateShopInfo(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerShopResponse>> response = sellerController.updateShopInfo(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shop details updated successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).updateShopInfo(request);
    }

    @Test
    @DisplayName("updateShopInfo: Service ném BusinessException (ACCOUNT_BANNED) -> Controller propagate ngoại lệ")
    void updateShopInfo_ServiceThrowsException_PropagatesException() {
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Banned Shop", null, null, null
        );

        when(sellerService.updateShopInfo(request)).thenThrow(new BusinessException(ErrorCode.ACCOUNT_BANNED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerService, times(1)).updateShopInfo(request);
    }

    @Test
    @DisplayName("updateShopInfo: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void updateShopInfo_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("updateShopInfo", UpdateSellerShopRequest.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateShopInfo phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("updateShopStatus: Cập nhật trạng thái thành công trả về HTTP 200 OK")
    void updateShopStatus_Success() {
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        SellerShopResponse responseDto = new SellerShopResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "apple-flagship-store",
                "Apple Flagship Store VN",
                "Mô tả",
                "https://techhub.vn/avatar.png",
                "https://techhub.vn/banner.png",
                ShopStatus.PAUSED,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        when(sellerService.updateShopStatus(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerShopResponse>> response = sellerController.updateShopStatus(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shop status updated successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).updateShopStatus(request);
    }

    @Test
    @DisplayName("updateShopStatus: Service ném BusinessException (INVALID_SHOP_STATUS_TRANSITION) -> Controller propagate ngoại lệ")
    void updateShopStatus_ServiceThrowsException_PropagatesException() {
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.SUSPENDED);

        when(sellerService.updateShopStatus(request)).thenThrow(new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.updateShopStatus(request));

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(sellerService, times(1)).updateShopStatus(request);
    }

    @Test
    @DisplayName("updateShopStatus: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void updateShopStatus_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("updateShopStatus", UpdateShopStatusRequest.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateShopStatus phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("getWarehouseAddress: Lấy địa chỉ kho thành công trả về HTTP 200 OK")
    void getWarehouseAddress_Success() {
        SellerWarehouseAddressResponse responseDto = new SellerWarehouseAddressResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn",
                "123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(5)
        );

        when(sellerService.getWarehouseAddress()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerWarehouseAddressResponse>> response = sellerController.getWarehouseAddress();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Warehouse address retrieved successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).getWarehouseAddress();
    }

    @Test
    @DisplayName("getWarehouseAddress: Service ném BusinessException (ADDRESS_NOT_FOUND) -> Controller propagate ngoại lệ")
    void getWarehouseAddress_ServiceThrowsException_PropagatesException() {
        when(sellerService.getWarehouseAddress()).thenThrow(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.getWarehouseAddress());

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(sellerService, times(1)).getWarehouseAddress();
    }

    @Test
    @DisplayName("getWarehouseAddress: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void getWarehouseAddress_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("getWarehouseAddress");
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getWarehouseAddress phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    // =========================================================================
    // PUT /api/seller/shop/address TESTS
    // =========================================================================

    @Test
    @DisplayName("updateWarehouseAddress: Cập nhật địa chỉ kho thành công trả về HTTP 200 OK")
    void updateWarehouseAddress_Success() {
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        SellerWarehouseAddressResponse responseDto = new SellerWarehouseAddressResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn",
                "123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        when(sellerService.updateWarehouseAddress(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerWarehouseAddressResponse>> response = sellerController.updateWarehouseAddress(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Warehouse address updated successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).updateWarehouseAddress(request);
    }

    @Test
    @DisplayName("updateWarehouseAddress: Service ném BusinessException (ACCOUNT_BANNED) -> Controller propagate ngoại lệ")
    void updateWarehouseAddress_ServiceException() {
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        when(sellerService.updateWarehouseAddress(request)).thenThrow(new BusinessException(ErrorCode.ACCOUNT_BANNED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerService, times(1)).updateWarehouseAddress(request);
    }

    @Test
    @DisplayName("updateWarehouseAddress: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void updateWarehouseAddress_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("updateWarehouseAddress", UpdateWarehouseAddressRequest.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateWarehouseAddress phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    // =========================================================================
    // GET /api/seller/shop/shipping-option TESTS
    // =========================================================================

    @Test
    @DisplayName("getShippingOption: Lấy cấu hình phí ship thành công trả về HTTP 200 OK")
    void getShippingOption_Success() {
        SellerShippingOptionResponse responseDto = new SellerShippingOptionResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Standard Delivery",
                new BigDecimal("25000.00"),
                true,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(5)
        );

        when(sellerService.getShippingOption()).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerShippingOptionResponse>> response = sellerController.getShippingOption();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shipping option retrieved successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).getShippingOption();
    }

    @Test
    @DisplayName("getShippingOption: Service ném BusinessException (SHIPPING_OPTION_NOT_FOUND) -> Controller propagate ngoại lệ")
    void getShippingOption_ServiceException() {
        when(sellerService.getShippingOption()).thenThrow(new BusinessException(ErrorCode.SHIPPING_OPTION_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.getShippingOption());

        assertEquals(ErrorCode.SHIPPING_OPTION_NOT_FOUND, ex.getErrorCode());
        verify(sellerService, times(1)).getShippingOption();
    }

    @Test
    @DisplayName("getShippingOption: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void getShippingOption_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("getShippingOption");
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getShippingOption phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }

    @Test
    @DisplayName("updateShippingOption: Cập nhật cước phí vận chuyển thành công trả về HTTP 200 OK")
    void updateShippingOption_Success() {
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));
        SellerShippingOptionResponse responseDto = new SellerShippingOptionResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Standard Delivery",
                new BigDecimal("22000.00"),
                true,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        when(sellerService.updateShippingOption(request)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<SellerShippingOptionResponse>> response = sellerController.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shipping fee updated successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

        verify(sellerService, times(1)).updateShippingOption(request);
    }

    @Test
    @DisplayName("updateShippingOption: Service ném BusinessException (SHIPPING_OPTION_NOT_FOUND) -> Controller propagate ngoại lệ")
    void updateShippingOption_ServiceException() {
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(sellerService.updateShippingOption(request)).thenThrow(new BusinessException(ErrorCode.SHIPPING_OPTION_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerController.updateShippingOption(request));

        assertEquals(ErrorCode.SHIPPING_OPTION_NOT_FOUND, ex.getErrorCode());
        verify(sellerService, times(1)).updateShippingOption(request);
    }

    @Test
    @DisplayName("updateShippingOption: Đảm bảo method có gắn annotation @PreAuthorize(\"hasRole('SELLER')\")")
    void updateShippingOption_HasPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = SellerController.class.getMethod("updateShippingOption", UpdateShippingOptionRequest.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateShippingOption phải có @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value());
    }
}


