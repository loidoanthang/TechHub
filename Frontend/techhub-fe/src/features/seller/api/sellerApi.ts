import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type { ShopStatus } from "@/types/enums";
import type {
  RegisterSellerRequest,
  SellerRegistrationResponse,
  SellerVerificationStatusResponse,
  SellerShopResponse,
  UpdateSellerShopRequest,
} from "@/types/seller";

export interface UpdateWarehouseAddressRequest {
  contactName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
}

export interface SellerWarehouseAddressResponse {
  addressId: string;
  contactName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
}

export interface UpdateShippingOptionRequest {
  standardShippingFee: number;
  expressShippingAvailable?: boolean;
}

export interface SellerShippingOptionResponse {
  standardShippingFee: number;
  expressShippingAvailable: boolean;
}

export const sellerApi = {
  registerSeller: (
    data: RegisterSellerRequest
  ): Promise<ApiResponse<SellerRegistrationResponse>> => {
    return apiClient.post(ENDPOINTS.SELLER.REGISTER, data);
  },

  getVerificationStatus: (): Promise<
    ApiResponse<SellerVerificationStatusResponse>
  > => {
    return apiClient.get(
      ENDPOINTS.SELLER.VERIFICATION_STATUS
    );
  },

  getShopInfo: (): Promise<ApiResponse<SellerShopResponse>> => {
    return apiClient.get(ENDPOINTS.SELLER.SHOP);
  },

  updateShopInfo: (
    data: UpdateSellerShopRequest
  ): Promise<ApiResponse<SellerShopResponse>> => {
    return apiClient.put(ENDPOINTS.SELLER.SHOP, data);
  },

  updateShopStatus: (
    status: ShopStatus
  ): Promise<ApiResponse<SellerShopResponse>> => {
    return apiClient.patch(ENDPOINTS.SELLER.SHOP_STATUS, { status });
  },

  getWarehouseAddress: (): Promise<
    ApiResponse<SellerWarehouseAddressResponse>
  > => {
    return apiClient.get(
      ENDPOINTS.SELLER.WAREHOUSE_ADDRESS
    );
  },

  updateWarehouseAddress: (
    data: UpdateWarehouseAddressRequest
  ): Promise<ApiResponse<SellerWarehouseAddressResponse>> => {
    return apiClient.put(
      ENDPOINTS.SELLER.WAREHOUSE_ADDRESS,
      data
    );
  },

  getShippingOption: (): Promise<
    ApiResponse<SellerShippingOptionResponse>
  > => {
    return apiClient.get(
      ENDPOINTS.SELLER.SHIPPING_OPTION
    );
  },

  updateShippingOption: (
    data: UpdateShippingOptionRequest
  ): Promise<ApiResponse<SellerShippingOptionResponse>> => {
    return apiClient.put(
      ENDPOINTS.SELLER.SHIPPING_OPTION,
      data
    );
  },
};
