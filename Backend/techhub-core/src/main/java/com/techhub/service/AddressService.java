package com.techhub.service;

import com.techhub.model.dto.request.CreateAddressRequest;
import com.techhub.model.dto.request.UpdateAddressRequest;
import com.techhub.model.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse createAddress(CreateAddressRequest request);

    List<AddressResponse> getMyAddresses();

    AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request);

    void deleteAddress(UUID addressId);

    AddressResponse setDefaultAddress(UUID addressId);
}
