package com.techhub.modules.seller.dto.response;

import com.techhub.modules.user.enums.Role;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminSellerApprovalResponse(
        UUID sellerId,
        UUID userId,
        UUID shopId,
        String sellerName,
        String shopName,
        SellerVerificationStatus verificationStatus,
        ShopStatus shopStatus,
        String rejectionReason,
        Set<Role> roles,
        LocalDateTime updatedAt
) {}
