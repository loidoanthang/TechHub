package com.techhub.controller;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.CreateAddressRequest;
import com.techhub.model.dto.request.UpdateAddressRequest;
import com.techhub.model.dto.response.AddressResponse;
import com.techhub.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Validated
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressResponse>> getMyAddresses() {
        List<AddressResponse> data = addressService.getMyAddresses();
        return ApiResponse.success(data, "Addresses retrieved successfully.", null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        AddressResponse data = addressService.createAddress(request);
        return ApiResponse.success(data, "Address created successfully.", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request
    ) {
        AddressResponse data = addressService.updateAddress(id, request);
        return ApiResponse.success(data, "Address updated successfully.", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ApiResponse.success(null, "Address deleted successfully.", null);
    }

    @PatchMapping("/{id}/default")
    public ApiResponse<AddressResponse> setDefaultAddress(@PathVariable UUID id) {
        AddressResponse data = addressService.setDefaultAddress(id);
        return ApiResponse.success(data, "Default address set successfully.", null);
    }
}
