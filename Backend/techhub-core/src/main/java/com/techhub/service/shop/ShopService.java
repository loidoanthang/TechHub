package com.techhub.service.shop;

import com.techhub.modules.shop.dto.response.PublicShopResponse;

public interface ShopService {

    PublicShopResponse getPublicShopProfile(String sellerName);
}
