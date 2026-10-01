package com.techhub.service.seller;

import com.techhub.model.dto.request.RegisterSellerRequest;
import com.techhub.model.dto.request.UpdateSellerProfileRequest;
import com.techhub.model.dto.request.UpdateSellerShopRequest;
import com.techhub.model.dto.request.UpdateShopStatusRequest;
import com.techhub.model.dto.request.UpdateShippingOptionRequest;
import com.techhub.model.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.model.dto.response.SellerProfileResponse;
import com.techhub.model.dto.response.SellerRegistrationResponse;
import com.techhub.model.dto.response.SellerShippingOptionResponse;
import com.techhub.model.dto.response.SellerShopResponse;
import com.techhub.model.dto.response.SellerVerificationStatusResponse;
import com.techhub.model.dto.response.SellerWarehouseAddressResponse;

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


