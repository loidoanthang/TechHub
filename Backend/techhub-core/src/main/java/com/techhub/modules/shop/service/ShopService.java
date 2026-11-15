package com.techhub.modules.shop.service;

import com.techhub.modules.shop.dto.response.PublicShopResponse;

public interface ShopService {

    PublicShopResponse getPublicShopProfile(String sellerName);
}
