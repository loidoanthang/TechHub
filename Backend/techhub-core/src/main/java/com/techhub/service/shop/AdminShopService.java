package com.techhub.service.shop;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.AdminUpdateShopStatusRequest;
import com.techhub.model.dto.response.AdminShopDetailResponse;
import com.techhub.model.dto.response.AdminShopResponse;
import com.techhub.model.enums.ShopStatus;

import java.util.UUID;

public interface AdminShopService {

    PageResponse<AdminShopResponse> getShops(
            String keyword,
            ShopStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminShopDetailResponse getShopDetail(UUID id);

    AdminShopResponse updateShopStatus(UUID id, AdminUpdateShopStatusRequest request);
}
