package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.FollowedShopResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.shop.ShopFollowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/followed-shops")
@RequiredArgsConstructor
public class BuyerShopFollowingController {

    private final ShopFollowingService shopFollowingService;

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PageResponse<FollowedShopResponse>> getFollowedShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(currentUserId, page, size);
        return ApiResponse.success(response, "Followed shops retrieved successfully", null);
    }
}
