package com.techhub.service.voucher.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherStatusRequest;
import com.techhub.model.dto.response.AdminVoucherDetailResponse;
import com.techhub.model.dto.response.AdminVoucherResponse;
import com.techhub.model.dto.response.PlatformVoucherResponse;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.AdminVoucherService;
import com.techhub.specification.VoucherSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVoucherServiceImpl implements AdminVoucherService {

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
            Map.entry("min_order_amount", "minOrderAmount"),
            Map.entry("code", "code"),
            Map.entry("name", "name")
    );

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformVoucherResponse createPlatformVoucher(CreatePlatformVoucherRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS/SYSTEM";
        log.info("Processing createPlatformVoucher by operator='{}' with code='{}'", operator, request.code());

        // 1. Normalize inputs
        String normalizedCode = request.code().trim().toUpperCase();
        String trimmedName = request.name().trim();

        // 2. Validate Date Range
        LocalDateTime now = LocalDateTime.now();
        if (!request.endDate().isAfter(request.startDate())) {
            log.warn("Voucher endDate must be after startDate. startDate={}, endDate={}", request.startDate(), request.endDate());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        if (request.endDate().isBefore(now)) {
            log.warn("Voucher endDate {} is in the past", request.endDate());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        LocalDateTime gracePeriodThreshold = now.minusMinutes(5);
        if (request.startDate().isBefore(gracePeriodThreshold)) {
            log.warn("Voucher startDate {} is in the past (before threshold {})", request.startDate(), gracePeriodThreshold);
            throw new BusinessException(ErrorCode.VOUCHER_START_DATE_IN_PAST);
        }

        // 3. Validate Discount Value & Cap
        BigDecimal maxDiscountAmount = request.maxDiscountAmount();
        if (request.discountType() == DiscountType.PERCENTAGE) {
            if (request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                log.warn("Percentage discount value cannot exceed 100%. Given: {}", request.discountValue());
                throw new BusinessException(ErrorCode.INVALID_DISCOUNT_VALUE);
            }
        } else if (request.discountType() == DiscountType.FIXED_AMOUNT) {
            maxDiscountAmount = null; // Cap is only applicable to percentage discounts
        }

        // 4. Validate Usage Limits
        if (request.usageLimit() != null && request.usageLimit() < request.usageLimitPerUser()) {
            log.warn("Usage limit {} cannot be less than usage limit per user {}", request.usageLimit(), request.usageLimitPerUser());
            throw new BusinessException(ErrorCode.INVALID_USAGE_LIMIT);
        }

        // 5. Check Duplicate Voucher Code
        if (voucherRepository.existsByCode(normalizedCode)) {
            log.warn("Voucher code '{}' already exists", normalizedCode);
            throw new BusinessException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
        }

        BigDecimal minOrderAmount = request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO;
        if (request.discountType() == DiscountType.FIXED_AMOUNT && minOrderAmount.compareTo(BigDecimal.ZERO) > 0
                && request.discountValue().compareTo(minOrderAmount) > 0) {
            log.warn("Platform voucher code='{}' has FIXED_AMOUNT discountValue {} exceeding minOrderAmount {}. This may lead to 100% subsidy (0-dong orders).",
                    normalizedCode, request.discountValue(), minOrderAmount);
        }

        // 6. Build and persist Voucher entity
        Voucher voucher = Voucher.builder()
                .shop(null)
                .type(VoucherType.PLATFORM)
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
                .isDeleted(false)
                .build();

        try {
            voucher = voucherRepository.saveAndFlush(voucher);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uk_vouchers_code") || msg.contains("code")) {
                log.warn("Duplicate voucher code constraint violation: {}", normalizedCode);
                throw new BusinessException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
            }
            log.error("Database integrity violation while saving platform voucher: {}", normalizedCode, e);
            throw e;
        }

        log.info("Successfully created platform voucher id={}, code='{}' by operator='{}'", voucher.getId(), voucher.getCode(), operator);
        return PlatformVoucherResponse.from(voucher, now);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminVoucherResponse> getVouchers(
            VoucherType type,
            UUID shopId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Processing getVouchers by admin: type={}, shopId={}, keyword='{}', appliesTo={}, discountType={}, status={}, page={}, size={}, sortBy={}, sortDir={}",
                type, shopId, keyword, appliesTo, discountType, status, page, size, sortBy, sortDir);

        // 1. Sanitize Pagination & Sort
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedSortKey = sortBy != null ? sortBy.trim().toLowerCase() : "";
        String sanitizedSortBy = SORT_FIELD_MAPPING.getOrDefault(normalizedSortKey, "createdAt");
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sanitizedSortBy).and(Sort.by(direction, "id"));
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        // 2. Query Database with Specification
        LocalDateTime now = LocalDateTime.now();
        Specification<Voucher> spec = VoucherSpecification.filterAdminVouchers(
                type, shopId, keyword, appliesTo, discountType, status, now
        );

        Page<Voucher> voucherPage = voucherRepository.findAll(spec, pageable);

        // 3. Map to DTOs using static factory method (Shop fetched via @EntityGraph)
        List<AdminVoucherResponse> content = voucherPage.getContent().stream()
                .map(v -> AdminVoucherResponse.from(v, now))
                .toList();

        return PageResponse.from(voucherPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVoucherDetailResponse getVoucherDetail(UUID id) {
        log.info("Processing getVoucherDetail by admin for voucherId={}", id);

        if (id == null) {
            log.warn("Attempt to fetch voucher detail with null id");
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        Voucher voucher = voucherRepository.findWithShopById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.isDeleted()) {
            log.warn("Attempt to access soft-deleted voucher id={}", id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        return AdminVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminVoucherDetailResponse updatePlatformVoucher(UUID id, UpdatePlatformVoucherRequest request) {
        log.info("Processing updatePlatformVoucher for voucherId={}", id);

        if (id == null) {
            log.warn("Attempt to update voucher with null id");
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        if (request == null) {
            log.warn("Attempt to update voucher with null request");
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        Voucher voucher = voucherRepository.findWithShopById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.isDeleted()) {
            log.warn("Attempt to update soft-deleted voucher id={}", id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        if (voucher.getType() != VoucherType.PLATFORM) {
            log.warn("Attempt to update non-platform voucher id={} of type={}", id, voucher.getType());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_TYPE);
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if voucher has already expired
        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            log.warn("Voucher id={} has already expired at {}. Rejecting update.", id, voucher.getEndDate());
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_EXPIRED);
        }

        // Validate percentage discount value
        if (request.discountType() == DiscountType.PERCENTAGE && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            log.warn("Percentage discount value cannot exceed 100%. Given: {}", request.discountValue());
            throw new BusinessException(ErrorCode.INVALID_DISCOUNT_VALUE);
        }

        // Determine effective max discount amount (null for FIXED_AMOUNT)
        BigDecimal effectiveMaxDiscount = (request.discountType() == DiscountType.FIXED_AMOUNT)
                ? null
                : request.maxDiscountAmount();

        // Safe counts
        int safeUsedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        int safeReservedCount = voucher.getReservedCount() != null ? voucher.getReservedCount() : 0;
        int currentCommittedCount = safeUsedCount + safeReservedCount;

        // If usedCount > 0, core discount fields CANNOT be modified
        if (safeUsedCount > 0) {
            boolean isDiscountTypeChanged = voucher.getDiscountType() != request.discountType();
            boolean isDiscountValueChanged = voucher.getDiscountValue().compareTo(request.discountValue()) != 0;
            BigDecimal currentMinOrder = voucher.getMinOrderAmount() != null ? voucher.getMinOrderAmount() : BigDecimal.ZERO;
            BigDecimal reqMinOrder = request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO;
            boolean isMinOrderChanged = currentMinOrder.compareTo(reqMinOrder) != 0;
            boolean isMaxDiscountChanged = isBigDecimalDifferent(voucher.getMaxDiscountAmount(), effectiveMaxDiscount);

            if (isDiscountTypeChanged || isDiscountValueChanged || isMinOrderChanged || isMaxDiscountChanged) {
                log.warn("Voucher id={} has usedCount={}. Core discount modifications are rejected.", id, safeUsedCount);
                throw new BusinessException(ErrorCode.CANNOT_MODIFY_CORE_DISCOUNT);
            }
        }

        // Validate usageLimit (guard against over-committing active reservations)
        if (request.usageLimit() != null) {
            if (request.usageLimit() < currentCommittedCount) {
                log.warn("Usage limit {} cannot be less than current committed count {} (used={}, reserved={})",
                        request.usageLimit(), currentCommittedCount, safeUsedCount, safeReservedCount);
                throw new BusinessException(ErrorCode.USAGE_LIMIT_LESS_THAN_USED);
            }
            if (request.usageLimit() < request.usageLimitPerUser()) {
                log.warn("Usage limit {} cannot be less than usageLimitPerUser {}", request.usageLimit(), request.usageLimitPerUser());
                throw new BusinessException(ErrorCode.INVALID_USAGE_LIMIT);
            }
        }

        // Date validation according to lifecycle (UPCOMING vs ONGOING)
        boolean isUpcoming = voucher.getStartDate() != null && voucher.getStartDate().isAfter(now);
        if (isUpcoming) {
            LocalDateTime newStartDate = request.startDate() != null ? request.startDate() : voucher.getStartDate();
            if (newStartDate.isBefore(now.minusMinutes(5))) {
                log.warn("Voucher startDate {} is in the past", newStartDate);
                throw new BusinessException(ErrorCode.VOUCHER_START_DATE_IN_PAST);
            }
            if (!request.endDate().isAfter(newStartDate)) {
                log.warn("Voucher endDate {} must be after startDate {}", request.endDate(), newStartDate);
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
            }
            voucher.setStartDate(newStartDate);
        } else {
            // ONGOING: startDate is immutable
            if (!request.endDate().isAfter(now) || (voucher.getStartDate() != null && !request.endDate().isAfter(voucher.getStartDate()))) {
                log.warn("Voucher endDate {} must be after now {} and startDate {}", request.endDate(), now, voucher.getStartDate());
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
            }
        }

        // Apply mutations
        voucher.setName(request.name().trim());
        if (safeUsedCount == 0) {
            voucher.setDiscountType(request.discountType());
            voucher.setDiscountValue(request.discountValue());
            voucher.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO);
            voucher.setMaxDiscountAmount(effectiveMaxDiscount);
        }
        voucher.setUsageLimit(request.usageLimit());
        voucher.setUsageLimitPerUser(request.usageLimitPerUser());
        voucher.setEndDate(request.endDate());

        voucher = voucherRepository.saveAndFlush(voucher);
        log.info("Successfully updated platform voucher id={}, code={}", voucher.getId(), voucher.getCode());

        return AdminVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminVoucherDetailResponse updatePlatformVoucherStatus(UUID id, UpdatePlatformVoucherStatusRequest request) {
        log.info("Processing updatePlatformVoucherStatus for voucherId={}", id);

        if (id == null) {
            log.warn("Attempt to update voucher status with null id");
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        if (request == null || request.isActive() == null) {
            log.warn("Attempt to update voucher status with null request or null isActive");
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        Voucher voucher = voucherRepository.findWithShopById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.isDeleted()) {
            log.warn("Attempt to update soft-deleted voucher id={}", id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        if (voucher.getType() != VoucherType.PLATFORM) {
            log.warn("Attempt to update non-platform voucher id={} of type={}", id, voucher.getType());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_TYPE);
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if voucher has already expired
        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            log.warn("Voucher id={} has already expired at {}. Rejecting status update.", id, voucher.getEndDate());
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_EXPIRED);
        }

        // Idempotent check: if current state matches target state, no-op return
        if (voucher.isActive() == request.isActive()) {
            log.info("Platform voucher id={} is already isActive={}. Idempotent return.", id, request.isActive());
            return AdminVoucherDetailResponse.from(voucher, now);
        }

        voucher.setActive(request.isActive());
        voucher = voucherRepository.saveAndFlush(voucher);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS/SYSTEM";
        log.info("Successfully updated platform voucher status id={}, code={}, isActive={} by operator={}",
                voucher.getId(), voucher.getCode(), voucher.isActive(), operator);

        return AdminVoucherDetailResponse.from(voucher, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatformVoucher(UUID id) {
        log.info("Processing deletePlatformVoucher for voucherId={}", id);

        if (id == null) {
            log.warn("Attempt to delete platform voucher with null id");
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        if (voucher.isDeleted()) {
            log.warn("Attempt to delete already soft-deleted voucher id={}", id);
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        if (voucher.getType() != VoucherType.PLATFORM) {
            log.warn("Attempt to delete non-platform voucher id={} of type={}", id, voucher.getType());
            throw new BusinessException(ErrorCode.INVALID_VOUCHER_TYPE);
        }

        int safeReservedCount = voucher.getReservedCount() != null ? voucher.getReservedCount() : 0;
        if (safeReservedCount > 0) {
            log.warn("Voucher {} has active reservations (reservedCount={}). Rejecting deletion.", id, safeReservedCount);
            throw new BusinessException(ErrorCode.VOUCHER_HAS_ACTIVE_RESERVATIONS);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS/SYSTEM";

        int safeUsedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        if (safeUsedCount == 0) {
            voucherRepository.delete(voucher);
            log.info("Successfully hard-deleted unused platform voucher id={}, code='{}' by operator={}",
                    id, voucher.getCode(), operator);
        } else {
            voucher.setDeleted(true);
            voucher.setActive(false);
            voucher.setDeletedAt(LocalDateTime.now());
            voucherRepository.save(voucher);
            log.info("Successfully soft-deleted used platform voucher id={}, code='{}' by operator={}",
                    id, voucher.getCode(), operator);
        }
    }

    private boolean isBigDecimalDifferent(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return a.compareTo(b) != 0;
    }
}
