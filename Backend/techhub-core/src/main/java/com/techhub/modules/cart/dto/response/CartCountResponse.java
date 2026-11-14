package com.techhub.modules.cart.dto.response;

public record CartCountResponse(
        int totalItemTypes
) {
    public static CartCountResponse empty() {
        return new CartCountResponse(0);
    }
}
