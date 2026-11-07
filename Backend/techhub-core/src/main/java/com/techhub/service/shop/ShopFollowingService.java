package com.techhub.service.shop;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.FollowShopResponse;
import com.techhub.model.dto.response.FollowedShopResponse;

import java.util.UUID;

public interface ShopFollowingService {

    FollowShopResponse followShop(UUID userId, String sellerName);

    void unfollowShop(UUID userId, String sellerName);

    PageResponse<FollowedShopResponse> getFollowedShops(UUID userId, int page, int size);
}
