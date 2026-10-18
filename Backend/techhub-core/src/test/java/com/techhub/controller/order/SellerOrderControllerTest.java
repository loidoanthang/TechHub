package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.SellerOrderBuyerResponse;
import com.techhub.model.dto.response.SellerOrderItemResponse;
import com.techhub.model.dto.response.SellerOrderResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.order.SellerOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.techhub.model.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.model.dto.response.SellerOrderDetailResponse;

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
@DisplayName("Unit Tests cho SellerOrderController (GET /api/seller/orders)")
class SellerOrderControllerTest {

    @Mock
    private SellerOrderService sellerOrderService;

    @InjectMocks
    private SellerOrderController sellerOrderController;

    private UUID userId;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Kiểm tra annotation cấp lớp của SellerOrderController (@RestController, @RequestMapping)")
    void controller_classAnnotations_Correct() {
        assertTrue(SellerOrderController.class.isAnnotationPresent(RestController.class),
                "SellerOrderController phải có annotation @RestController");

        RequestMapping requestMapping = SellerOrderController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "SellerOrderController phải có annotation @RequestMapping");
        assertArrayEquals(new String[]{"/api/seller/orders"}, requestMapping.value(),
                "Endpoint phải là /api/seller/orders");
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức getSellerOrders (@GetMapping, @PreAuthorize)")
    void getSellerOrders_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = SellerOrderController.class.getMethod("getSellerOrders",
                OrderStatus.class, String.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getSellerOrders phải có annotation @GetMapping");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method getSellerOrders phải có annotation @PreAuthorize");
        assertEquals("hasRole('SELLER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('SELLER')");
    }

    @Test
    @DisplayName("getSellerOrders_Success_Returns200WithOrders")
    void getSellerOrders_Success_Returns200WithOrders() {
        SellerOrderBuyerResponse buyer = new SellerOrderBuyerResponse(
                UUID.randomUUID(), "buyer_user", "Nguyen Van A", "0987654321", "Ha Noi", "123 Cau Giay"
        );
        SellerOrderItemResponse item = new SellerOrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Apple 20W Adapter", "White", "APL-20W",
                "https://img.techhub.vn/apple.png", BigDecimal.valueOf(500000), BigDecimal.valueOf(470000), 1, BigDecimal.valueOf(470000)
        );
        SellerOrderResponse order = new SellerOrderResponse(
                UUID.randomUUID(), "ORD-20261015-8F3A", OrderStatus.CONFIRMED, "Đã xác nhận",
                buyer, 1, List.of(item), BigDecimal.valueOf(500000), BigDecimal.valueOf(30000),
                BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.valueOf(23500),
                BigDecimal.valueOf(446500), BigDecimal.valueOf(500000), PaymentMethod.COD,
                "Thanh toán khi nhận hàng (COD)", PaymentStatus.PENDING, "Chưa thanh toán",
                "Giao som nhe", LocalDateTime.now(), LocalDateTime.now(), List.of("START_PREPARING", "CANCEL_ORDER")
        );
        PageResponse<SellerOrderResponse> pageResponse = new PageResponse<>(
                List.of(order), 0, 10, 1L, 1, true
        );

        when(sellerOrderService.getSellerOrders(eq(userId), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(pageResponse);

        ApiResponse<PageResponse<SellerOrderResponse>> response =
                sellerOrderController.getSellerOrders(null, null, null, null, 0, 10, "createdAt", "desc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched seller orders successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals("ORD-20261015-8F3A", response.getData().content().get(0).orderNumber());
        assertEquals(OrderStatus.CONFIRMED, response.getData().content().get(0).status());
        assertEquals(BigDecimal.valueOf(446500), response.getData().content().get(0).totalSellerEarnings());

        verify(sellerOrderService, times(1))
                .getSellerOrders(eq(userId), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10), eq("createdAt"), eq("desc"));
    }

    @Test
    @DisplayName("getSellerOrders_Success_WithFilterParameters")
    void getSellerOrders_Success_WithFilterParameters() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 15);
        PageResponse<SellerOrderResponse> emptyPage = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0L, 0, true
        );

        when(sellerOrderService.getSellerOrders(eq(userId), eq(OrderStatus.PREPARING), eq("ORD-2026"), eq(startDate), eq(endDate), eq(1), eq(20), eq("totalAmount"), eq("asc")))
                .thenReturn(emptyPage);

        ApiResponse<PageResponse<SellerOrderResponse>> response =
                sellerOrderController.getSellerOrders(OrderStatus.PREPARING, "ORD-2026", startDate, endDate, 1, 20, "totalAmount", "asc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().content().isEmpty());

        verify(sellerOrderService, times(1))
                .getSellerOrders(eq(userId), eq(OrderStatus.PREPARING), eq("ORD-2026"), eq(startDate), eq(endDate), eq(1), eq(20), eq("totalAmount"), eq("asc"));
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenServiceThrowsBusinessException")
    void getSellerOrders_Throw_WhenServiceThrowsBusinessException() {
        when(sellerOrderService.getSellerOrders(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.NOT_A_SELLER));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.getSellerOrders(null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.NOT_A_SELLER, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenInvalidDateRange")
    void getSellerOrders_Throw_WhenInvalidDateRange() {
        LocalDate startDate = LocalDate.of(2026, 10, 20);
        LocalDate endDate = LocalDate.of(2026, 10, 10);

        when(sellerOrderService.getSellerOrders(any(), any(), any(), eq(startDate), eq(endDate), anyInt(), anyInt(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.getSellerOrders(null, null, startDate, endDate, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức getSellerOrderDetail (@GetMapping(\"/{id}\"), @PreAuthorize)")
    void getSellerOrderDetail_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = SellerOrderController.class.getMethod("getSellerOrderDetail", UUID.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getSellerOrderDetail phải có annotation @GetMapping");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, getMapping.value(),
                "GetMapping path phải là /{id}");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method getSellerOrderDetail phải có annotation @PreAuthorize");
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('SELLER')", preAuth.value(),
                "PreAuthorize phải yêu cầu hasRole('SELLER')");
    }

    @Test
    @DisplayName("getSellerOrderDetail_Success_ReturnsOrderDetails")
    void getSellerOrderDetail_Success_ReturnsOrderDetails() {
        UUID orderId = UUID.randomUUID();
        com.techhub.model.dto.response.SellerOrderDetailResponse mockDetail = new com.techhub.model.dto.response.SellerOrderDetailResponse(
                orderId,
                "ORD-20261015-8F3A2B1C90",
                OrderStatus.CONFIRMED,
                "Đã xác nhận (Chờ chuẩn bị hàng)",
                UUID.randomUUID(),
                "Apple Official Store",
                null,
                null,
                Collections.emptyList(),
                0,
                "Giao giờ hành chính",
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(30000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(23500),
                BigDecimal.valueOf(446500),
                BigDecimal.valueOf(500000),
                PaymentMethod.COD,
                "Thanh toán khi nhận hàng (COD)",
                PaymentStatus.PENDING,
                "Chưa thanh toán (Thu tiền mặt khi giao)",
                LocalDateTime.now(),
                null,
                null,
                null,
                Collections.emptyList(),
                List.of("START_PREPARING", "CANCEL_ORDER")
        );

        when(sellerOrderService.getSellerOrderDetail(userId, orderId)).thenReturn(mockDetail);

        ApiResponse<com.techhub.model.dto.response.SellerOrderDetailResponse> response =
                sellerOrderController.getSellerOrderDetail(orderId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("ORD-20261015-8F3A2B1C90", response.getData().orderNumber());
        assertEquals(OrderStatus.CONFIRMED, response.getData().status());
        assertEquals(0, BigDecimal.valueOf(446500).compareTo(response.getData().totalSellerEarnings()));

        verify(sellerOrderService, times(1)).getSellerOrderDetail(userId, orderId);
    }

    @Test
    @DisplayName("getSellerOrderDetail_Throw_WhenOrderNotFound")
    void getSellerOrderDetail_Throw_WhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(sellerOrderService.getSellerOrderDetail(userId, orderId))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.getSellerOrderDetail(orderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("getSellerOrderDetail_Throw_WhenNotApprovedSeller")
    void getSellerOrderDetail_Throw_WhenNotApprovedSeller() {
        UUID orderId = UUID.randomUUID();
        when(sellerOrderService.getSellerOrderDetail(userId, orderId))
                .thenThrow(new BusinessException(ErrorCode.NOT_A_SELLER));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.getSellerOrderDetail(orderId));

        assertEquals(ErrorCode.NOT_A_SELLER, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức updateSellerOrderStatus (@PatchMapping(\"/{id}/status\"), @PreAuthorize)")
    void updateSellerOrderStatus_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = SellerOrderController.class.getMethod("updateSellerOrderStatus", UUID.class, UpdateSellerOrderStatusRequest.class);

        assertTrue(method.isAnnotationPresent(PatchMapping.class),
                "Method updateSellerOrderStatus phải có annotation @PatchMapping");
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertArrayEquals(new String[]{"/{id}/status"}, patchMapping.value(),
                "PatchMapping path phải là /{id}/status");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method updateSellerOrderStatus phải có annotation @PreAuthorize");
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('SELLER')", preAuth.value(),
                "PreAuthorize phải yêu cầu hasRole('SELLER')");
    }

    @Test
    @DisplayName("updateSellerOrderStatus_Success_ReturnsOrderDetailResponse")
    void updateSellerOrderStatus_Success_ReturnsOrderDetailResponse() {
        UUID orderId = UUID.randomUUID();
        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, "Đang đóng gói");

        SellerOrderDetailResponse mockDetail = new SellerOrderDetailResponse(
                orderId,
                "ORD-20261015-8F3A2B1C90",
                OrderStatus.PREPARING,
                "Đang chuẩn bị hàng",
                UUID.randomUUID(),
                "Apple Official Store",
                null,
                null,
                Collections.emptyList(),
                0,
                "Giao giờ hành chính | [Shop]: Đang đóng gói",
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(30000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(23500),
                BigDecimal.valueOf(446500),
                BigDecimal.valueOf(500000),
                PaymentMethod.COD,
                "Thanh toán khi nhận hàng (COD)",
                PaymentStatus.PENDING,
                "Chưa thanh toán (Thu tiền mặt khi giao)",
                LocalDateTime.now(),
                null,
                null,
                null,
                Collections.emptyList(),
                List.of("WAITING_FOR_PICKUP")
        );

        when(sellerOrderService.updateSellerOrderStatus(userId, orderId, request)).thenReturn(mockDetail);

        ApiResponse<SellerOrderDetailResponse> response =
                sellerOrderController.updateSellerOrderStatus(orderId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Updated order fulfillment status successfully", response.getMessage());
        assertEquals("ORD-20261015-8F3A2B1C90", response.getData().orderNumber());
        assertEquals(OrderStatus.PREPARING, response.getData().status());
        assertEquals(0, BigDecimal.valueOf(446500).compareTo(response.getData().totalSellerEarnings()));

        verify(sellerOrderService, times(1)).updateSellerOrderStatus(userId, orderId, request);
    }

    @Test
    @DisplayName("updateSellerOrderStatus_Throw_WhenOrderNotFound")
    void updateSellerOrderStatus_Throw_WhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);

        when(sellerOrderService.updateSellerOrderStatus(userId, orderId, request))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.updateSellerOrderStatus(orderId, request));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("updateSellerOrderStatus_Throw_WhenInvalidStatusTransition")
    void updateSellerOrderStatus_Throw_WhenInvalidStatusTransition() {
        UUID orderId = UUID.randomUUID();
        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.DELIVERED, null);

        when(sellerOrderService.updateSellerOrderStatus(userId, orderId, request))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.updateSellerOrderStatus(orderId, request));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("processCancellationRequest_Success_WhenApproved")
    void processCancellationRequest_Success_WhenApproved() {
        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(com.techhub.model.enums.CancellationRequestStatus.APPROVED, "Đồng ý");

        com.techhub.model.dto.response.CancellationRequestResponse mockResponse =
                new com.techhub.model.dto.response.CancellationRequestResponse(
                        requestId, orderId, "ORD-123", com.techhub.model.enums.CancellationRequestStatus.APPROVED,
                        "Đã chấp thuận hủy đơn", "Đổi ý", "Đồng ý",
                        java.time.LocalDateTime.now().minusHours(1), java.time.LocalDateTime.now()
                );

        when(sellerOrderService.processCancellationRequest(userId, requestId, request)).thenReturn(mockResponse);

        ApiResponse<com.techhub.model.dto.response.CancellationRequestResponse> response =
                sellerOrderController.processCancellationRequest(requestId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(com.techhub.model.enums.CancellationRequestStatus.APPROVED, response.getData().status());
        verify(sellerOrderService).processCancellationRequest(userId, requestId, request);
    }

    @Test
    @DisplayName("processCancellationRequest_Throw_WhenAlreadyProcessed")
    void processCancellationRequest_Throw_WhenAlreadyProcessed() {
        UUID requestId = UUID.randomUUID();
        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(com.techhub.model.enums.CancellationRequestStatus.REJECTED, "Không đồng ý");

        when(sellerOrderService.processCancellationRequest(userId, requestId, request))
                .thenThrow(new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_PROCESSED));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderController.processCancellationRequest(requestId, request));

        assertEquals(ErrorCode.CANCELLATION_REQUEST_ALREADY_PROCESSED, exception.getErrorCode());
    }
}
