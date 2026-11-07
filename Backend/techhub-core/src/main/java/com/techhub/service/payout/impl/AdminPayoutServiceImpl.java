package com.techhub.service.payout.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.payout.ProcessPayoutRequest;
import com.techhub.model.dto.response.payout.AdminPayoutResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PayoutProcessAction;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.repository.SellerPayoutRequestRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.SellerWalletTransactionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.payout.AdminPayoutService;
import jakarta.persistence.criteria.Predicate;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPayoutServiceImpl implements AdminPayoutService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "amount", "status");

    private final SellerPayoutRequestRepository sellerPayoutRequestRepository;
    private final ShopRepository shopRepository;
    private final SellerRepository sellerRepository;
    private final SellerWalletTransactionRepository sellerWalletTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPayoutResponse> getPayoutRequests(
            PayoutStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        log.info("Admin querying payout requests: status={}, keyword={}, startDate={}, endDate={}, page={}, size={}, sortBy={}, direction={}",
                status, keyword, startDate, endDate, page, size, sortBy, direction);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        String resolvedSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, resolvedSortBy).and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<SellerPayoutRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate sellerNamePred = cb.like(cb.lower(root.get("seller").get("sellerName")), pattern);
                Predicate bankAccPred = cb.like(cb.lower(root.get("bankAccountNumber")), pattern);
                Predicate holderPred = cb.like(cb.lower(root.get("bankAccountHolder")), pattern);
                predicates.add(cb.or(sellerNamePred, bankAccPred, holderPred));
            }

            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }

            if (endDate != null) {
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("createdAt"), endDateTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SellerPayoutRequest> pageResult = sellerPayoutRequestRepository.findAll(spec, pageable);

        Set<UUID> sellerIds = pageResult.getContent().stream()
                .map(p -> p.getSeller() != null ? p.getSeller().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> shopNameMap = Collections.emptyMap();
        if (!sellerIds.isEmpty()) {
            shopNameMap = shopRepository.findBySellerIdIn(sellerIds).stream()
                    .filter(shop -> shop.getSeller() != null)
                    .collect(Collectors.toMap(
                            shop -> shop.getSeller().getId(),
                            Shop::getName,
                            (existing, replacement) -> existing
                    ));
        }

        final Map<UUID, String> finalShopNameMap = shopNameMap;
        List<AdminPayoutResponse> content = pageResult.getContent().stream()
                .map(payout -> {
                    String shopName = null;
                    if (payout.getSeller() != null) {
                        shopName = finalShopNameMap.getOrDefault(payout.getSeller().getId(), payout.getSeller().getSellerName());
                    }
                    return AdminPayoutResponse.from(payout, shopName);
                })
                .toList();

        return PageResponse.from(pageResult, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminPayoutResponse processPayoutRequest(UUID id, UUID adminId, ProcessPayoutRequest request) {
        log.info("Admin {} processing payout request {}: action={}, reason={}",
                adminId, id, request.action(), request.rejectionReason());

        if (request.action() == PayoutProcessAction.REJECT) {
            if (request.rejectionReason() == null || request.rejectionReason().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PAYOUT_REJECTION_REASON_REQUIRED);
            }
        }

        SellerPayoutRequest payout = sellerPayoutRequestRepository.findForUpdateById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYOUT_REQUEST_NOT_FOUND));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            log.warn("Payout request {} is not pending (status={})", id, payout.getStatus());
            throw new BusinessException(ErrorCode.PAYOUT_ALREADY_PROCESSED);
        }

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (request.action() == PayoutProcessAction.APPROVE) {
            payout.setStatus(PayoutStatus.COMPLETED);
            payout.setProcessedBy(adminUser);
            payout.setProcessedAt(now);
            payout.setRejectionReason(null);
            sellerPayoutRequestRepository.save(payout);
            log.info("Payout request {} approved by admin {}", id, adminId);
        } else if (request.action() == PayoutProcessAction.REJECT) {
            if (sellerWalletTransactionRepository.existsByReferenceIdAndType(payout.getId(), SellerWalletTransactionType.WITHDRAWAL_REFUND)) {
                log.warn("Withdrawal refund transaction already exists for payout request {}", id);
                throw new BusinessException(ErrorCode.PAYOUT_ALREADY_PROCESSED);
            }

            Seller seller = sellerRepository.findWithUserForUpdateById(payout.getSeller().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

            BigDecimal balanceBefore = seller.getBalance();
            sellerRepository.incrementBalance(seller.getId(), payout.getAmount());
            BigDecimal balanceAfter = balanceBefore.add(payout.getAmount());
            seller.setBalance(balanceAfter);

            SellerWalletTransaction refundTx = SellerWalletTransaction.builder()
                    .seller(seller)
                    .type(SellerWalletTransactionType.WITHDRAWAL_REFUND)
                    .amount(payout.getAmount())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .referenceId(payout.getId())
                    .description("Hoàn tiền yêu cầu rút tiền bị từ chối: " + request.rejectionReason().trim())
                    .build();
            sellerWalletTransactionRepository.save(refundTx);

            payout.setStatus(PayoutStatus.REJECTED);
            payout.setProcessedBy(adminUser);
            payout.setProcessedAt(now);
            payout.setRejectionReason(request.rejectionReason().trim());
            sellerPayoutRequestRepository.save(payout);
            log.info("Payout request {} rejected by admin {}, amount {} refunded to seller {}",
                    id, adminId, payout.getAmount(), seller.getId());
        }

        String shopName = null;
        if (payout.getSeller() != null) {
            shopName = shopRepository.findBySellerId(payout.getSeller().getId())
                    .map(Shop::getName)
                    .orElse(payout.getSeller().getSellerName());
        }

        return AdminPayoutResponse.from(payout, shopName);
    }
}
