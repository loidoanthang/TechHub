package com.techhub.model.dto.response;

import java.util.List;
import java.util.UUID;

public record ReorderResponse(
        UUID orderId,
        int totalItemsRequested,
        int itemsAddedSuccessfully,
        List<ReorderItemDto> addedItems,
        List<ReorderUnavailableItemDto> unavailableItems,
        int cartTotalItemTypes,
        int cartTotalQuantity
) {}
