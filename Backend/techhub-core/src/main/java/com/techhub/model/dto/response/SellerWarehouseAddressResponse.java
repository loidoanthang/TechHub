package com.techhub.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerWarehouseAddressResponse(
        UUID id,
        UUID shopId,
        String contactName,
        String phone,
        String province,
        String district,
        String ward,
        String streetAddress,
        String fullAddress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
