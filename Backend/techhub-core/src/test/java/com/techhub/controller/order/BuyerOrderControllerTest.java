package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.AddressSnapshotResponse;
import com.techhub.model.dto.response.BuyerOrderDetailResponse;
import com.techhub.model.dto.response.BuyerOrderItemResponse;
import com.techhub.model.dto.response.BuyerOrderResponse;
import com.techhub.model.dto.response.ReorderResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.order.OrderService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.techhub.model.dto.request.CreateCancellationRequest;
import com.techhub.model.dto.response.CancellationRequestResponse;
import com.techhub.model.enums.CancellationRequestStatus;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerOrderController (GET /api/buyer/orders)")
class BuyerOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private BuyerOrderController buyerOrderController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp lớp của BuyerOrderController (@RestController, @RequestMapping)")
    void controller_classAnnotations_Correct() {
        assertTrue(BuyerOrderController.class.isAnnotationPresent(RestController.class),
                "BuyerOrderController phải có annotation @RestController");

        RequestMapping requestMapping = BuyerOrderController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "BuyerOrderController phải có annotation @RequestMapping");
        assertArrayEquals(new String[]{"/api/buyer/orders"}, requestMapping.value(),
                "Endpoint phải là /api/buyer/orders");
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức getBuyerOrders (@GetMapping, @PreAuthorize)")
    void getBuyerOrders_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = BuyerOrderController.class.getMethod("getBuyerOrders",
                OrderStatus.class, String.class, int.class, int.class, String.class, String.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getBuyerOrders phải có annotation @GetMapping");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method getBuyerOrders phải có annotation @PreAuthorize");
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Chỉ người dùng có quyền ROLE_BUYER mới được phép gọi API");
    }

    @Test
    @DisplayName("getBuyerOrders thành công: gọi OrderService và trả về ApiResponse chuẩn")
    void getBuyerOrders_Success() {
        UUID orderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        BuyerOrderItemResponse item = new BuyerOrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Củ sạc 20W", "Trắng", "SKU-20W",
                "http://img.png", new BigDecimal("500000.00"), 1,
                BigDecimal.ZERO, new BigDecimal("500000.00"), new BigDecimal("500000.00")
        );

        BuyerOrderResponse orderResponse = new BuyerOrderResponse(
                orderId, "ORD-20261015-001", shopId, "Apple Official Store", "apple-official-store",
                OrderStatus.CONFIRMED, "Đã xác nhận", PaymentMethod.COD, PaymentStatus.PENDING,
                new BigDecimal("500000.00"), new BigDecimal("30000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("530000.00"),
                1, List.of(item), LocalDateTime.now(), null, null, null,
                List.of("REQUEST_CANCELLATION")
        );

        PageResponse<BuyerOrderResponse> pageResponse = new PageResponse<>(
                List.of(orderResponse), 0, 10, 1L, 1, true
        );

        when(orderService.getBuyerOrders(eq(userId), eq(OrderStatus.CONFIRMED), eq("Apple"), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(pageResponse);

        ApiResponse<PageResponse<BuyerOrderResponse>> response = buyerOrderController.getBuyerOrders(
                OrderStatus.CONFIRMED, "Apple", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched buyer orders successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().content().size());
        assertEquals("ORD-20261015-001", response.getData().content().get(0).orderNumber());

        verify(orderService, times(1)).getBuyerOrders(
                eq(userId), eq(OrderStatus.CONFIRMED), eq("Apple"), eq(0), eq(10), eq("createdAt"), eq("desc")
        );
    }

    @Test
    @DisplayName("getBuyerOrders với tham số mặc định: kiểm tra truyền đúng giá trị")
    void getBuyerOrders_DefaultParameters() {
        PageResponse<BuyerOrderResponse> emptyPage = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0L, 0, true
        );

        when(orderService.getBuyerOrders(eq(userId), isNull(), isNull(), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(emptyPage);

        ApiResponse<PageResponse<BuyerOrderResponse>> response = buyerOrderController.getBuyerOrders(
                null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().content().isEmpty());

        verify(orderService, times(1)).getBuyerOrders(
                eq(userId), isNull(), isNull(), eq(0), eq(10), eq("createdAt"), eq("desc")
        );
    }

    @Test
    @DisplayName("getBuyerOrders ném UNAUTHENTICATED khi không có thông tin xác thực trong SecurityContext")
    void getBuyerOrders_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.getBuyerOrders(null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức getBuyerOrderDetail (@GetMapping, @PreAuthorize)")
    void getBuyerOrderDetail_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = BuyerOrderController.class.getMethod("getBuyerOrderDetail", UUID.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getBuyerOrderDetail phải có annotation @GetMapping");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, getMapping.value(),
                "Endpoint path phải là /{id}");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method getBuyerOrderDetail phải có annotation @PreAuthorize");
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Chỉ người dùng có quyền ROLE_BUYER mới được phép gọi API");
    }

    @Test
    @DisplayName("getBuyerOrderDetail thành công: gọi OrderService và trả về ApiResponse chứa BuyerOrderDetailResponse")
    void getBuyerOrderDetail_Success() {
        UUID orderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        com.techhub.model.dto.response.AddressSnapshotResponse address = new com.techhub.model.dto.response.AddressSnapshotResponse(
                UUID.randomUUID(), "Nguyễn Văn A", "0987654321", "123 Cầu Giấy", "Dịch Vọng", "Cầu Giấy", "Hà Nội", "123 Cầu Giấy, Dịch Vọng, Cầu Giấy, Hà Nội"
        );

        BuyerOrderItemResponse item = new BuyerOrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Củ sạc 20W", "Trắng", "SKU-20W",
                "http://img.png", new BigDecimal("500000.00"), 1,
                BigDecimal.ZERO, new BigDecimal("500000.00"), new BigDecimal("500000.00")
        );

        com.techhub.model.dto.response.BuyerOrderDetailResponse detailResponse = new com.techhub.model.dto.response.BuyerOrderDetailResponse(
                orderId, "ORD-20261015-001", OrderStatus.DELIVERED, "Đã giao hàng thành công",
                shopId, "Apple Official Store", "apple-official-store", "http://avatar.png",
                address, List.of(item), 1,
                new BigDecimal("500000.00"), new BigDecimal("30000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("530000.00"),
                "Giao giờ hành chính", PaymentMethod.COD, "Thanh toán khi nhận hàng (COD)",
                PaymentStatus.PAID, "Đã thanh toán tiền mặt cho bưu tá",
                LocalDateTime.now(), LocalDateTime.now(), null, null,
                Collections.emptyList(), List.of("COMPLETE_ORDER", "REQUEST_RETURN")
        );

        when(orderService.getBuyerOrderDetail(eq(userId), eq(orderId))).thenReturn(detailResponse);

        ApiResponse<com.techhub.model.dto.response.BuyerOrderDetailResponse> response = buyerOrderController.getBuyerOrderDetail(orderId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched buyer order details successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(orderId, response.getData().orderId());
        assertEquals("ORD-20261015-001", response.getData().orderNumber());
        assertEquals(OrderStatus.DELIVERED, response.getData().status());
        assertEquals("Apple Official Store", response.getData().shopName());
        assertEquals("Nguyễn Văn A", response.getData().deliveryAddress().recipientName());

        verify(orderService, times(1)).getBuyerOrderDetail(eq(userId), eq(orderId));
    }

    @Test
    @DisplayName("getBuyerOrderDetail ném UNAUTHENTICATED khi không có thông tin xác thực")
    void getBuyerOrderDetail_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();
        UUID orderId = UUID.randomUUID();

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.getBuyerOrderDetail(orderId)
        );

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("TC-CTRL-01: completeOrder thành công - Trả về 200 OK và status = COMPLETED")
    void tc01_completeOrder_Success() {
        UUID orderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        AddressSnapshotResponse address = new AddressSnapshotResponse(
                UUID.randomUUID(), "Lê Hải Đăng", "0987654321", "12 Quang Trung",
                "Mộ Lao", "Hà Đông", "Hà Nội", "12 Quang Trung, Mộ Lao, Hà Đông, Hà Nội"
        );

        BuyerOrderItemResponse item = new BuyerOrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Chuột TechPro X1", "Đen", "SKU-TPX1",
                "http://img.png", new BigDecimal("650000.00"), 1,
                new BigDecimal("50000.00"), new BigDecimal("600000.00"), new BigDecimal("600000.00")
        );

        BuyerOrderDetailResponse completedResponse = new BuyerOrderDetailResponse(
                orderId, "ORD-20261016-89AB12CD", OrderStatus.COMPLETED, "Hoàn thành đơn hàng",
                shopId, "Laptop Hà Đông Official", "laptop-hadong-official", "http://avatar.png",
                address, List.of(item), 1,
                new BigDecimal("650000.00"), new BigDecimal("30000.00"),
                new BigDecimal("30000.00"), new BigDecimal("20000.00"), new BigDecimal("630000.00"),
                "Giao giờ hành chính", PaymentMethod.COD, "Thanh toán khi nhận hàng (COD)",
                PaymentStatus.PAID, "Đã thanh toán tiền mặt cho bưu tá",
                LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(1), LocalDateTime.now(), null,
                Collections.emptyList(), List.of("REVIEW", "BUY_AGAIN")
        );

        when(orderService.completeOrder(eq(userId), eq(orderId))).thenReturn(completedResponse);

        ApiResponse<BuyerOrderDetailResponse> response = buyerOrderController.completeOrder(orderId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Order completed successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(orderId, response.getData().orderId());
        assertEquals(OrderStatus.COMPLETED, response.getData().status());
        assertTrue(response.getData().availableActions().contains("REVIEW"));
        assertTrue(response.getData().availableActions().contains("BUY_AGAIN"));

        verify(orderService, times(1)).completeOrder(eq(userId), eq(orderId));
    }

    @Test
    @DisplayName("TC-CTRL-02: completeOrder khi đơn hàng không tồn tại - Trả về ORDER_NOT_FOUND")
    void tc02_completeOrder_OrderNotFound_Returns404() {
        UUID orderId = UUID.randomUUID();
        when(orderService.completeOrder(eq(userId), eq(orderId)))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.completeOrder(orderId)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderService, times(1)).completeOrder(eq(userId), eq(orderId));
    }

    @Test
    @DisplayName("TC-CTRL-03: completeOrder khi trạng thái đơn không hợp lệ - Trả về INVALID_STATUS_TRANSITION")
    void tc03_completeOrder_InvalidStatus_Returns400() {
        UUID orderId = UUID.randomUUID();
        when(orderService.completeOrder(eq(userId), eq(orderId)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.completeOrder(orderId)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verify(orderService, times(1)).completeOrder(eq(userId), eq(orderId));
    }

    @Test
    @DisplayName("TC-CTRL-04: completeOrder kiểm tra cấu hình @PreAuthorize('hasRole(\\'BUYER\\')') và @PatchMapping")
    void tc04_completeOrder_AnnotationCheck() throws NoSuchMethodException {
        Method method = BuyerOrderController.class.getMethod("completeOrder", UUID.class);

        assertTrue(method.isAnnotationPresent(PatchMapping.class), "Endpoint completeOrder phải có @PatchMapping");
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertArrayEquals(new String[]{"/{id}/complete"}, patchMapping.value());

        assertTrue(method.isAnnotationPresent(PreAuthorize.class), "Endpoint completeOrder phải có @PreAuthorize");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-CTRL-01: requestOrderCancellation thành công - Trả về HTTP 201 Created và CancellationRequestResponse")
    void tc01_requestCancellation_Success_ReturnsCreated() {
        UUID orderId = UUID.randomUUID();
        CreateCancellationRequest request = new CreateCancellationRequest("Tôi muốn thay đổi địa chỉ nhận hàng");
        CancellationRequestResponse mockResponse = new CancellationRequestResponse(
                UUID.randomUUID(), orderId, "ORD-20261016-001",
                CancellationRequestStatus.PENDING, "Đang chờ Người bán phản hồi",
                request.reason(), null, LocalDateTime.now(), null
        );

        when(orderService.requestOrderCancellation(eq(userId), eq(orderId), eq(request)))
                .thenReturn(mockResponse);

        ApiResponse<CancellationRequestResponse> response = buyerOrderController.requestOrderCancellation(orderId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(orderId, response.getData().orderId());
        assertEquals(CancellationRequestStatus.PENDING, response.getData().status());
        assertEquals(request.reason(), response.getData().reason());
        verify(orderService, times(1)).requestOrderCancellation(eq(userId), eq(orderId), eq(request));
    }

    @Test
    @DisplayName("TC-CTRL-02: requestOrderCancellation khi reason rỗng hoặc vi phạm - Service ném INVALID_INPUT_DATA")
    void tc02_requestCancellation_BlankReason_ThrowsInvalidInputData() {
        UUID orderId = UUID.randomUUID();
        CreateCancellationRequest request = new CreateCancellationRequest("   ");

        when(orderService.requestOrderCancellation(eq(userId), eq(orderId), eq(request)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT_DATA));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.requestOrderCancellation(orderId, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, exception.getErrorCode());
        verify(orderService, times(1)).requestOrderCancellation(eq(userId), eq(orderId), eq(request));
    }

    @Test
    @DisplayName("TC-CTRL-03: requestOrderCancellation khi đơn đã có yêu cầu hủy - Ném CANCELLATION_REQUEST_ALREADY_EXISTS")
    void tc03_requestCancellation_AlreadyExists_ThrowsConflict() {
        UUID orderId = UUID.randomUUID();
        CreateCancellationRequest request = new CreateCancellationRequest("Đổi địa chỉ giao");

        when(orderService.requestOrderCancellation(eq(userId), eq(orderId), eq(request)))
                .thenThrow(new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                buyerOrderController.requestOrderCancellation(orderId, request)
        );

        assertEquals(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS, exception.getErrorCode());
        verify(orderService, times(1)).requestOrderCancellation(eq(userId), eq(orderId), eq(request));
    }

    @Test
    @DisplayName("TC-CTRL-04: requestOrderCancellation kiểm tra cấu hình @PostMapping, @ResponseStatus(CREATED) và @PreAuthorize")
    void tc04_requestCancellation_AnnotationCheck() throws NoSuchMethodException {
        Method method = BuyerOrderController.class.getMethod("requestOrderCancellation", UUID.class, CreateCancellationRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class), "Endpoint phải có @PostMapping");
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/{id}/cancellation-request"}, postMapping.value());

        assertTrue(method.isAnnotationPresent(ResponseStatus.class), "Endpoint phải có @ResponseStatus");
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
        assertEquals(HttpStatus.CREATED, responseStatus.value());

        assertTrue(method.isAnnotationPresent(PreAuthorize.class), "Endpoint phải có @PreAuthorize");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value());
    }

    // ==========================================
    // POST /api/buyer/orders/{id}/reorder (API 12)
    // ==========================================

    @Test
    @DisplayName("TC-16: controller_reorder_Success - Gọi service mua lại thành công trả về HTTP 200 OK")
    void controller_reorder_Success() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        ReorderResponse mockResponse = new ReorderResponse(
                orderId, 2, 2, List.of(), List.of(), 2, 3
        );

        when(orderService.reorder(eq(userId), eq(orderId))).thenReturn(mockResponse);

        // Act
        ApiResponse<ReorderResponse> response = buyerOrderController.reorder(orderId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(orderId, response.getData().orderId());
        assertEquals("Xử lý mua lại đơn hàng thành công", response.getMessage());
        verify(orderService, times(1)).reorder(eq(userId), eq(orderId));
    }

    @Test
    @DisplayName("TC-17: controller_reorder_HasPostMappingAnnotation - Xác nhận endpoint có @PostMapping(\"/{id}/reorder\")")
    void controller_reorder_HasPostMappingAnnotation() throws NoSuchMethodException {
        // Act
        Method method = BuyerOrderController.class.getMethod("reorder", UUID.class);

        // Assert
        assertTrue(method.isAnnotationPresent(PostMapping.class), "Endpoint phải có @PostMapping");
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/{id}/reorder"}, postMapping.value());
    }

    @Test
    @DisplayName("TC-18: controller_reorder_ProtectedByPreAuthorizeBuyer - Xác nhận phân quyền @PreAuthorize(\"hasRole('BUYER')\")")
    void controller_reorder_ProtectedByPreAuthorizeBuyer() throws NoSuchMethodException {
        // Act
        Method method = BuyerOrderController.class.getMethod("reorder", UUID.class);

        // Assert
        assertTrue(method.isAnnotationPresent(PreAuthorize.class), "Endpoint phải có @PreAuthorize");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value());
    }
}
