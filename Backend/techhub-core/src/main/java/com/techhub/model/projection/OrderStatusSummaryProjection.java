package com.techhub.model.projection;

import com.techhub.model.enums.OrderStatus;

import java.math.BigDecimal;

public interface OrderStatusSummaryProjection {
    OrderStatus getStatus();
    Long getOrderCount();
    BigDecimal getTotalEarnings();
}
