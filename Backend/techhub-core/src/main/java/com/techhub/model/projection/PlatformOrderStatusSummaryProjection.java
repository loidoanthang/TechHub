package com.techhub.model.projection;

import com.techhub.model.enums.OrderStatus;

import java.math.BigDecimal;

public interface PlatformOrderStatusSummaryProjection {
    OrderStatus getStatus();
    Long getOrderCount();
    BigDecimal getTotalAmount();
    BigDecimal getTotalCommission();
}
