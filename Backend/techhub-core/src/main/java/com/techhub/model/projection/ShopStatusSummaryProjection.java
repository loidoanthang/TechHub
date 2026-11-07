package com.techhub.model.projection;

import com.techhub.model.enums.ShopStatus;

public interface ShopStatusSummaryProjection {
    ShopStatus getStatus();
    Long getCount();
}
