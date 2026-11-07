package com.techhub.model.dto.response;

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
public class FollowShopResponse {
    private UUID shopId;
    private String shopName;
    private String sellerName;
    private LocalDateTime followedAt;
    private Long followerCount;
    private Boolean following;
}
