package com.techhub.service.order;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateReturnRequest;
import com.techhub.model.dto.request.ProcessReturnRequest;
import com.techhub.model.dto.response.ReturnRequestResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.impl.ReturnRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ReturnRequestService (Return & Refund)")
class ReturnRequestServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private RefundTransactionRepository refundTransactionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @InjectMocks
    private ReturnRequestServiceImpl returnRequestService;

    private UUID userId;
    private User buyer;
    private UUID shopId;
    private Shop shop;
    private Seller seller;
    private UUID orderId;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        buyer = new User();
        buyer.setId(userId);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setEmailVerified(true);

        seller = new Seller();
        seller.setId(UUID.randomUUID());
        seller.setVerificationStatus(SellerVerificationStatus.APPROVED);
        seller.setUser(buyer);
        seller.setBalance(BigDecimal.ZERO);

        shopId = UUID.randomUUID();
        shop = new Shop();
        shop.setId(shopId);
        shop.setName("Tech Store");
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setSeller(seller);

        orderId = UUID.randomUUID();
        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(new BigDecimal("200000.00"));

        order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-20261018-001");
        order.setUser(buyer);
        order.setShop(shop);
        order.setPayment(payment);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalAmount(new BigDecimal("200000.00"));
        order.setTotalSellerEarnings(new BigDecimal("180000.00"));
        order.setDeliveredAt(LocalDateTime.now().minusHours(24)); // Within 72h window
        order.setCreatedAt(LocalDateTime.now().minusDays(3));
        order.setUpdatedAt(LocalDateTime.now().minusHours(24));
    }

    @Test
    @DisplayName("createReturnRequest - Success: chuyển đơn sang RETURN_REQUESTED (Escrow Freeze)")
    void createReturnRequest_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));
        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(returnRequestRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest rr = invocation.getArgument(0);
            rr.setId(UUID.randomUUID());
            rr.setCreatedAt(LocalDateTime.now());
            return rr;
        });

        CreateReturnRequest request = new CreateReturnRequest(
                "Sản phẩm bị nứt vỡ", "https://img.techhub.vn/proof.jpg",
                "Vietcombank", "1234567890", "Nguyen Van A"
        );

        ReturnRequestResponse response = returnRequestService.createReturnRequest(userId, orderId, request);

        assertNotNull(response);
        assertEquals(ReturnRequestStatus.PENDING, response.status());
        assertEquals(OrderStatus.RETURN_REQUESTED, order.getStatus()); // Escrow Freeze
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("createReturnRequest - Throws ORDER_NOT_ELIGIBLE_FOR_RETURN khi đơn chưa DELIVERED")
    void createReturnRequest_Throws_WhenNotDelivered() {
        order.setStatus(OrderStatus.SHIPPING);
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));
        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        CreateReturnRequest request = new CreateReturnRequest(
                "Lỗi sản phẩm", null, "VCB", "123", "User"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                returnRequestService.createReturnRequest(userId, orderId, request)
        );
        assertEquals(ErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN, ex.getErrorCode());
    }

    @Test
    @DisplayName("createReturnRequest - Throws ORDER_NOT_ELIGIBLE_FOR_RETURN khi quá hạn 72h")
    void createReturnRequest_Throws_When72hExpired() {
        order.setDeliveredAt(LocalDateTime.now().minusHours(73)); // Expired
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));
        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        CreateReturnRequest request = new CreateReturnRequest(
                "Lỗi sản phẩm", null, "VCB", "123", "User"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                returnRequestService.createReturnRequest(userId, orderId, request)
        );
        assertEquals(ErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN, ex.getErrorCode());
    }

    @Test
    @DisplayName("cancelReturnRequest - Success: Buyer rút khiếu nại, unfreeze về DELIVERED nếu còn trong 72h")
    void cancelReturnRequest_Success_UnfreezeToDelivered() {
        UUID rrId = UUID.randomUUID();
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setDeliveredAt(LocalDateTime.now().minusHours(10)); // still within 72h

        ReturnRequest rr = ReturnRequest.builder()
                .id(rrId)
                .order(order)
                .user(buyer)
                .status(ReturnRequestStatus.PENDING)
                .reason("Gãy chân đế")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));
        when(returnRequestRepository.findByIdAndUserId(rrId, userId)).thenReturn(Optional.of(rr));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        ReturnRequestResponse response = returnRequestService.cancelReturnRequest(userId, rrId);

        assertNotNull(response);
        assertEquals(ReturnRequestStatus.CANCELLED, response.status());
        assertEquals(OrderStatus.DELIVERED, order.getStatus()); // Restored to DELIVERED
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("processReturnRequest - APPROVED: Chuyển đơn sang REFUNDED, ghi nhận RefundTransaction")
    void processReturnRequest_Approved() {
        UUID rrId = UUID.randomUUID();
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        ReturnRequest rr = ReturnRequest.builder()
                .id(rrId)
                .order(order)
                .user(buyer)
                .status(ReturnRequestStatus.PENDING)
                .reason("Màn hình sọc")
                .bankName("Techcombank")
                .bankAccountNumber("987654321")
                .bankAccountHolder("Nguyen Van B")
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(returnRequestRepository.findByIdAndShopId(rrId, shop.getId())).thenReturn(Optional.of(rr));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        ProcessReturnRequest request = new ProcessReturnRequest(ReturnRequestStatus.APPROVED, "Đồng ý nhận lại hàng");

        ReturnRequestResponse response = returnRequestService.processReturnRequest(userId, rrId, request);

        assertNotNull(response);
        assertEquals(ReturnRequestStatus.APPROVED, response.status());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
        verifyNoInteractions(sellerWalletTransactionRepository); // Seller balance NOT credited
    }

    @Test
    @DisplayName("processReturnRequest - REJECTED: Chuyển đơn sang COMPLETED, quyết toán tiền vào Ví Seller")
    void processReturnRequest_Rejected() {
        UUID rrId = UUID.randomUUID();
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        ReturnRequest rr = ReturnRequest.builder()
                .id(rrId)
                .order(order)
                .user(buyer)
                .status(ReturnRequestStatus.PENDING)
                .reason("Khách chê màu không đẹp")
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(returnRequestRepository.findByIdAndShopId(rrId, shop.getId())).thenReturn(Optional.of(rr));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        ProcessReturnRequest request = new ProcessReturnRequest(ReturnRequestStatus.REJECTED, "Hàng đúng mô tả, đã bóc tem không thể đổi");

        ReturnRequestResponse response = returnRequestService.processReturnRequest(userId, rrId, request);

        assertNotNull(response);
        assertEquals(ReturnRequestStatus.REJECTED, response.status());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(sellerRepository).incrementBalance(eq(seller.getId()), eq(order.getTotalSellerEarnings()));
        verify(sellerWalletTransactionRepository).save(any(SellerWalletTransaction.class));
    }
}
