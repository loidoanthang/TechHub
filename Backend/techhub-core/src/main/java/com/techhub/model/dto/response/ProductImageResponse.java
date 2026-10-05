package com.techhub.model.dto.response;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder
) {}
