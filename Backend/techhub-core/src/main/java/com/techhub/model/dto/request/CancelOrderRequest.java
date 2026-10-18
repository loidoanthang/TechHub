package com.techhub.model.dto.request;

import com.techhub.model.enums.CancellationReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotNull(message = "Cancellation reason code is required")
        CancellationReasonCode reasonCode,

        @Size(max = 500, message = "Reason note cannot exceed 500 characters")
        String reasonNote
) {}
