package com.techhub.modules.seller.dto.request;

import com.techhub.modules.seller.enums.PayoutProcessAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessPayoutRequest(
        @NotNull(message = "Action must not be null")
        PayoutProcessAction action,

        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String rejectionReason
) {
}
