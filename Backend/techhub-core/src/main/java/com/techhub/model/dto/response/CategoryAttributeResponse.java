package com.techhub.model.dto.response;

import com.techhub.model.enums.CategoryAttributeDataType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryAttributeResponse(
        UUID id,
        String name,
        CategoryAttributeDataType dataType,
        List<String> options,
        String sampleValue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
