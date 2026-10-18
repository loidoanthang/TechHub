package com.techhub.service.voucher;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherStatusRequest;
import com.techhub.model.dto.response.AdminVoucherDetailResponse;
import com.techhub.model.dto.response.AdminVoucherResponse;
import com.techhub.model.dto.response.PlatformVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;

import java.util.UUID;

public interface AdminVoucherService {
    PlatformVoucherResponse createPlatformVoucher(CreatePlatformVoucherRequest request);

    PageResponse<AdminVoucherResponse> getVouchers(
            VoucherType type,
            UUID shopId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminVoucherDetailResponse getVoucherDetail(UUID id);

    AdminVoucherDetailResponse updatePlatformVoucher(UUID id, UpdatePlatformVoucherRequest request);

    AdminVoucherDetailResponse updatePlatformVoucherStatus(UUID id, UpdatePlatformVoucherStatusRequest request);

    void deletePlatformVoucher(UUID id);
}
