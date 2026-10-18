package com.techhub.model.dto.response;

import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public record CartPlatformVoucherResponse(
    UUID id,
    String code,
    String name,
    VoucherType type,
    VoucherAppliesTo appliesTo,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Boolean isEligible,
    BigDecimal estimatedDiscount,
    BigDecimal upsellRemainingAmount,
    String ineligibilityReason
) {

    public static CartPlatformVoucherResponse from(Voucher voucher, BigDecimal orderAmount, BigDecimal shippingFee) {
        if (voucher == null) {
            return null;
        }

        Boolean isEligible = null;
        BigDecimal estimatedDiscount = null;
        BigDecimal upsellRemainingAmount = null;
        String ineligibilityReason = null;

        if (orderAmount != null) {
            BigDecimal effectiveOrderAmount = orderAmount.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO
                    : orderAmount;
            BigDecimal minOrder = voucher.getMinOrderAmount() != null
                    ? voucher.getMinOrderAmount()
                    : BigDecimal.ZERO;

            if (effectiveOrderAmount.compareTo(minOrder) >= 0) {
                isEligible = true;
                upsellRemainingAmount = BigDecimal.ZERO;
                ineligibilityReason = null;

                if (voucher.getAppliesTo() == VoucherAppliesTo.SHIPPING_FEE) {
                    BigDecimal effectiveShipping = (shippingFee != null && shippingFee.compareTo(BigDecimal.ZERO) > 0)
                            ? shippingFee
                            : null;
                    if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        estimatedDiscount = effectiveShipping != null
                                ? voucher.getDiscountValue().min(effectiveShipping)
                                : voucher.getDiscountValue();
                    } else { // PERCENTAGE
                        BigDecimal baseAmount = effectiveShipping != null ? effectiveShipping : BigDecimal.ZERO;
                        BigDecimal rawDiscount = baseAmount
                                .multiply(voucher.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (voucher.getMaxDiscountAmount() != null) {
                            rawDiscount = rawDiscount.min(voucher.getMaxDiscountAmount());
                        }
                        estimatedDiscount = rawDiscount;
                    }
                } else { // PRODUCT
                    if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        estimatedDiscount = voucher.getDiscountValue().min(effectiveOrderAmount);
                    } else { // PERCENTAGE
                        BigDecimal rawDiscount = effectiveOrderAmount
                                .multiply(voucher.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (voucher.getMaxDiscountAmount() != null) {
                            rawDiscount = rawDiscount.min(voucher.getMaxDiscountAmount());
                        }
                        estimatedDiscount = rawDiscount.min(effectiveOrderAmount);
                    }
                }
            } else {
                isEligible = false;
                estimatedDiscount = BigDecimal.ZERO;
                upsellRemainingAmount = minOrder.subtract(effectiveOrderAmount);

                long roundedUpsell = upsellRemainingAmount.setScale(0, RoundingMode.HALF_UP).longValue();
                String formattedAmount = String.format(Locale.GERMAN, "%,d", roundedUpsell);
                ineligibilityReason = "Mua thêm " + formattedAmount + "₫ để dùng mã này";
            }
        }

        return new CartPlatformVoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getName(),
                voucher.getType(),
                voucher.getAppliesTo(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getMinOrderAmount(),
                voucher.getMaxDiscountAmount(),
                voucher.getStartDate(),
                voucher.getEndDate(),
                isEligible,
                estimatedDiscount,
                upsellRemainingAmount,
                ineligibilityReason
        );
    }

    public static CartPlatformVoucherResponse from(Voucher voucher) {
        return from(voucher, null, null);
    }
}
