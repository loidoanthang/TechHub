package com.techhub.service.seller.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.payout.WithdrawalRequest;
import com.techhub.model.dto.response.payout.PayoutResponse;
import com.techhub.model.dto.response.wallet.SellerWalletTransactionResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.SellerPayoutRequestRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.SellerWalletTransactionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.seller.SellerWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerWalletServiceImpl implements SellerWalletService {

    private static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("100000000.00");

    private final SellerRepository sellerRepository;
    private final SellerPayoutRequestRepository sellerPayoutRequestRepository;
    private final SellerWalletTransactionRepository sellerWalletTransactionRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayoutResponse requestWithdrawal(UUID currentUserId, WithdrawalRequest request) {
        log.info("Processing withdrawal request for user: {}, amount: {}", currentUserId, request.amount());

        // 1. Lock Seller entity with User immediately under PESSIMISTIC_WRITE lock.
        // This serializes all concurrent operations for this seller and prevents race conditions on both daily limit and balance.
        Seller lockedSeller = sellerRepository.findWithUserForUpdateByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        // 2. Validate Verification & Account Status
        if (lockedSeller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Withdrawal denied: seller {} verification status is {}", lockedSeller.getId(), lockedSeller.getVerificationStatus());
            throw new BusinessException(ErrorCode.SELLER_NOT_VERIFIED);
        }

        if (lockedSeller.getUser() != null && lockedSeller.getUser().getStatus() != UserStatus.ACTIVE) {
            log.warn("Withdrawal denied: user {} status is {}", lockedSeller.getUser().getId(), lockedSeller.getUser().getStatus());
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        // 3. Validate Shop Status
        Optional<Shop> shopOpt = shopRepository.findBySellerId(lockedSeller.getId());
        if (shopOpt.isPresent() && shopOpt.get().getStatus() == ShopStatus.BANNED) {
            log.warn("Withdrawal denied: shop {} is BANNED", shopOpt.get().getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 4. Validate Bank Information
        if (isBlank(lockedSeller.getBankName()) || isBlank(lockedSeller.getBankAccountNumber()) || isBlank(lockedSeller.getBankAccountHolder())) {
            log.warn("Withdrawal denied: seller {} bank account details incomplete", lockedSeller.getId());
            throw new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_CONFIGURED);
        }

        // 5. Calculate Daily Withdrawn Amount while holding the lock (closes concurrency window)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = LocalDate.now().plusDays(1).atStartOfDay();
        BigDecimal todayWithdrawn = sellerPayoutRequestRepository.sumDailyWithdrawalAmount(
                lockedSeller.getId(),
                startOfDay,
                startOfNextDay,
                List.of(PayoutStatus.PENDING, PayoutStatus.COMPLETED)
        );

        if (todayWithdrawn == null) {
            todayWithdrawn = BigDecimal.ZERO;
        }

        BigDecimal cumulativeAfter = todayWithdrawn.add(request.amount());
        if (cumulativeAfter.compareTo(DAILY_WITHDRAWAL_LIMIT) > 0) {
            log.warn("Withdrawal denied: daily limit exceeded for seller {}. Current today: {}, Requested: {}, Limit: {}",
                    lockedSeller.getId(), todayWithdrawn, request.amount(), DAILY_WITHDRAWAL_LIMIT);
            throw new BusinessException(ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED);
        }

        // 6. Check Balance
        if (lockedSeller.getBalance().compareTo(request.amount()) < 0) {
            log.warn("Withdrawal denied: insufficient balance for seller {}. Balance: {}, Requested: {}",
                    lockedSeller.getId(), lockedSeller.getBalance(), request.amount());
            throw new BusinessException(ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        // 7. Atomic Balance Deduction
        BigDecimal balanceBefore = lockedSeller.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(request.amount());
        lockedSeller.setBalance(balanceAfter);
        sellerRepository.save(lockedSeller);

        // 8. Snapshot Bank Information in Payout Request
        SellerPayoutRequest payoutRequest = SellerPayoutRequest.builder()
                .seller(lockedSeller)
                .amount(request.amount())
                .bankName(lockedSeller.getBankName())
                .bankAccountNumber(lockedSeller.getBankAccountNumber())
                .bankAccountHolder(lockedSeller.getBankAccountHolder())
                .status(PayoutStatus.PENDING)
                .build();
        payoutRequest = sellerPayoutRequestRepository.save(payoutRequest);

        // 9. Save Ledger Transaction
        SellerWalletTransaction walletTransaction = SellerWalletTransaction.builder()
                .seller(lockedSeller)
                .type(SellerWalletTransactionType.WITHDRAWAL)
                .amount(request.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(payoutRequest.getId())
                .description("Payout request withdrawal deduction")
                .build();
        sellerWalletTransactionRepository.save(walletTransaction);

        String shopName = shopOpt.map(Shop::getName).orElse(lockedSeller.getSellerName());
        log.info("Withdrawal request created successfully: payoutId={}, sellerId={}, remainingBalance={}",
                payoutRequest.getId(), lockedSeller.getId(), balanceAfter);

        return PayoutResponse.from(payoutRequest, balanceAfter, shopName);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SellerWalletTransactionResponse> getWalletTransactions(
            UUID currentUserId,
            SellerWalletTransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String direction
    ) {
        log.info("Fetching wallet transactions for user: {}, type: {}, startDate: {}, endDate: {}, page: {}, size: {}, direction: {}",
                currentUserId, type, startDate, endDate, page, size, direction);

        Seller seller = sellerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        Specification<SellerWalletTransaction> spec = (root, query, cb) ->
                cb.equal(root.get("seller").get("id"), seller.getId());

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
        }

        if (endDate != null) {
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), endDateTime));
        }

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));

        Page<SellerWalletTransaction> pageResult = sellerWalletTransactionRepository.findAll(spec, pageable);

        List<SellerWalletTransactionResponse> content = pageResult.getContent().stream()
                .map(SellerWalletTransactionResponse::from)
                .toList();

        return PageResponse.from(pageResult, content);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
