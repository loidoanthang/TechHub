package com.techhub.model.dto.response;

import com.techhub.model.enums.CategoryStatus;

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
