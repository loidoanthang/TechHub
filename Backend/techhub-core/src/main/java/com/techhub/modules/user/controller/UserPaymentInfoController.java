package com.techhub.modules.user.controller;

import com.techhub.common.ApiResponse;
import com.techhub.common.VNPayUtils;
import com.techhub.modules.payment.dto.response.PaymentInfoResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayTokenLinkResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.security.annotation.RequireActiveBuyer;
import com.techhub.modules.user.service.UserPaymentInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/payment-info")
@RequiredArgsConstructor
@Validated
public class UserPaymentInfoController {

    private final UserPaymentInfoService userPaymentInfoService;

    /**
     * Khởi tạo yêu cầu liên kết thẻ VNPAY Token (nhận link chuyển hướng sang VNPAY để nhập thẻ & OTP).
     */
    @PostMapping("/link-init")
    @PreAuthorize("hasRole('BUYER')")
    @RequireActiveBuyer
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VNPayTokenLinkResponse> initiateTokenLink(HttpServletRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String clientIp = VNPayUtils.getClientIp(request);
        VNPayTokenLinkResponse response = userPaymentInfoService.initiateTokenLink(currentUserId, clientIp);
        return ApiResponse.success(response, "Khởi tạo liên kết thẻ VNPAY thành công", null);
    }

    /**
     * Webhook IPN từ VNPAY để xác nhận hoàn tất liên kết thẻ và lưu token.
     */
    @GetMapping("/token-ipn")
    public VNPayIpnResponse processTokenCreateIpn(@RequestParam Map<String, String> queryParams) {
        return userPaymentInfoService.processTokenCreateIpn(queryParams);
    }

    /**
     * Lấy danh sách thẻ thanh toán liên kết của người dùng.
     */
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<PaymentInfoResponse>> getPaymentInfos() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<PaymentInfoResponse> response = userPaymentInfoService.getPaymentInfos(currentUserId);
        return ApiResponse.success(response, "Payment information retrieved successfully", null);
    }

    /**
     * Đặt một thẻ làm phương thức thanh toán mặc định.
     */
    @PatchMapping("/{id}/default")
    @PreAuthorize("hasRole('BUYER')")
    @RequireActiveBuyer
    public ApiResponse<PaymentInfoResponse> setDefaultPaymentInfo(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentInfoResponse response = userPaymentInfoService.setDefaultPaymentInfo(currentUserId, id);
        return ApiResponse.success(response, "Default payment information updated successfully", null);
    }

    /**
     * Hủy liên kết thẻ.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    @RequireActiveBuyer
    public ApiResponse<Void> deletePaymentInfo(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        userPaymentInfoService.deletePaymentInfo(currentUserId, id);
        return ApiResponse.success(null, "Payment information deleted successfully", null);
    }
}
