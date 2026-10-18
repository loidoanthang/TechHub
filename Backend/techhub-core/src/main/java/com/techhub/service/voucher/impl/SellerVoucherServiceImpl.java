package com.techhub.service.voucher.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.model.dto.response.ShopVoucherDetailResponse;
import com.techhub.model.dto.response.ShopVoucherResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.SellerVoucherService;
import com.techhub.specification.VoucherSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerVoucherServiceImpl implements SellerVoucherService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final VoucherRepository voucherRepository;

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.ofEntries(
            Map.entry("createdat", "createdAt"),
            Map.entry("created_at", "createdAt"),
            Map.entry("startdate", "startDate"),
            Map.entry("start_date", "startDate"),
            Map.entry("enddate", "endDate"),
            Map.entry("end_date", "endDate"),
            Map.entry("discountvalue", "discountValue"),
            Map.entry("discount_value", "discountValue"),
            Map.entry("usedcount", "usedCount"),
            Map.entry("used_count", "usedCount"),
            Map.entry("minorderamount", "minOrderAmount"),
            Map.entry("min_order_amount", "minOrderAmount")
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVoucherResponse createShopVoucher(UUID userId, CreateShopVoucherRequest request) {
        log.info("Processing createShopVoucher for userId={}, voucherCode='{}'", userId, request.code());

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Shop status
        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Shop {} is PAUSED. Rejecting voucher creation.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            log.warn("Shop {} status {} is not active. Rejecting voucher creation.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Normalize inputs
        String normalizedCode = request.code().trim().toUpperCase();
        String trimmedName = request.name().trim();

        // 3. Validate Date Range
        if (!request.endDate().isAfter(request.startDate())) {
            log.warn("Voucher endDate must be after startDate. startDate={}, endDate={}", request.startDate(), request.endDate());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        if (request.endDate().isBefore(LocalDateTime.now())) {
            log.warn("Voucher endDate {} is in the past", request.endDate());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        LocalDateTime gracePeriodThreshold = LocalDateTime.now().minusMinutes(5);
        if (request.startDate().isBefore(gracePeriodThreshold)) {
            log.warn("Voucher startDate {} is in the past (before threshold {})", request.startDate(), gracePeriodThreshold);
            throw new BusinessException(ErrorCode.VOUCHER_START_DATE_IN_PAST);
        }

        // 4. Validate Discount Value & Cap
        BigDecimal maxDiscountAmount = request.maxDiscountAmount();
        if (request.discountType() == DiscountType.PERCENTAGE) {
            if (request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                log.warn("Percentage discount value cannot exceed 100%. Given: {}", request.discountValue());
                throw new BusinessException(ErrorCode.INVALID_DISCOUNT_VALUE);
            }
        } else if (request.discountType() == DiscountType.FIXED_AMOUNT) {
            maxDiscountAmount = null; // Cap is only applicable to percentage discounts
        }

        // 5. Validate Usage Limits
        if (request.usageLimit() != null && request.usageLimit() < request.usageLimitPerUser()) {
            log.warn("Usage limit {} cannot be less than usage limit per user {}", request.usageLimit(), request.usageLimitPerUser());
            throw new BusinessException(ErrorCode.INVALID_USAGE_LIMIT);
        }

        // 6. Check Duplicate Voucher Code
        if (voucherRepository.existsByCode(normalizedCode)) {
            log.warn("Voucher code '{}' already exists", normalizedCode);
            throw new BusinessException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
        }

        BigDecimal minOrderAmount = request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO;

        // 7. Build and persist Voucher entity
        Voucher voucher = Voucher.builder()
                .shop(shop)
                .type(VoucherType.SHOP)
                .appliesTo(request.appliesTo())
                .code(normalizedCode)
                .name(trimmedName)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .usageLimit(request.usageLimit())
                .usageLimitPerUser(request.usageLimitPerUser())
                .reservedCount(0)
                .usedCount(0)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isActive(true)
                .build();

        try {
            voucher = voucherRepository.saveAndFlush(voucher);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uk_vouchers_code") || msg.contains("code")) {
                log.warn("Duplicate voucher code constraint violation: {}", normalizedCode);
                throw new BusinessException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
            }
            log.error("Database integrity violation while saving voucher: {}", normalizedCode, e);
            throw e;
        }

        log.info("Successfully created shop voucher id={}, code='{}' for shopId={}", voucher.getId(), voucher.getCode(), shop.getId());

        return ShopVoucherResponse.from(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopVoucherResponse> getShopVouchers(
            UUID userId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Processing getShopVouchers for userId={}, keyword='{}', appliesTo={}, discountType={}, status={}, page={}, size={}, sortBy={}, sortDir={}",
                userId, keyword, appliesTo, discountType, status, page, size, sortBy, sortDir);

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Tiered Penalty Model (BR-07)
        // BANNED shops are completely blocked from accessing shop vouchers
        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} status is BANNED. Rejecting getShopVouchers.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: PAUSED and SUSPENDED shops are granted read-only access for audits/appeals

        // 2. Sanitize Pagination & Sort
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedSortKey = sortBy != null ? sortBy.trim().toLowerCase() : "";
        String sanitizedSortBy = SORT_FIELD_MAPPING.getOrDefault(normalizedSortKey, "createdAt");
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sanitizedSortBy).and(Sort.by(direction, "id"));
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        // 3. Query Database with Specification
        LocalDateTime now = LocalDateTime.now();
        Specification<Voucher> spec = VoucherSpecification.filterShopVouchers(
                shop.getId(), keyword, appliesTo, discountType, status, now
        );

        Page<Voucher> voucherPage = voucherRepository.findAll(spec, pageable);

        // 4. Map to DTOs using static factory method
        List<ShopVoucherResponse> content = voucherPage.getContent().stream()
                .map(v -> ShopVoucherResponse.from(v, now))
                .toList();

        return PageResponse.from(voucherPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopVoucherDetailResponse getShopVoucherDetail(UUID userId, UUID id) {
        log.info("Processing getShopVoucherDetail for userId={}, voucherId={}", userId, id);

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Tiered Penalty Model (BR-07)
        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} status is BANNED. Rejecting getShopVoucherDetail.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Fetch Voucher
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        // 3. IDOR and Scope Check
        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shop.getId()) || voucher.getType() != VoucherType.SHOP || voucher.isDeleted()) {
            log.warn("IDOR attempt, soft-deleted, or cross-shop voucher access: userId={}, shopId={}, voucherId={}", userId, shop.getId(), id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 4. Return DTO with operational metrics
        LocalDateTime now = LocalDateTime.now();
        return ShopVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVoucherDetailResponse updateShopVoucher(UUID userId, UUID id, UpdateShopVoucherRequest request) {
        log.info("Processing updateShopVoucher for userId={}, voucherId={}", userId, id);

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Shop status (BR-07)
        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Shop {} is PAUSED. Rejecting voucher update.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            log.warn("Shop {} status {} is not active. Rejecting voucher update.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Fetch Voucher & IDOR Check
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shop.getId()) || voucher.getType() != VoucherType.SHOP || voucher.isDeleted()) {
            log.warn("IDOR attempt, soft-deleted, or cross-shop voucher update: userId={}, shopId={}, voucherId={}", userId, shop.getId(), id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 3. Lifecycle State Check (BR-16)
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getEndDate().isBefore(now)) {
            log.warn("Voucher {} has already expired at {}. Rejecting update.", id, voucher.getEndDate());
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_EXPIRED);
        }

        // 4. Validate End Date
        if (!request.endDate().isAfter(now)) {
            log.warn("Voucher endDate {} must be in the future (after {})", request.endDate(), now);
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        // 5. Determine and Validate Start Date according to state (UPCOMING vs ONGOING)
        LocalDateTime effectiveStartDate;
        if (voucher.getStartDate().isAfter(now)) {
            // Voucher is UPCOMING: Seller can change startDate
            effectiveStartDate = request.startDate() != null ? request.startDate() : voucher.getStartDate();
            LocalDateTime gracePeriodThreshold = now.minusMinutes(5);
            if (effectiveStartDate.isBefore(gracePeriodThreshold)) {
                log.warn("Upcoming voucher startDate {} is in the past (before threshold {})", effectiveStartDate, gracePeriodThreshold);
                throw new BusinessException(ErrorCode.VOUCHER_START_DATE_IN_PAST);
            }
        } else {
            // Voucher is ONGOING: startDate is locked to original DB startDate
            effectiveStartDate = voucher.getStartDate();
        }

        if (!request.endDate().isAfter(effectiveStartDate)) {
            log.warn("Voucher endDate {} must be after startDate {}", request.endDate(), effectiveStartDate);
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        // 6. Validate Discount Value & Cap
        BigDecimal maxDiscountAmount = request.maxDiscountAmount();
        if (request.discountType() == DiscountType.PERCENTAGE) {
            if (request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                log.warn("Percentage discount value cannot exceed 100%. Given: {}", request.discountValue());
                throw new BusinessException(ErrorCode.INVALID_DISCOUNT_VALUE);
            }
        } else if (request.discountType() == DiscountType.FIXED_AMOUNT) {
            maxDiscountAmount = null; // Cap is only applicable to percentage discounts
        }

        // 7. Validate Usage Limits
        if (request.usageLimit() != null) {
            if (request.usageLimit() < request.usageLimitPerUser()) {
                log.warn("Usage limit {} cannot be less than usage limit per user {}", request.usageLimit(), request.usageLimitPerUser());
                throw new BusinessException(ErrorCode.INVALID_USAGE_LIMIT);
            }
            int currentUsedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
            if (request.usageLimit() < currentUsedCount) {
                log.warn("Usage limit {} cannot be less than currently used count {}", request.usageLimit(), currentUsedCount);
                throw new BusinessException(ErrorCode.USAGE_LIMIT_LESS_THAN_USED);
            }
        }

        // 8. Update Entity fields
        BigDecimal minOrderAmount = request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO;
        voucher.setName(request.name().trim());
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setMaxDiscountAmount(maxDiscountAmount);
        voucher.setUsageLimit(request.usageLimit());
        voucher.setUsageLimitPerUser(request.usageLimitPerUser());
        voucher.setStartDate(effectiveStartDate);
        voucher.setEndDate(request.endDate());

        voucher = voucherRepository.save(voucher);
        log.info("Successfully updated shop voucher id={}, code='{}' for shopId={}", voucher.getId(), voucher.getCode(), shop.getId());

        return ShopVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVoucherDetailResponse updateShopVoucherStatus(UUID userId, UUID id, UpdateShopVoucherStatusRequest request) {
        log.info("Processing updateShopVoucherStatus for userId={}, voucherId={}, isActive={}", userId, id, request.isActive());

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Shop status (BR-07)
        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Shop {} is PAUSED. Rejecting voucher status update.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            log.warn("Shop {} status {} is not active. Rejecting voucher status update.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Fetch Voucher & IDOR Check
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shop.getId()) || voucher.getType() != VoucherType.SHOP || voucher.isDeleted()) {
            log.warn("IDOR attempt, soft-deleted, or cross-shop voucher status update: userId={}, shopId={}, voucherId={}", userId, shop.getId(), id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 3. Lifecycle State Check (BR-16): Expired vouchers are immutable
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getEndDate().isBefore(now)) {
            log.warn("Voucher {} has already expired at {}. Rejecting status update.", id, voucher.getEndDate());
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_EXPIRED);
        }

        // 4. Update status
        if (request.isActive() != null) {
            voucher.setActive(request.isActive());
        }
        voucher = voucherRepository.save(voucher);
        log.info("Successfully updated shop voucher status id={}, code='{}', isActive={} for shopId={}",
                voucher.getId(), voucher.getCode(), voucher.isActive(), shop.getId());

        return ShopVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShopVoucher(UUID userId, UUID id) {
        log.info("Processing deleteShopVoucher for userId={}, voucherId={}", userId, id);

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Gatekeeper check: Shop status (BR-07)
        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Shop {} is PAUSED. Rejecting voucher deletion.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            log.warn("Shop {} status {} is not active. Rejecting voucher deletion.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Fetch Voucher & IDOR Check
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.getShop() == null || !voucher.getShop().getId().equals(shop.getId()) || voucher.getType() != VoucherType.SHOP || voucher.isDeleted()) {
            log.warn("IDOR attempt, soft-deleted, or cross-shop voucher deletion: userId={}, shopId={}, voucherId={}", userId, shop.getId(), id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 3. Active Reservation Invariant Check (BR-16B)
        int safeReservedCount = voucher.getReservedCount() != null ? voucher.getReservedCount() : 0;
        if (safeReservedCount > 0) {
            log.warn("Voucher {} has active reservations (reservedCount={}). Rejecting deletion.", id, safeReservedCount);
            throw new BusinessException(ErrorCode.VOUCHER_HAS_ACTIVE_RESERVATIONS);
        }

        // 4. Safe Deletion: Hard delete if unused (usedCount == 0), Soft delete if already used (usedCount > 0)
        int safeUsedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        if (safeUsedCount == 0) {
            voucherRepository.delete(voucher);
            log.info("Successfully hard-deleted unused shop voucher id={}, code='{}' for shopId={}", id, voucher.getCode(), shop.getId());
        } else {
            voucher.setDeleted(true);
            voucher.setActive(false);
            voucher.setDeletedAt(LocalDateTime.now());
            voucherRepository.save(voucher);
            log.info("Successfully soft-deleted used shop voucher id={}, code='{}' (usedCount={}) for shopId={}", id, voucher.getCode(), safeUsedCount, shop.getId());
        }
    }

    public static VoucherStatus computeStatus(Voucher voucher, LocalDateTime now) {
        return ShopVoucherResponse.computeStatus(voucher, now);
    }
}
