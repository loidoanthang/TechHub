package com.techhub.model.dto.response;

import lombok.Builder;

@Builder
public record PaymentChannelResponse(
        String code,
        String name,
        String description,
        String iconUrl
) {}
