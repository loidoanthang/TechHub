package com.techhub.model.dto.response;

import com.techhub.model.enums.CategoryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryDetailResponse(
        UUID id,
        String name,
        String description,
        CategoryStatus status,
        List<CategoryAttributeResponse> attributes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
