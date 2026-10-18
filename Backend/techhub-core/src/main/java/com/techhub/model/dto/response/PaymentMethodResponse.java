package com.techhub.model.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PaymentMethodResponse(
        String code,
        String name,
        String description,
        String iconUrl,
        boolean isDefault,
        boolean isActive,
        List<PaymentChannelResponse> channels
) {}
