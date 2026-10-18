package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReturnRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason,

        String proofImages,

        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName,

        @Size(max = 50, message = "Bank account number cannot exceed 50 characters")
        String bankAccountNumber,

        @Size(max = 150, message = "Bank account holder cannot exceed 150 characters")
        String bankAccountHolder
) {}
