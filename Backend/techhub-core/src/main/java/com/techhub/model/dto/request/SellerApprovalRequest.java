package com.techhub.model.dto.request;

import com.techhub.model.enums.SellerVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SellerApprovalRequest(
        @NotNull(message = "Approval status is required")
        SellerVerificationStatus status,

        @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
        String rejectionReason
) {}
