package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.ProcessReturnRequest;
import com.techhub.model.dto.response.ReturnRequestResponse;
import com.techhub.model.enums.ReturnRequestStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.order.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller/return-requests")
@RequiredArgsConstructor
public class SellerReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getSellerReturnRequests(
            @RequestParam(required = false) ReturnRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<ReturnRequestResponse> response = returnRequestService.getSellerReturnRequests(
                currentUserId, status, page, size
        );
        return ApiResponse.success(response, "Fetched seller return requests successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ReturnRequestResponse> getSellerReturnRequestDetail(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.getSellerReturnRequestDetail(currentUserId, id);
        return ApiResponse.success(response, "Fetched return request detail successfully", null);
    }

    @PatchMapping("/{id}/process")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ReturnRequestResponse> processReturnRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessReturnRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.processReturnRequest(currentUserId, id, request);
        return ApiResponse.success(response, "Xử lý yêu cầu trả hàng / hoàn tiền thành công", null);
    }
}
