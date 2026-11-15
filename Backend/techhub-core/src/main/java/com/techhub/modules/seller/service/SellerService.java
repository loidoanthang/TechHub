package com.techhub.modules.seller.service;

import com.techhub.modules.seller.dto.request.RegisterSellerRequest;
import com.techhub.modules.seller.dto.request.UpdateSellerProfileRequest;
import com.techhub.modules.seller.dto.request.UpdateSellerShopRequest;
import com.techhub.modules.seller.dto.request.UpdateShopStatusRequest;
import com.techhub.modules.seller.dto.request.UpdateShippingOptionRequest;
import com.techhub.modules.user.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.modules.seller.dto.response.SellerProfileResponse;
import com.techhub.modules.seller.dto.response.SellerRegistrationResponse;
import com.techhub.modules.seller.dto.response.SellerShippingOptionResponse;
import com.techhub.modules.seller.dto.response.SellerShopResponse;
import com.techhub.modules.seller.dto.response.SellerVerificationStatusResponse;
import com.techhub.modules.seller.dto.response.SellerWarehouseAddressResponse;

public interface SellerService {

    SellerRegistrationResponse registerSeller(RegisterSellerRequest request);

    SellerVerificationStatusResponse getVerificationStatus();

    SellerProfileResponse getSellerProfile();

    SellerProfileResponse updateSellerProfile(UpdateSellerProfileRequest request);

    SellerShopResponse getShopInfo();

    SellerShopResponse updateShopInfo(UpdateSellerShopRequest request);

    SellerShopResponse updateShopStatus(UpdateShopStatusRequest request);

    SellerWarehouseAddressResponse getWarehouseAddress();

    SellerWarehouseAddressResponse updateWarehouseAddress(UpdateWarehouseAddressRequest request);

    SellerShippingOptionResponse getShippingOption();

    SellerShippingOptionResponse updateShippingOption(UpdateShippingOptionRequest request);
}


