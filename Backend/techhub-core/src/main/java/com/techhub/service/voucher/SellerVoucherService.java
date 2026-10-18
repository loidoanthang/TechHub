package com.techhub.service.voucher;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.model.dto.response.ShopVoucherDetailResponse;
import com.techhub.model.dto.response.ShopVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;

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
