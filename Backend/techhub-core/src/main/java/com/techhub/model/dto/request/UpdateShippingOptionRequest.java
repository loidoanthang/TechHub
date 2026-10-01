package com.techhub.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateShippingOptionRequest(
        @NotNull(message = "Shipping fee is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Shipping fee cannot be negative")
        @DecimalMax(value = "10000000.00", inclusive = true, message = "Shipping fee cannot exceed 10,000,000 VND")
        @Digits(integer = 8, fraction = 2, message = "Shipping fee must have at most 8 integer digits and 2 decimal places")
        BigDecimal shippingFee
) {
}
