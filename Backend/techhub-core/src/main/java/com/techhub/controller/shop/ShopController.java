package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.service.shop.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{sellerName}")
    public ResponseEntity<ApiResponse<PublicShopResponse>> getPublicShopProfile(
            @PathVariable String sellerName
    ) {
        PublicShopResponse data = shopService.getPublicShopProfile(sellerName);
        return ResponseEntity.ok(ApiResponse.success(data, "Shop profile retrieved successfully", null));
    }
}
