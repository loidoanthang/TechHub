package com.techhub.controller.voucher;

import com.techhub.common.ApiResponse;
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
import com.techhub.service.voucher.AdminVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final AdminVoucherService adminVoucherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlatformVoucherResponse> createPlatformVoucher(
            @Valid @RequestBody CreatePlatformVoucherRequest request
    ) {
        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);
        return ApiResponse.success(response, "Platform voucher created successfully", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminVoucherResponse>> getVouchers(
            @RequestParam(required = false) VoucherType type,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VoucherAppliesTo appliesTo,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminVoucherResponse> response = adminVoucherService.getVouchers(
                type, shopId, keyword, appliesTo, discountType, status, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Fetched admin vouchers successfully", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminVoucherDetailResponse> getVoucherDetail(@PathVariable UUID id) {
        AdminVoucherDetailResponse response = adminVoucherService.getVoucherDetail(id);
        return ApiResponse.success(response, "Fetched admin voucher detail successfully", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminVoucherDetailResponse> updatePlatformVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformVoucherRequest request
    ) {
        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(id, request);
        return ApiResponse.success(response, "Platform voucher updated successfully", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminVoucherDetailResponse> updatePlatformVoucherStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformVoucherStatusRequest request
    ) {
        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(id, request);
        return ApiResponse.success(response, "Platform voucher status updated successfully", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlatformVoucher(@PathVariable UUID id) {
        adminVoucherService.deletePlatformVoucher(id);
        return ApiResponse.success(null, "Platform voucher deleted successfully", null);
    }
}
