package com.techhub.model.dto.response;

import java.util.UUID;

public record ReorderUnavailableItemDto(
        UUID productVariantId,
        String productName,
        String variantName,
        String sku,
        int requestedQuantity,
        String reason,
        String message
) {}
