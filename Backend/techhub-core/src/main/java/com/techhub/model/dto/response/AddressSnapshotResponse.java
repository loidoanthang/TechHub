package com.techhub.model.dto.response;

import java.util.UUID;

public record AddressSnapshotResponse(
        UUID id,
        String recipientName,
        String phoneNumber,
        String streetAddress,
        String ward,
        String district,
        String provinceCity,
        String fullAddress
) {}
