package com.techhub.model.dto.request.payout;

import com.techhub.model.enums.PayoutProcessAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessPayoutRequest(
        @NotNull(message = "Action must not be null")
        PayoutProcessAction action,

        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String rejectionReason
) {
}
