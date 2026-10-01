package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.SellerApprovalRequest;
import com.techhub.model.dto.response.AdminSellerApprovalResponse;
import com.techhub.model.dto.response.AdminSellerDetailResponse;
import com.techhub.model.dto.response.AdminSellerResponse;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.service.admin.AdminSellerService;
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
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    @GetMapping
    public ApiResponse<PageResponse<AdminSellerResponse>> getSellers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SellerVerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminSellerResponse> data = adminSellerService.getSellers(
            keyword, status, page, size, sortBy, sortDir
        );
        return ApiResponse.success(data, "Seller list retrieved successfully.", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminSellerDetailResponse> getSellerDetail(@PathVariable UUID id) {
        AdminSellerDetailResponse data = adminSellerService.getSellerDetail(id);
        return ApiResponse.success(data, "Seller details retrieved successfully.", null);
    }

    @PatchMapping("/{id}/approval")
    public ApiResponse<AdminSellerApprovalResponse> updateSellerApproval(
            @PathVariable UUID id,
            @Valid @RequestBody SellerApprovalRequest request
    ) {
        AdminSellerApprovalResponse data = adminSellerService.updateSellerApproval(id, request);
        return ApiResponse.success(data, "Seller approval status updated successfully.", null);
    }
}

