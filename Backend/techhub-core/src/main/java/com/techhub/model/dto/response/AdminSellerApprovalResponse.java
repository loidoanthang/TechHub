package com.techhub.model.dto.response;

import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;

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
