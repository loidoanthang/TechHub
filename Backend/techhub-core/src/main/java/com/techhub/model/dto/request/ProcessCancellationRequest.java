package com.techhub.model.dto.request;

import com.techhub.model.enums.CancellationRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessCancellationRequest(
        @NotNull(message = "Decision status is required (APPROVED or REJECTED)")
        CancellationRequestStatus decision,

        @Size(max = 500, message = "Seller response cannot exceed 500 characters")
        String sellerResponse
) {}
