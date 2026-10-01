package com.techhub.service.shop;

import com.techhub.model.dto.response.PublicShopResponse;

public interface ShopService {

    PublicShopResponse getPublicShopProfile(String sellerName);
}
