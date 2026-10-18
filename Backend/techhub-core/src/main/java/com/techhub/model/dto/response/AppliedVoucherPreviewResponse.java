package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AppliedVoucherPreviewResponse(
        UUID voucherId,
        String code,
        String name,
        BigDecimal discountAmount
) {}
