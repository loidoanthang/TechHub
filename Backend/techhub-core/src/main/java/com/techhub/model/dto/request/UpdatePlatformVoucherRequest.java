package com.techhub.model.dto.request;

import com.techhub.model.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdatePlatformVoucherRequest(
    @NotBlank(message = "Voucher name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    String name,

    @NotNull(message = "Discount type is required")
    DiscountType discountType,

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @DecimalMax(value = "999999999999.99", message = "Discount value cannot exceed 999,999,999,999.99")
    BigDecimal discountValue,

    @DecimalMin(value = "0.00", message = "Minimum order amount must be non-negative")
    @DecimalMax(value = "999999999999.99", message = "Minimum order amount cannot exceed 999,999,999,999.99")
    BigDecimal minOrderAmount,

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    @DecimalMax(value = "999999999999.99", message = "Maximum discount amount cannot exceed 999,999,999,999.99")
    BigDecimal maxDiscountAmount,

    @Min(value = 1, message = "Usage limit must be at least 1")
    Integer usageLimit,

    @NotNull(message = "Usage limit per user is required")
    @Min(value = 1, message = "Usage limit per user must be at least 1")
    Integer usageLimitPerUser,

    LocalDateTime startDate,

    @NotNull(message = "End date is required")
    LocalDateTime endDate
) {}
