package com.techhub.model.dto.response;

import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminVoucherDetailResponse(
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
        int usageLimitPerUser,
        int reservedCount,
        int usedCount,
        Integer remainingCount,
        Double redemptionRate,
        UUID shopId,
        String shopName,
        ShopStatus shopStatus,
        String shopAvatarUrl,
        String sponsor,
        String subsidyNote,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean isActive,
        VoucherStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminVoucherDetailResponse from(Voucher voucher, LocalDateTime now) {
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

        String sponsor = voucher.getType() == VoucherType.PLATFORM ? "TECHHUB_PLATFORM" : "SHOP_SELLER";
        String subsidyNote = voucher.getType() == VoucherType.PLATFORM
                ? "Sàn TechHub tài trợ 100% chiết khấu ngân sách (BR-16D)"
                : "Gian hàng tự chịu trách nhiệm bù lỗ 100% chiết khấu (BR-16D)";

        Shop shop = voucher.getShop();

        return new AdminVoucherDetailResponse(
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
                voucher.getUsageLimitPerUser() != null ? voucher.getUsageLimitPerUser() : 1,
                safeReservedCount,
                safeUsedCount,
                remainingCount,
                redemptionRate,
                shop != null ? shop.getId() : null,
                shop != null ? shop.getName() : null,
                shop != null ? shop.getStatus() : null,
                shop != null ? shop.getAvatarUrl() : null,
                sponsor,
                subsidyNote,
                voucher.getStartDate(),
                voucher.getEndDate(),
                voucher.isActive(),
                AdminVoucherResponse.computeStatus(voucher, effectiveNow),
                voucher.getCreatedAt(),
                voucher.getUpdatedAt()
        );
    }
}
