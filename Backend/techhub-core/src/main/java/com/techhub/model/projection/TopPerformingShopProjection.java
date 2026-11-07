package com.techhub.model.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopPerformingShopProjection {
    UUID getShopId();
    String getShopName();
    String getLogoUrl();
    Long getTotalOrders();
    BigDecimal getTotalGmv();
    BigDecimal getCommissionContributed();
}
