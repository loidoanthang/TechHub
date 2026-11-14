package com.techhub.modules.category.dto.response;

import com.techhub.modules.category.enums.CategoryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategorySummaryResponse(
        UUID id,
        String name,
        String description,
        CategoryStatus status,
        long attributesCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
