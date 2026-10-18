package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.CreateReturnRequest;
import com.techhub.model.dto.response.ReturnRequestResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.order.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping("/orders/{id}/return-request")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReturnRequestResponse> createReturnRequest(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.createReturnRequest(currentUserId, id, request);
        return ApiResponse.success(response, "Gửi yêu cầu trả hàng / hoàn tiền thành công", null);
    }

    @PostMapping("/return-requests/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReturnRequestResponse> cancelReturnRequest(
            @PathVariable UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.cancelReturnRequest(currentUserId, id);
        return ApiResponse.success(response, "Rút lại yêu cầu trả hàng thành công", null);
    }

    @GetMapping("/return-requests/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReturnRequestResponse> getBuyerReturnRequest(
            @PathVariable UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.getBuyerReturnRequest(currentUserId, id);
        return ApiResponse.success(response, "Fetched return request detail successfully", null);
    }

    @GetMapping("/orders/{id}/return-request")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReturnRequestResponse> getBuyerReturnRequestByOrderId(
            @PathVariable UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequestResponse response = returnRequestService.getBuyerReturnRequestByOrderId(currentUserId, id);
        return ApiResponse.success(response, "Fetched order return request successfully", null);
    }
}
