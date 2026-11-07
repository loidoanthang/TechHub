package com.techhub.model.dto.response;

import com.techhub.model.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowedShopResponse {
    private UUID shopId;
    private String sellerName;
    private String shopName;
    private String avatarUrl;
    private String bannerUrl;
    private ShopStatus status;
    private Boolean isPaused;
    private Long followerCount;
    private Long totalProducts;
    private LocalDateTime followedAt;
}
