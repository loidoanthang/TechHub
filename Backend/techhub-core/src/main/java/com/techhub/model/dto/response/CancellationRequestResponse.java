package com.techhub.model.dto.response;

import com.techhub.model.enums.CancellationRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CancellationRequestResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        CancellationRequestStatus status,
        String statusDescription,
        String reason,
        String sellerResponse,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt
) {}
