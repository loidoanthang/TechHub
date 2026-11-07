package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.AdminUpdateShopStatusRequest;
import com.techhub.model.dto.response.AdminShopDetailResponse;
import com.techhub.model.dto.response.AdminShopResponse;
import com.techhub.model.enums.ShopStatus;
import com.techhub.service.shop.AdminShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminShopController {

    private final AdminShopService adminShopService;

    @GetMapping
    public ApiResponse<PageResponse<AdminShopResponse>> getShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminShopResponse> data = adminShopService.getShops(
                keyword, status, page, size, sortBy, sortDir
        );
        return ApiResponse.success(data, "Shop list retrieved successfully.", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminShopDetailResponse> getShopDetail(@PathVariable UUID id) {
        AdminShopDetailResponse data = adminShopService.getShopDetail(id);
        return ApiResponse.success(data, "Shop details retrieved successfully.", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminShopResponse> updateShopStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateShopStatusRequest request
    ) {
        AdminShopResponse data = adminShopService.updateShopStatus(id, request);
        return ApiResponse.success(data, "Shop status updated successfully.", null);
    }
}
