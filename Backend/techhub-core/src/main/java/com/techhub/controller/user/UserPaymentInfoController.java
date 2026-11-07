package com.techhub.controller.user;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.CreatePaymentInfoRequest;
import com.techhub.model.dto.response.PaymentInfoResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.user.UserPaymentInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/payment-info")
@RequiredArgsConstructor
@Validated
public class UserPaymentInfoController {

    private final UserPaymentInfoService userPaymentInfoService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentInfoResponse> createPaymentInfo(
            @Valid @RequestBody CreatePaymentInfoRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentInfoResponse response = userPaymentInfoService.createPaymentInfo(currentUserId, request);
        return ApiResponse.success(response, "Payment information added successfully", null);
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<PaymentInfoResponse>> getPaymentInfos() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<PaymentInfoResponse> response = userPaymentInfoService.getPaymentInfos(currentUserId);
        return ApiResponse.success(response, "Payment information retrieved successfully", null);
    }

    @PatchMapping("/{id}/default")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PaymentInfoResponse> setDefaultPaymentInfo(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentInfoResponse response = userPaymentInfoService.setDefaultPaymentInfo(currentUserId, id);
        return ApiResponse.success(response, "Default payment information updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<Void> deletePaymentInfo(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        userPaymentInfoService.deletePaymentInfo(currentUserId, id);
        return ApiResponse.success(null, "Payment information deleted successfully", null);
    }
}
