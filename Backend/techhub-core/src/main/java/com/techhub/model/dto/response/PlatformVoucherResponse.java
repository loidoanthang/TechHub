package com.techhub.model.dto.response;

import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlatformVoucherResponse(
    UUID id,
    String code,
    String name,
    VoucherType type,
    VoucherAppliesTo appliesTo,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount,
    Integer usageLimit,
    Integer usageLimitPerUser,
    Integer reservedCount,
    Integer usedCount,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean isActive,
    VoucherStatus status,
    LocalDateTime createdAt
) {

    public static VoucherStatus computeStatus(Voucher voucher, LocalDateTime now) {
        if (voucher == null) {
            return null;
        }
        if (!voucher.isActive()) {
            return VoucherStatus.DISABLED;
        }
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();
        if (voucher.getEndDate().isBefore(effectiveNow)) {
            return VoucherStatus.EXPIRED;
        }
        if (voucher.getStartDate().isAfter(effectiveNow)) {
            return VoucherStatus.UPCOMING;
        }
        return VoucherStatus.ONGOING;
    }

    public static PlatformVoucherResponse from(Voucher voucher, LocalDateTime now) {
        if (voucher == null) {
            return null;
        }
        return new PlatformVoucherResponse(
            voucher.getId(),
            voucher.getCode(),
            voucher.getName(),
            voucher.getType(),
            voucher.getAppliesTo(),
            voucher.getDiscountType(),
            voucher.getDiscountValue(),
            voucher.getMinOrderAmount(),
            voucher.getMaxDiscountAmount(),
            voucher.getUsageLimit(),
            voucher.getUsageLimitPerUser(),
            voucher.getReservedCount(),
            voucher.getUsedCount(),
            voucher.getStartDate(),
            voucher.getEndDate(),
            voucher.isActive(),
            computeStatus(voucher, now),
            voucher.getCreatedAt()
        );
    }
}
