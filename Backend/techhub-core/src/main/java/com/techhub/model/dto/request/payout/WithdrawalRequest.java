package com.techhub.model.dto.request.payout;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull(message = "Withdrawal amount is required")
        @DecimalMin(value = "50000.00", message = "Withdrawal amount must be at least 50,000 VND")
        @DecimalMax(value = "100000000.00", message = "Withdrawal amount must not exceed 100,000,000 VND")
        @Digits(integer = 11, fraction = 2, message = "Withdrawal amount format is invalid")
        BigDecimal amount
) {
}
