package com.techhub.model.dto.response.review;

import com.techhub.model.entity.ReviewMedia;
import com.techhub.model.enums.ReviewMediaType;

import java.util.UUID;

public record ReviewMediaResponse(
        UUID id,
        String mediaUrl,
        ReviewMediaType mediaType,
        int displayOrder
) {
    public static ReviewMediaResponse from(ReviewMedia media) {
        if (media == null) return null;
        return new ReviewMediaResponse(
                media.getId(),
                media.getMediaUrl(),
                media.getMediaType(),
                media.getDisplayOrder()
        );
    }
}
