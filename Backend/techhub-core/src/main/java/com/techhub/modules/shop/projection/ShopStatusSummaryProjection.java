package com.techhub.modules.shop.projection;

import com.techhub.modules.shop.enums.ShopStatus;

public interface ShopStatusSummaryProjection {
    ShopStatus getStatus();
    Long getCount();
}
