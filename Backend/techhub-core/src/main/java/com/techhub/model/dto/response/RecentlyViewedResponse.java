package com.techhub.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentlyViewedResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String slug;
    private String coverImageUrl;
    private BigDecimal minPrice;
    private Boolean inStock;
    private Boolean isShopPaused;
    private LocalDateTime viewedAt;
}
