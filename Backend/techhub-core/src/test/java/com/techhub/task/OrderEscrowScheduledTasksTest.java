package com.techhub.task;

import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho OrderEscrowScheduledTasks (72h SLAs)")
class OrderEscrowScheduledTasksTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CancellationRequestRepository cancellationRequestRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundTransactionRepository refundTransactionRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private OrderEscrowScheduledTasks escrowTasks;

    private UUID orderId;
    private Order order;
    private Seller seller;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        seller = new Seller();
        seller.setId(UUID.randomUUID());
        seller.setBalance(BigDecimal.ZERO);

        Shop shop = new Shop();
        shop.setId(UUID.randomUUID());
        shop.setSeller(seller);

        order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-TEST-001");
        order.setStatus(OrderStatus.DELIVERED);
        order.setShop(shop);
        order.setTotalSellerEarnings(new BigDecimal("190000.00"));
        order.setTotalAmount(new BigDecimal("200000.00"));
    }

    @Test
    @DisplayName("autoCompleteDeliveredOrders - Settle thành công khi Double-Condition Update affected rows > 0")
    void autoCompleteDeliveredOrders_Success() {
        when(orderRepository.findByStatusAndDeliveredAtBefore(eq(OrderStatus.DELIVERED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(order));
        when(orderRepository.completeDeliveredOrderAtomic(eq(orderId), any(LocalDateTime.class))).thenReturn(1);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(sellerWalletTransactionRepository.existsByReferenceIdAndType(eq(orderId), eq(SellerWalletTransactionType.ORDER_SETTLEMENT)))
                .thenReturn(false);

        escrowTasks.autoCompleteDeliveredOrders();

        verify(sellerRepository).incrementBalance(eq(seller.getId()), eq(order.getTotalSellerEarnings()));
        verify(sellerWalletTransactionRepository).save(any(SellerWalletTransaction.class));
    }

    @Test
    @DisplayName("autoCompleteDeliveredOrders - Chống Race Condition: Bỏ qua khi affected rows == 0")
    void autoCompleteDeliveredOrders_RaceCondition_Skipped() {
        when(orderRepository.findByStatusAndDeliveredAtBefore(eq(OrderStatus.DELIVERED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(order));
        // Concurrent update changed status to RETURN_REQUESTED -> atomic update affects 0 rows
        when(orderRepository.completeDeliveredOrderAtomic(eq(orderId), any(LocalDateTime.class))).thenReturn(0);

        escrowTasks.autoCompleteDeliveredOrders();

        verify(sellerRepository, never()).incrementBalance(any(), any());
        verify(sellerWalletTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("autoApproveCancellationRequests - Auto approve sau 72h, hủy đơn và hoàn kho")
    void autoApproveCancellationRequests_Success() {
        UUID crId = UUID.randomUUID();
        order.setStatus(OrderStatus.CONFIRMED);

        CancellationRequest cr = CancellationRequest.builder()
                .id(crId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .build();

        when(cancellationRequestRepository.findByStatusAndCreatedAtBefore(eq(CancellationRequestStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(cr));
        when(cancellationRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        escrowTasks.autoApproveCancellationRequests();

        assertEquals(CancellationRequestStatus.AUTO_APPROVED, cr.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(cancellationRequestRepository).save(cr);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("autoApproveReturnRequests - Auto approve sau 72h, chuyển REFUNDED và ghi nhận hoàn tiền")
    void autoApproveReturnRequests_Success() {
        UUID rrId = UUID.randomUUID();
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PAID);
        payment.setRefundedAmount(BigDecimal.ZERO);
        order.setPayment(payment);

        ReturnRequest rr = ReturnRequest.builder()
                .id(rrId)
                .order(order)
                .status(ReturnRequestStatus.PENDING)
                .build();

        when(returnRequestRepository.findByStatusAndCreatedAtBefore(eq(ReturnRequestStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(rr));
        when(returnRequestRepository.findById(rrId)).thenReturn(Optional.of(rr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        escrowTasks.autoApproveReturnRequests();

        assertEquals(ReturnRequestStatus.AUTO_APPROVED, rr.getStatus());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        verify(returnRequestRepository).save(rr);
        verify(orderRepository).save(order);
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
    }
}
