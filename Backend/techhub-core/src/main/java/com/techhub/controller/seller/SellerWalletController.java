package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.payout.WithdrawalRequest;
import com.techhub.model.dto.response.payout.PayoutResponse;
import com.techhub.model.dto.response.wallet.SellerWalletTransactionResponse;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.security.SecurityUtils;
import com.techhub.service.seller.SellerWalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/wallet")
@RequiredArgsConstructor
@Validated
public class SellerWalletController {

    private final SellerWalletService sellerWalletService;

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PayoutResponse> requestWithdrawal(
            @Valid @RequestBody WithdrawalRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PayoutResponse data = sellerWalletService.requestWithdrawal(currentUserId, request);
        return ApiResponse.success(data, "Withdrawal request submitted successfully", null);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<SellerWalletTransactionResponse>> getWalletTransactions(
            @RequestParam(required = false) SellerWalletTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<SellerWalletTransactionResponse> data = sellerWalletService.getWalletTransactions(
                currentUserId, type, startDate, endDate, page, size, direction
        );
        return ApiResponse.success(data, "Wallet transactions retrieved successfully", null);
    }
}
