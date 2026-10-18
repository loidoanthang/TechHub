package com.techhub.model.dto.response;

public record CartCountResponse(
        int totalItemTypes
) {
    public static CartCountResponse empty() {
        return new CartCountResponse(0);
    }
}
