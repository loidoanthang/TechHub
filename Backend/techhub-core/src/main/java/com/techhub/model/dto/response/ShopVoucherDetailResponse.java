package com.techhub.model.dto.response;

import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShopVoucherDetailResponse(
    UUID id,
    UUID shopId,
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
    Integer remainingCount,
    Double redemptionRate,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean isActive,
    VoucherStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ShopVoucherDetailResponse from(Voucher voucher, LocalDateTime now) {
        if (voucher == null) {
            return null;
        }
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();

        Integer remainingCount = null;
        Double redemptionRate = null;
        int safeUsedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        int safeReservedCount = voucher.getReservedCount() != null ? voucher.getReservedCount() : 0;
        if (voucher.getUsageLimit() != null) {
            remainingCount = Math.max(0, voucher.getUsageLimit() - safeUsedCount);
            if (voucher.getUsageLimit() > 0) {
                double rate = ((double) safeUsedCount / voucher.getUsageLimit()) * 100.0;
                redemptionRate = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }

        return new ShopVoucherDetailResponse(
            voucher.getId(),
            voucher.getShop() != null ? voucher.getShop().getId() : null,
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
            safeReservedCount,
            safeUsedCount,
            remainingCount,
            redemptionRate,
            voucher.getStartDate(),
            voucher.getEndDate(),
            voucher.isActive(),
            ShopVoucherResponse.computeStatus(voucher, effectiveNow),
            voucher.getCreatedAt(),
            voucher.getUpdatedAt()
        );
    }

    public static ShopVoucherDetailResponse from(Voucher voucher) {
        return from(voucher, LocalDateTime.now());
    }
}
