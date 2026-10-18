package com.techhub.service.order.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateReturnRequest;
import com.techhub.model.dto.request.ProcessReturnRequest;
import com.techhub.model.dto.response.ReturnRequestResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(UUID userId, UUID orderId, CreateReturnRequest request) {
        log.info("Processing createReturnRequest for userId={}, orderId={}", userId, orderId);

        User buyer = validateBuyerAccount(userId);
        if (orderId == null || request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 1. Order must be DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.warn("Cannot create return request for order {} in status {}", orderId, order.getStatus());
            throw new BusinessException(ErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN);
        }

        // 2. Must be within 72h window from deliveredAt
        LocalDateTime now = LocalDateTime.now();
        if (order.getDeliveredAt() == null || order.getDeliveredAt().plusHours(72).isBefore(now)) {
            log.warn("72-hour return window expired for order {}", orderId);
            throw new BusinessException(ErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN);
        }

        // 3. Check for existing active return request
        returnRequestRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() != ReturnRequestStatus.CANCELLED) {
                log.warn("Return request already exists for order {} with status {}", orderId, existing.getStatus());
                throw new BusinessException(ErrorCode.RETURN_REQUEST_ALREADY_EXISTS);
            }
        });

        // 4. Validate bank info for COD orders
        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentMethod() == PaymentMethod.COD) {
            if (request.bankName() == null || request.bankName().isBlank() ||
                request.bankAccountNumber() == null || request.bankAccountNumber().isBlank() ||
                request.bankAccountHolder() == null || request.bankAccountHolder().isBlank()) {
                log.warn("Bank details required for COD return request on order {}", orderId);
                throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
            }
        }

        // 5. Create & Save ReturnRequest
        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .user(buyer)
                .reason(request.reason().trim())
                .proofImages(request.proofImages())
                .status(ReturnRequestStatus.PENDING)
                .bankName(request.bankName())
                .bankAccountNumber(request.bankAccountNumber())
                .bankAccountHolder(request.bankAccountHolder())
                .build();
        returnRequest = returnRequestRepository.save(returnRequest);

        // 6. Escrow Freeze: Transition DELIVERED -> RETURN_REQUESTED
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        log.info("Return request {} created successfully for order {}", returnRequest.getId(), orderId);
        return mapToReturnRequestResponse(returnRequest);
    }

    @Override
    @Transactional
    public ReturnRequestResponse cancelReturnRequest(UUID userId, UUID returnRequestId) {
        log.info("Processing cancelReturnRequest for userId={}, returnRequestId={}", userId, returnRequestId);

        validateBuyerAccount(userId);
        if (returnRequestId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        ReturnRequest returnRequest = returnRequestRepository.findByIdAndUserId(returnRequestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (returnRequest.getStatus() != ReturnRequestStatus.PENDING) {
            log.warn("Return request {} is in status {}, cannot cancel", returnRequestId, returnRequest.getStatus());
            throw new BusinessException(ErrorCode.RETURN_REQUEST_CANNOT_BE_CANCELLED);
        }

        LocalDateTime now = LocalDateTime.now();
        returnRequest.setStatus(ReturnRequestStatus.CANCELLED);
        returnRequest.setRespondedAt(now);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Unfreeze Escrow: Order status RETURN_REQUESTED -> DELIVERED or COMPLETED
        Order order = orderRepository.findByIdForUpdate(returnRequest.getOrder().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.RETURN_REQUESTED) {
            if (order.getDeliveredAt() != null && order.getDeliveredAt().plusHours(72).isBefore(now)) {
                // 72h window already expired: transition directly to COMPLETED and settle
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedAt(now);
                settleOrderToSellerWallet(order, now);
            } else {
                // Still within 72h window: restore to DELIVERED
                order.setStatus(OrderStatus.DELIVERED);
            }
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }

        log.info("Return request {} successfully cancelled by buyer {}", returnRequestId, userId);
        return mapToReturnRequestResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getBuyerReturnRequest(UUID userId, UUID returnRequestId) {
        validateBuyerAccount(userId);
        ReturnRequest rr = returnRequestRepository.findByIdAndUserId(returnRequestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
        return mapToReturnRequestResponse(rr);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getBuyerReturnRequestByOrderId(UUID userId, UUID orderId) {
        validateBuyerAccount(userId);
        ReturnRequest rr = returnRequestRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
        return mapToReturnRequestResponse(rr);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> getSellerReturnRequests(UUID userId, ReturnRequestStatus status, int page, int size) {
        Shop shop = validateSellerAndShop(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReturnRequest> pageResult = (status != null)
                ? returnRequestRepository.findByShopIdAndStatus(shop.getId(), status, pageable)
                : returnRequestRepository.findByShopId(shop.getId(), pageable);

        return PageResponse.from(pageResult, pageResult.getContent().stream().map(this::mapToReturnRequestResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getSellerReturnRequestDetail(UUID userId, UUID returnRequestId) {
        Shop shop = validateSellerAndShop(userId);
        ReturnRequest rr = returnRequestRepository.findByIdAndShopId(returnRequestId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
        return mapToReturnRequestResponse(rr);
    }

    @Override
    @Transactional
    public ReturnRequestResponse processReturnRequest(UUID userId, UUID returnRequestId, ProcessReturnRequest request) {
        log.info("Processing processReturnRequest for userId={}, returnRequestId={}, decision={}",
                userId, returnRequestId, request != null ? request.decision() : null);

        Shop shop = validateSellerAndShop(userId);
        if (returnRequestId == null || request == null || request.decision() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        ReturnRequest returnRequest = returnRequestRepository.findByIdAndShopId(returnRequestId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (returnRequest.getStatus() != ReturnRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_ALREADY_PROCESSED);
        }

        Order order = orderRepository.findByIdForUpdate(returnRequest.getOrder().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (request.decision() == ReturnRequestStatus.APPROVED) {
            returnRequest.setStatus(ReturnRequestStatus.APPROVED);
            returnRequest.setSellerNote(request.sellerNote());
            returnRequest.setRespondedAt(now);
            returnRequest = returnRequestRepository.save(returnRequest);

            // Transition Order to REFUNDED
            order.setStatus(OrderStatus.REFUNDED);
            order.setUpdatedAt(now);
            orderRepository.save(order);

            // Record Refund Transaction
            processRefundTransaction(order, returnRequest, now);

            log.info("Return request {} APPROVED for order {}. Order status: REFUNDED", returnRequestId, order.getId());
        } else if (request.decision() == ReturnRequestStatus.REJECTED) {
            returnRequest.setStatus(ReturnRequestStatus.REJECTED);
            returnRequest.setSellerNote(request.sellerNote() != null ? request.sellerNote().trim() : "Seller rejected return request");
            returnRequest.setRespondedAt(now);
            returnRequest = returnRequestRepository.save(returnRequest);

            // Unfreeze Escrow: Order status -> COMPLETED & Settle to Seller Wallet
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(now);
            order.setUpdatedAt(now);
            orderRepository.save(order);

            settleOrderToSellerWallet(order, now);

            log.info("Return request {} REJECTED for order {}. Order status: COMPLETED (Settled to seller)", returnRequestId, order.getId());
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        return mapToReturnRequestResponse(returnRequest);
    }

    private void processRefundTransaction(Order order, ReturnRequest returnRequest, LocalDateTime now) {
        Payment payment = order.getPayment();
        if (payment == null) return;

        BigDecimal refundAmt = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        if (payment.getPaymentMethod() == PaymentMethod.COD) {
            RefundTransaction refundTx = RefundTransaction.builder()
                    .payment(payment)
                    .order(order)
                    .returnRequest(returnRequest)
                    .amount(refundAmt)
                    .refundMethod(RefundMethod.BANK_TRANSFER_MANUAL)
                    .status(RefundStatus.PENDING)
                    .targetBankName(returnRequest.getBankName())
                    .targetBankAccount(returnRequest.getBankAccountNumber())
                    .targetBankHolder(returnRequest.getBankAccountHolder())
                    .createdAt(now)
                    .build();
            refundTransactionRepository.save(refundTx);
            log.info("Recorded BANK_TRANSFER_MANUAL refund transaction for COD order {}: amount={}", order.getId(), refundAmt);
        } else {
            BigDecimal currentRefunded = Objects.requireNonNullElse(payment.getRefundedAmount(), BigDecimal.ZERO);
            payment.setRefundedAmount(currentRefunded.add(refundAmt));
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);

            RefundTransaction refundTx = RefundTransaction.builder()
                    .payment(payment)
                    .order(order)
                    .returnRequest(returnRequest)
                    .amount(refundAmt)
                    .refundMethod(RefundMethod.GATEWAY_REFUND)
                    .status(RefundStatus.SUCCESS)
                    .gatewayRefundId("MOCK_REFUND_" + UUID.randomUUID().toString().substring(0, 8))
                    .createdAt(now)
                    .build();
            refundTransactionRepository.save(refundTx);
            log.info("Recorded GATEWAY_REFUND transaction for Online order {}: amount={}", order.getId(), refundAmt);
        }
    }

    private void settleOrderToSellerWallet(Order order, LocalDateTime now) {
        if (order.getShop() == null || order.getShop().getSeller() == null) {
            return;
        }
        Seller seller = order.getShop().getSeller();
        BigDecimal earnings = order.getTotalSellerEarnings() != null ? order.getTotalSellerEarnings() : BigDecimal.ZERO;
        if (earnings.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (sellerWalletTransactionRepository.existsByReferenceIdAndType(order.getId(), SellerWalletTransactionType.ORDER_SETTLEMENT)) {
            log.info("Order {} already settled to seller wallet. Skipping duplicate credit.", order.getId());
            return;
        }

        BigDecimal balanceBefore = seller.getBalance() != null ? seller.getBalance() : BigDecimal.ZERO;
        sellerRepository.incrementBalance(seller.getId(), earnings);
        BigDecimal balanceAfter = balanceBefore.add(earnings);

        SellerWalletTransaction walletTx = SellerWalletTransaction.builder()
                .seller(seller)
                .type(SellerWalletTransactionType.ORDER_SETTLEMENT)
                .amount(earnings)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(order.getId())
                .description("Quyết toán doanh thu đơn hàng #" + order.getOrderNumber())
                .createdAt(now)
                .build();
        sellerWalletTransactionRepository.save(walletTx);
        log.info("Settled order {} earnings (+{}) to seller {}", order.getId(), earnings, seller.getId());
    }

    private User validateBuyerAccount(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return user;
    }

    private Shop validateSellerAndShop(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        User user = seller.getUser();
        if (user != null) {
            if (user.getStatus() == UserStatus.BANNED) {
                throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
            }
            if (user.getStatus() == UserStatus.DELETED) {
                throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
            }
            if (!user.isEmailVerified()) {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
            }
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        return shop;
    }

    private ReturnRequestResponse mapToReturnRequestResponse(ReturnRequest rr) {
        Order order = rr.getOrder();
        UUID orderId = order != null ? order.getId() : null;
        String orderNumber = order != null ? order.getOrderNumber() : "";
        UUID shopId = (order != null && order.getShop() != null) ? order.getShop().getId() : null;
        String shopName = (order != null && order.getShop() != null) ? order.getShop().getName() : "";
        BigDecimal refundAmount = order != null ? order.getTotalAmount() : BigDecimal.ZERO;

        String statusDescription = switch (rr.getStatus()) {
            case PENDING -> "Đang chờ Người bán xử lý";
            case APPROVED -> "Người bán đã chấp nhận trả hàng & hoàn tiền";
            case REJECTED -> "Người bán đã từ chối yêu cầu trả hàng";
            case CANCELLED -> "Người mua đã rút lại yêu cầu trả hàng";
            case AUTO_APPROVED -> "Hệ thống tự động chấp thuận hoàn tiền sau 72h";
        };

        return new ReturnRequestResponse(
                rr.getId(),
                orderId,
                orderNumber,
                shopId,
                shopName,
                rr.getStatus(),
                statusDescription,
                rr.getReason(),
                rr.getProofImages(),
                refundAmount,
                rr.getSellerNote(),
                rr.getBankName(),
                rr.getBankAccountNumber(),
                rr.getBankAccountHolder(),
                rr.getCreatedAt(),
                rr.getRespondedAt()
        );
    }
}
