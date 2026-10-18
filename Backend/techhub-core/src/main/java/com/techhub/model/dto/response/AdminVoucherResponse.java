package com.techhub.model.dto.response;

import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminVoucherResponse(
    UUID id,
    UUID shopId,
    String shopName,
    ShopStatus shopStatus,
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
    boolean isDeleted,
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
        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(effectiveNow)) {
            return VoucherStatus.EXPIRED;
        }
        if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(effectiveNow)) {
            return VoucherStatus.UPCOMING;
        }
        return VoucherStatus.ONGOING;
    }

    public static AdminVoucherResponse from(Voucher voucher, LocalDateTime now) {
        if (voucher == null) {
            return null;
        }
        return new AdminVoucherResponse(
            voucher.getId(),
            voucher.getShop() != null ? voucher.getShop().getId() : null,
            voucher.getShop() != null ? voucher.getShop().getName() : null,
            voucher.getShop() != null ? voucher.getShop().getStatus() : null,
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
            voucher.isDeleted(),
            computeStatus(voucher, now),
            voucher.getCreatedAt()
        );
    }
}
