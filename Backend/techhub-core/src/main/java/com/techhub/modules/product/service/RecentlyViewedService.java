package com.techhub.modules.product.service;

import com.techhub.modules.product.dto.response.RecentlyViewedResponse;

import java.util.List;
import java.util.UUID;

public interface RecentlyViewedService {

    void recordProductView(UUID userId, UUID productId);

    List<RecentlyViewedResponse> getRecentlyViewedProducts(UUID userId);
}
