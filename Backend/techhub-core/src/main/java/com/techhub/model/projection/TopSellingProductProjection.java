package com.techhub.model.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopSellingProductProjection {
    UUID getProductId();
    String getProductName();
    String getImageUrl();
    Long getUnitsSold();
    BigDecimal getTotalRevenue();
}
