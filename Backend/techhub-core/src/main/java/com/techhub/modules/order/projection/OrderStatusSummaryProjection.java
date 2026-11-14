package com.techhub.modules.order.projection;

import com.techhub.modules.order.enums.OrderStatus;

import java.math.BigDecimal;

public interface OrderStatusSummaryProjection {
    OrderStatus getStatus();
    Long getOrderCount();
    BigDecimal getTotalEarnings();
}
