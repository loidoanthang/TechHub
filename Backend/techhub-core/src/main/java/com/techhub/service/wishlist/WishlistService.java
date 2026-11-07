package com.techhub.service.wishlist;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.WishlistResponse;

import java.util.UUID;

public interface WishlistService {

    WishlistResponse addToWishlist(UUID userId, UUID productId);

    void removeFromWishlist(UUID userId, UUID productId);

    PageResponse<WishlistResponse> getWishlist(UUID userId, int page, int size);
}
