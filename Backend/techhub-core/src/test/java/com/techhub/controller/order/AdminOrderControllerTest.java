package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CancelOrderRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.User;
import com.techhub.model.enums.*;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.order.AdminOrderService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminOrderController (GET /api/admin/orders)")
class AdminOrderControllerTest {

    @Mock
    private AdminOrderService adminOrderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@techhub.vn");
        user.setPassword("encoded_pass");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRoles(Set.of(Role.ADMIN));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Class level annotations: @RestController và @RequestMapping(\"/api/admin/orders\")")
    void controllerAnnotations_Correct() {
        Class<?> clazz = AdminOrderController.class;
        assertTrue(clazz.isAnnotationPresent(RestController.class));
        assertTrue(clazz.isAnnotationPresent(RequestMapping.class));
        RequestMapping reqMapping = clazz.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/admin/orders"}, reqMapping.value());
    }

    @Test
    @DisplayName("getAdminOrders method annotations: @GetMapping và @PreAuthorize(\"hasRole('ADMIN')\")")
    void getAdminOrders_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = AdminOrderController.class.getMethod(
                "getAdminOrders",
                UUID.class, UUID.class, OrderStatus.class, String.class,
                LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class
        );
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", preAuth.value());
    }

    @Test
    @DisplayName("getAdminOrders: Thành công trả về PageResponse<AdminOrderResponse>")
    void getAdminOrders_Success_ReturnsPageResponse() {
        UUID orderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        AdminOrderShopResponse shop = new AdminOrderShopResponse(shopId, "Apple Store", "apple-store", "seller@apple.com", "0987654321");
        AdminOrderBuyerResponse buyer = new AdminOrderBuyerResponse(UUID.randomUUID(), "Nguyen Van A", "a@test.com", "Nguyen Van A", "0987654321", "Ha Noi", "123 Cau Giay");
        AdminOrderItemResponse item = new AdminOrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "iPhone 15", "Black", "IPH-15", "img.png", BigDecimal.valueOf(20000000), BigDecimal.valueOf(19000000), 1, BigDecimal.valueOf(19000000));

        AdminOrderResponse orderResponse = new AdminOrderResponse(
                orderId, "ORD-20261015-01", OrderStatus.CONFIRMED, "Đã xác nhận",
                shop, buyer, 1, List.of(item),
                BigDecimal.valueOf(20000000), BigDecimal.valueOf(30000), BigDecimal.valueOf(1000000),
                BigDecimal.ZERO, BigDecimal.valueOf(950000), BigDecimal.valueOf(18050000), BigDecimal.valueOf(19030000),
                PaymentMethod.COD, "COD", PaymentStatus.PENDING, "Chưa thanh toán",
                "Giao nhanh", LocalDateTime.now(), LocalDateTime.now(), List.of("VIEW_DETAILS", "FORCE_CANCEL")
        );

        PageResponse<AdminOrderResponse> pageResponse = new PageResponse<>(
                List.of(orderResponse), 0, 10, 1L, 1, true
        );

        when(adminOrderService.getAdminOrders(
                eq(userId), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10), eq("createdAt"), eq("desc")
        )).thenReturn(pageResponse);

        ApiResponse<PageResponse<AdminOrderResponse>> response = adminOrderController.getAdminOrders(
                null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched platform orders successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals(orderId, response.getData().content().get(0).orderId());
    }

    @Test
    @DisplayName("getAdminOrders: Trả về trang rỗng khi không có đơn hàng")
    void getAdminOrders_Success_EmptyPage() {
        PageResponse<AdminOrderResponse> emptyPage = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0L, 0, true
        );

        when(adminOrderService.getAdminOrders(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(emptyPage);

        ApiResponse<PageResponse<AdminOrderResponse>> response = adminOrderController.getAdminOrders(
                null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().content().isEmpty());
    }

    @Test
    @DisplayName("getAdminOrders: Ném exception khi tài khoản Admin bị BANNED")
    void getAdminOrders_Throw_WhenUserBanned() {
        when(adminOrderService.getAdminOrders(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_BANNED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderController.getAdminOrders(null, null, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getAdminOrders: Ném exception khi startDate > endDate")
    void getAdminOrders_Throw_WhenDateRangeInvalid() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 10);

        when(adminOrderService.getAdminOrders(eq(userId), isNull(), isNull(), isNull(), isNull(), eq(start), eq(end), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderController.getAdminOrders(null, null, null, null, start, end, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("getAdminOrderDetail method annotations: @GetMapping(\"/{id}\") và @PreAuthorize(\"hasRole('ADMIN')\")")
    void getAdminOrderDetail_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = AdminOrderController.class.getMethod("getAdminOrderDetail", UUID.class);
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, getMapping.value());
        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", preAuth.value());
    }

    @Test
    @DisplayName("getAdminOrderDetail: Thành công trả về ApiResponse<AdminOrderDetailResponse>")
    void getAdminOrderDetail_Success_ReturnsOrderDetailResponse() {
        UUID orderId = UUID.randomUUID();
        AdminOrderShopResponse shop = new AdminOrderShopResponse(UUID.randomUUID(), "Apple Store", "apple-store", "seller@apple.com", "0987654321");
        AdminOrderBuyerResponse buyer = new AdminOrderBuyerResponse(UUID.randomUUID(), "Nguyen Van A", "a@test.com", "Nguyen Van A", "0987654321", "Ha Noi", "123 Cau Giay");
        AddressSnapshotResponse address = new AddressSnapshotResponse(UUID.randomUUID(), "Nguyen Van A", "0987654321", "123 Cau Giay", "Dich Vong", "Cau Giay", "Ha Noi", "123 Cau Giay, Dich Vong, Cau Giay, Ha Noi");
        AdminOrderItemResponse item = new AdminOrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "iPhone 15", "Black", "IPH-15", "img.png", BigDecimal.valueOf(20000000), BigDecimal.valueOf(19000000), 1, BigDecimal.valueOf(19000000));
        AdminOrderPaymentResponse payment = new AdminOrderPaymentResponse(UUID.randomUUID(), PaymentMethod.COD, "COD", PaymentStatus.PENDING, "Chưa thanh toán", null, null);

        AdminOrderDetailResponse detailResponse = new AdminOrderDetailResponse(
                orderId, "ORD-20261015-01", OrderStatus.CONFIRMED, "Đã xác nhận",
                shop, buyer, address, List.of(item), 1, "Giao nhanh",
                BigDecimal.valueOf(20000000), BigDecimal.valueOf(30000), BigDecimal.valueOf(1000000),
                BigDecimal.ZERO, BigDecimal.valueOf(950000), BigDecimal.valueOf(18050000), BigDecimal.valueOf(19030000),
                payment, LocalDateTime.now(), LocalDateTime.now(), null, null, null,
                Collections.emptyList(), List.of("VIEW_DETAILS", "FORCE_CANCEL")
        );

        when(adminOrderService.getAdminOrderDetail(eq(userId), eq(orderId))).thenReturn(detailResponse);

        ApiResponse<AdminOrderDetailResponse> response = adminOrderController.getAdminOrderDetail(orderId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched platform order details successfully", response.getMessage());
        assertEquals(orderId, response.getData().orderId());
        assertEquals("ORD-20261015-01", response.getData().orderNumber());
    }

    @Test
    @DisplayName("getAdminOrderDetail: Ném ORDER_NOT_FOUND khi đơn hàng không tồn tại")
    void getAdminOrderDetail_Throw_WhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(adminOrderService.getAdminOrderDetail(eq(userId), eq(orderId)))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderController.getAdminOrderDetail(orderId));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getAdminOrderDetail: Ném ACCOUNT_BANNED khi tài khoản Admin bị BANNED")
    void getAdminOrderDetail_Throw_WhenAdminBanned() {
        UUID orderId = UUID.randomUUID();
        when(adminOrderService.getAdminOrderDetail(eq(userId), eq(orderId)))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_BANNED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderController.getAdminOrderDetail(orderId));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    // =========================================================================================
    // POST /api/admin/orders/{id}/cancel Controller Tests (Phase 11 - Row 10)
    // =========================================================================================

    @Test
    @DisplayName("TC-17: cancelAdminOrder gọi Service và trả về ApiResponse success thành công")
    void cancelAdminOrder_Success_ReturnsApiResponse() {
        UUID orderId = UUID.randomUUID();
        CancelOrderRequest request = new CancelOrderRequest(CancellationReasonCode.SELLER_OUT_OF_STOCK, "Shop hết hàng");

        AdminOrderDetailResponse detailResponse = new AdminOrderDetailResponse(
                orderId, "ORD-20261015-01", OrderStatus.CANCELLED, "Đã hủy",
                null, null, null, Collections.emptyList(), 0,
                "[Admin Cancel - admin@techhub.vn]: SELLER_OUT_OF_STOCK - Shop hết hàng",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, LocalDateTime.now(), LocalDateTime.now(), null, null, LocalDateTime.now(),
                Collections.emptyList(), List.of("VIEW_DETAILS")
        );

        when(adminOrderService.cancelAdminOrder(eq(userId), eq(orderId), eq(request))).thenReturn(detailResponse);

        ApiResponse<AdminOrderDetailResponse> response = adminOrderController.cancelAdminOrder(orderId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Order cancelled successfully by administrator", response.getMessage());
        assertEquals(orderId, response.getData().orderId());
        assertEquals(OrderStatus.CANCELLED, response.getData().status());
        verify(adminOrderService).cancelAdminOrder(eq(userId), eq(orderId), eq(request));
    }

    @Test
    @DisplayName("TC-18: Bean Validation bắt lỗi khi reasonCode bị null")
    void cancelAdminOrder_Validation_FailsWhenReasonCodeIsNull() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        CancelOrderRequest invalidRequest = new CancelOrderRequest(null, "Lý do huỷ");
        Set<ConstraintViolation<CancelOrderRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reasonCode")));
    }

    @Test
    @DisplayName("TC-19: Bean Validation bắt lỗi khi reasonNote vượt quá 500 ký tự")
    void cancelAdminOrder_Validation_FailsWhenReasonNoteExceeds500Chars() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String longNote = "A".repeat(501);
        CancelOrderRequest invalidRequest = new CancelOrderRequest(CancellationReasonCode.OTHER, longNote);
        Set<ConstraintViolation<CancelOrderRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reasonNote")));
    }

    @Test
    @DisplayName("TC-20: Kiểm tra metadata bảo mật @PreAuthorize('hasRole(\\'ADMIN\\')') và @PostMapping('/{id}/cancel')")
    void cancelAdminOrder_SecurityAnnotation_HasAdminPreAuthorize() throws NoSuchMethodException {
        Method method = AdminOrderController.class.getMethod("cancelAdminOrder", UUID.class, CancelOrderRequest.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method cancelAdminOrder must be annotated with @PreAuthorize");
        assertEquals("hasRole('ADMIN')", preAuthorize.value());

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "Method cancelAdminOrder must be annotated with @PostMapping");
        assertArrayEquals(new String[]{"/{id}/cancel"}, postMapping.value());
    }
}
