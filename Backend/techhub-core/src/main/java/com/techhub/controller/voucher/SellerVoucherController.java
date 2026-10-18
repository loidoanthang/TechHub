package com.techhub.controller.voucher;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.model.dto.response.ShopVoucherDetailResponse;
import com.techhub.model.dto.response.ShopVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.voucher.SellerVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller/vouchers")
@RequiredArgsConstructor
@Validated
public class SellerVoucherController {

    private final SellerVoucherService sellerVoucherService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopVoucherResponse> createShopVoucher(
            @Valid @RequestBody CreateShopVoucherRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ShopVoucherResponse response = sellerVoucherService.createShopVoucher(userId, request);
        return ApiResponse.success(response, "Shop voucher created successfully", null);
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<ShopVoucherResponse>> getShopVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VoucherAppliesTo appliesTo,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                userId, keyword, appliesTo, discountType, status, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Fetched shop vouchers successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ShopVoucherDetailResponse> getShopVoucherDetail(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, id);
        return ApiResponse.success(response, "Fetched shop voucher detail successfully", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ShopVoucherDetailResponse> updateShopVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShopVoucherRequest request
     ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, id, request);
        return ApiResponse.success(response, "Shop voucher updated successfully", null);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ShopVoucherDetailResponse> updateShopVoucherStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShopVoucherStatusRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, id, request);
        return ApiResponse.success(response, "Shop voucher status updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<Void> deleteShopVoucher(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        sellerVoucherService.deleteShopVoucher(userId, id);
        return ApiResponse.success(null, "Shop voucher deleted successfully", null);
    }
}
