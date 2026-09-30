package com.techhub.model.dto.response;

import com.techhub.model.entity.Address;
import com.techhub.model.enums.AddressLabel;

import java.time.LocalDateTime;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        AddressLabel label,
        String recipientName,
        String phone,
        String province,
        String district,
        String ward,
        String streetAddress,
        String fullAddress,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse from(Address address) {
        String fullAddress = String.format("%s, %s, %s, %s",
                address.getStreetAddress(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince()
        );
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getStreetAddress(),
                fullAddress,
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
