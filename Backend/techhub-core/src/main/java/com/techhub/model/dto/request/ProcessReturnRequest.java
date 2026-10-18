package com.techhub.model.dto.request;

import com.techhub.model.enums.ReturnRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessReturnRequest(
        @NotNull(message = "Decision status is required (APPROVED or REJECTED)")
        ReturnRequestStatus decision,

        @Size(max = 500, message = "Seller note cannot exceed 500 characters")
        String sellerNote
) {}
