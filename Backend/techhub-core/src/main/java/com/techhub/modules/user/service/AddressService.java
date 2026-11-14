package com.techhub.modules.user.service;

import com.techhub.modules.user.dto.request.CreateAddressRequest;
import com.techhub.modules.user.dto.request.UpdateAddressRequest;
import com.techhub.modules.user.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse createAddress(CreateAddressRequest request);

    List<AddressResponse> getMyAddresses();

    AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request);

    void deleteAddress(UUID addressId);

    AddressResponse setDefaultAddress(UUID addressId);
}
