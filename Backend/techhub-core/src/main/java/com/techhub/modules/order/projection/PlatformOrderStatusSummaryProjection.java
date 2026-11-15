package com.techhub.modules.order.projection;

import com.techhub.modules.order.enums.OrderStatus;

import java.math.BigDecimal;

public interface PlatformOrderStatusSummaryProjection {
    OrderStatus getStatus();
    Long getOrderCount();
    BigDecimal getTotalAmount();
    BigDecimal getTotalCommission();
}
