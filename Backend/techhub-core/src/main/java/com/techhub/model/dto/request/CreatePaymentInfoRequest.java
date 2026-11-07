package com.techhub.model.dto.request;

import com.techhub.model.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentInfoRequest(
        @NotNull(message = "Payment type is required")
        PaymentType paymentType,

        @NotBlank(message = "Provider is required")
        @Size(min = 2, max = 50, message = "Provider must be between 2 and 50 characters")
        String provider,

        @NotBlank(message = "Account number is required")
        @Size(min = 6, max = 30, message = "Account number must be between 6 and 30 characters")
        String accountNumber,

        @NotBlank(message = "Account holder name is required")
        @Size(min = 2, max = 150, message = "Account holder name must be between 2 and 150 characters")
        String accountHolderName,

        String cardExpiry,

        Boolean isDefault
) {}
