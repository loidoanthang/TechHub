package com.techhub.model.dto.response;

import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SellerRegistrationResponse(
        UUID sellerId,
        UUID shopId,
        String sellerName,
        String shopName,
        SellerVerificationStatus verificationStatus,
        ShopStatus shopStatus,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        String warehouseAddress,
        BigDecimal shippingFee,
        LocalDateTime createdAt,
        boolean isResubmitted
) {}
