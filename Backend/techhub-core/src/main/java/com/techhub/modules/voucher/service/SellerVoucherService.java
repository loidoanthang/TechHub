package com.techhub.modules.voucher.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.shop.dto.request.CreateShopVoucherRequest;
import com.techhub.modules.shop.dto.request.UpdateShopVoucherRequest;
import com.techhub.modules.shop.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.modules.shop.dto.response.ShopVoucherDetailResponse;
import com.techhub.modules.shop.dto.response.ShopVoucherResponse;
import com.techhub.modules.voucher.enums.DiscountType;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;
import com.techhub.modules.voucher.enums.VoucherStatus;

import java.util.UUID;

public interface SellerVoucherService {
    ShopVoucherResponse createShopVoucher(UUID userId, CreateShopVoucherRequest request);

    PageResponse<ShopVoucherResponse> getShopVouchers(
            UUID userId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ShopVoucherDetailResponse getShopVoucherDetail(UUID userId, UUID id);

    ShopVoucherDetailResponse updateShopVoucher(UUID userId, UUID id, UpdateShopVoucherRequest request);

    ShopVoucherDetailResponse updateShopVoucherStatus(UUID userId, UUID id, UpdateShopVoucherStatusRequest request);

    void deleteShopVoucher(UUID userId, UUID id);
}
