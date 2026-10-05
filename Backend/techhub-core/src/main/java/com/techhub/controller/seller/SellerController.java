package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.RegisterSellerRequest;
import com.techhub.model.dto.request.UpdateSellerProfileRequest;
import com.techhub.model.dto.request.UpdateSellerShopRequest;
import com.techhub.model.dto.request.UpdateShippingOptionRequest;
import com.techhub.model.dto.request.UpdateShopStatusRequest;
import com.techhub.model.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.model.dto.response.SellerProfileResponse;
import com.techhub.model.dto.response.SellerRegistrationResponse;
import com.techhub.model.dto.response.SellerShippingOptionResponse;
import com.techhub.model.dto.response.SellerShopResponse;
import com.techhub.model.dto.response.SellerVerificationStatusResponse;
import com.techhub.model.dto.response.SellerWarehouseAddressResponse;
import com.techhub.service.seller.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@Validated
public class SellerController {

    private final SellerService sellerService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SellerRegistrationResponse> registerSeller(
            @Valid @RequestBody RegisterSellerRequest request
    ) {
        SellerRegistrationResponse data = sellerService.registerSeller(request);
        String message = data.isResubmitted()
                ? "Seller application re-submitted successfully. Status reset to PENDING."
                : "Seller registration submitted successfully. Please wait for administrator approval.";
        return ApiResponse.success(data, message, null);
    }

    @GetMapping("/verification-status")
    public ApiResponse<SellerVerificationStatusResponse> getVerificationStatus() {
        SellerVerificationStatusResponse data = sellerService.getVerificationStatus();
        String message = resolveVerificationStatusMessage(data);
        return ApiResponse.success(data, message, null);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerProfileResponse> getSellerProfile() {
        SellerProfileResponse data = sellerService.getSellerProfile();
        return ApiResponse.success(data, "Seller profile retrieved successfully", null);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerProfileResponse> updateSellerProfile(
            @Valid @RequestBody UpdateSellerProfileRequest request
    ) {
        SellerProfileResponse data = sellerService.updateSellerProfile(request);
        return ApiResponse.success(data, "Seller profile updated successfully", null);
    }

    @GetMapping("/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerShopResponse> getShopInfo() {
        SellerShopResponse data = sellerService.getShopInfo();
        return ApiResponse.success(data, "Shop details retrieved successfully", null);
    }

    @PutMapping("/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerShopResponse> updateShopInfo(
            @Valid @RequestBody UpdateSellerShopRequest request
    ) {
        SellerShopResponse data = sellerService.updateShopInfo(request);
        return ApiResponse.success(data, "Shop details updated successfully", null);
    }

    @PatchMapping("/shop/status")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerShopResponse> updateShopStatus(
            @Valid @RequestBody UpdateShopStatusRequest request
    ) {
        SellerShopResponse data = sellerService.updateShopStatus(request);
        return ApiResponse.success(data, "Shop status updated successfully", null);
    }

    @GetMapping("/shop/address")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerWarehouseAddressResponse> getWarehouseAddress() {
        SellerWarehouseAddressResponse data = sellerService.getWarehouseAddress();
        return ApiResponse.success(data, "Warehouse address retrieved successfully", null);
    }

    @PutMapping("/shop/address")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerWarehouseAddressResponse> updateWarehouseAddress(
            @Valid @RequestBody UpdateWarehouseAddressRequest request
    ) {
        SellerWarehouseAddressResponse data = sellerService.updateWarehouseAddress(request);
        return ApiResponse.success(data, "Warehouse address updated successfully", null);
    }

    @GetMapping("/shop/shipping-option")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerShippingOptionResponse> getShippingOption() {
        SellerShippingOptionResponse data = sellerService.getShippingOption();
        return ApiResponse.success(data, "Shipping option retrieved successfully", null);
    }

    @PutMapping("/shop/shipping-option")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerShippingOptionResponse> updateShippingOption(
            @Valid @RequestBody UpdateShippingOptionRequest request
    ) {
        SellerShippingOptionResponse data = sellerService.updateShippingOption(request);
        return ApiResponse.success(data, "Shipping fee updated successfully", null);
    }

    private String resolveVerificationStatusMessage(SellerVerificationStatusResponse data) {
        if (!data.hasApplied() || data.verificationStatus() == null) {
            return "User has not submitted a seller registration";
        }
        return switch (data.verificationStatus()) {
            case PENDING -> "Seller application is pending review";
            case REJECTED -> "Seller application was rejected";
            case APPROVED -> "Seller application is approved";
        };
    }
}
