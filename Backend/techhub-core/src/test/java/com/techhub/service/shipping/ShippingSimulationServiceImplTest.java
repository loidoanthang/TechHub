package com.techhub.service.shipping;

import com.techhub.model.dto.response.ShippingSimulationResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.Payment;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.service.shipping.impl.ShippingSimulationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShippingSimulationServiceImpl - Tự động hóa Giao vận & Quyết toán COD")
class ShippingSimulationServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ShippingSimulationServiceImpl shippingSimulationService;

    private UUID paymentId;
    private Payment mockCodPayment;
    private Payment mockOnlinePayment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();

        mockCodPayment = Payment.builder()
                .id(paymentId)
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(500000))
                .build();

        mockOnlinePayment = Payment.builder()
                .id(UUID.randomUUID())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PAID)
                .amount(BigDecimal.valueOf(1000000))
                .build();
    }

    private Order createMockOrder(UUID id, OrderStatus status, Payment payment) {
        return Order.builder()
                .id(id)
                .orderNumber("ORD-" + id.toString().substring(0, 8))
                .status(status)
                .payment(payment)
                .deliveryAddressSnapshot("{}")
                .totalProductAmount(BigDecimal.valueOf(500000))
                .shippingFee(BigDecimal.valueOf(30000))
                .shopDiscountAmount(BigDecimal.ZERO)
                .platformDiscountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(530000))
                .createdAt(LocalDateTime.now().minusHours(3))
                .updatedAt(LocalDateTime.now().minusHours(3))
                .build();
    }

    @Test
    @DisplayName("TC-01: Chuyển chặng thành công WAITING_FOR_PICKUP -> SHIPPING")
    void tc01_advanceShippingLifecycle_WaitingToShipping_Success() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, OrderStatus.WAITING_FOR_PICKUP, mockCodPayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(List.of(order));
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(1, response.processedWaitingOrders());
        assertEquals(0, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());

        verify(orderRepository, times(1)).saveAll(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-02: Chuyển chặng SHIPPING -> DELIVERED cho đơn COD lẻ 1 Shop -> Payment chuyển sang PAID")
    void tc02_advanceShippingLifecycle_ShippingToDelivered_AndCodSingleOrder_Paid() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, OrderStatus.SHIPPING, mockCodPayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order));

        when(orderRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(order)); // Chỉ có 1 đơn lẻ

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(0, response.processedWaitingOrders());
        assertEquals(1, response.processedShippingOrders());
        assertEquals(1, response.updatedCodPayments());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(PaymentStatus.PAID, mockCodPayment.getStatus(), "Đơn COD lẻ giao thành công -> Payment phải chuyển thành PAID");

        verify(orderRepository, times(1)).save(order);
        verify(paymentRepository, times(1)).save(mockCodPayment);
    }

    @Test
    @DisplayName("TC-03: Đơn hàng Đa Shop COD - 1 đơn DELIVERED, 1 đơn còn SHIPPING -> Payment giữ PENDING")
    void tc03_advanceShippingLifecycle_ShippingToDelivered_AndCodMultiShop_NotAllDelivered_RemainsPending() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        Order order1 = createMockOrder(orderId1, OrderStatus.SHIPPING, mockCodPayment);
        Order order2 = createMockOrder(orderId2, OrderStatus.SHIPPING, mockCodPayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        // Giả sử batch này chỉ quét và chuyển order1 trước
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order1));

        // Sibling orders trong DB: order1 vừa DELIVERED, nhưng order2 vẫn đang SHIPPING
        Order order2StillShipping = createMockOrder(orderId2, OrderStatus.SHIPPING, mockCodPayment);
        when(orderRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(order1, order2StillShipping));

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(1, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments(), "Chưa được tính là PAID vì còn đơn 2 chưa giao xong");
        assertEquals(PaymentStatus.PENDING, mockCodPayment.getStatus(), "Payment phải giữ nguyên PENDING");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Đơn hàng Đa Shop COD - Tất cả đơn con đều DELIVERED -> Payment chuyển sang PAID")
    void tc04_advanceShippingLifecycle_ShippingToDelivered_AndCodMultiShop_AllDelivered_Paid() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        Order order1 = createMockOrder(orderId1, OrderStatus.SHIPPING, mockCodPayment);
        Order order2 = createMockOrder(orderId2, OrderStatus.SHIPPING, mockCodPayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order1, order2));

        when(orderRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(order1, order2));

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(2, response.processedShippingOrders());
        assertEquals(1, response.updatedCodPayments(), "Cả 2 đơn con cùng paymentId đều xong -> Đổi Payment 1 lần thành PAID");
        assertEquals(PaymentStatus.PAID, mockCodPayment.getStatus());

        verify(paymentRepository, times(1)).save(mockCodPayment);
    }

    @Test
    @DisplayName("TC-05: Đơn hàng Đa Shop COD - 1 đơn DELIVERED, 1 đơn CANCELLED -> Payment chuyển sang PAID")
    void tc05_advanceShippingLifecycle_ShippingToDelivered_AndCodMultiShop_DeliveredAndCancelled_Paid() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        Order order1 = createMockOrder(orderId1, OrderStatus.SHIPPING, mockCodPayment);
        Order order2Cancelled = createMockOrder(orderId2, OrderStatus.CANCELLED, mockCodPayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order1));

        when(orderRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(order1, order2Cancelled));

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(1, response.processedShippingOrders());
        assertEquals(1, response.updatedCodPayments());
        assertEquals(PaymentStatus.PAID, mockCodPayment.getStatus(), "Đơn còn lại bị hủy thì tiền COD đơn giao thành công vẫn chuyển PAID");

        verify(paymentRepository, times(1)).save(mockCodPayment);
    }

    @Test
    @DisplayName("TC-06: Đơn hàng Online VNPAY giao thành công -> Payment giữ nguyên PAID, không cập nhật thừa")
    void tc06_advanceShippingLifecycle_ShippingToDelivered_OnlineVNPay_NoPaymentStatusChange() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, OrderStatus.SHIPPING, mockOnlinePayment);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order));

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(1, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments(), "Online VNPAY không tính vào updatedCodPayments");
        assertEquals(PaymentStatus.PAID, mockOnlinePayment.getStatus());

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-07: Chống chạy chồng đè (Re-entrant Guard) khi có tiến trình khác đang thực thi")
    void tc07_advanceShippingLifecycle_ReentrantGuard_ReturnsAlreadyInProgress() throws Exception {
        // Giả lập isRunning = true bằng Reflection
        Field isRunningField = ShippingSimulationServiceImpl.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        AtomicBoolean isRunning = (AtomicBoolean) isRunningField.get(shippingSimulationService);
        isRunning.set(true);

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(0, response.processedWaitingOrders());
        assertEquals(0, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments());
        assertEquals("Simulation is already in progress, please wait.", response.message());

        verify(orderRepository, never()).findByStatusOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("TC-08: Không có đơn hàng nào thỏa điều kiện -> Trả về 0 đơn")
    void tc08_advanceShippingLifecycle_EmptyOrders_ReturnsZero() {
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(0, response.processedWaitingOrders());
        assertEquals(0, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments());
        assertTrue(response.message().contains("Advanced 0 orders to SHIPPING, 0 orders to DELIVERED"));
    }

    @Test
    @DisplayName("TC-09: Đơn hàng bị khuyết Payment (Dirty Data) -> Xử lý an toàn không sập NPE")
    void tc09_advanceShippingLifecycle_OrderWithNullPayment_HandledSafely() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, OrderStatus.SHIPPING, null); // Payment = null

        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.WAITING_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStatusOrderByCreatedAtAsc(eq(OrderStatus.SHIPPING), any(Pageable.class)))
                .thenReturn(List.of(order));

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(true, 50);

        assertNotNull(response);
        assertEquals(1, response.processedShippingOrders());
        assertEquals(0, response.updatedCodPayments());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());

        verify(orderRepository, times(1)).save(order);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10: Giới hạn kích thước mẻ (Batch Size Clamping) tối đa 200")
    void tc10_advanceShippingLifecycle_BatchSizeClamping_Max200() {
        when(orderRepository.findByStatusOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());

        shippingSimulationService.advanceShippingLifecycle(true, 9999);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository, atLeastOnce()).findByStatusOrderByCreatedAtAsc(any(), pageableCaptor.capture());

        assertEquals(200, pageableCaptor.getValue().getPageSize(), "Batch size quá lớn phải clamp về 200");
    }
}
