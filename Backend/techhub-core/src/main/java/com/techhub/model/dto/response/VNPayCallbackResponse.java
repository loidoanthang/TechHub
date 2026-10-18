package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VNPayCallbackResponse(
        boolean success,
        UUID paymentId,
        String transactionNo,
        BigDecimal amount,
        String bankCode,
        String orderInfo,
        String responseCode,
        String message,
        List<String> orderNumbers
) {}
