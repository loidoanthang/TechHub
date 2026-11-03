import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type { UserProfile, UpdateProfileRequest } from "@/types/user";
import type { AddressResponse, CreateAddressRequest, UpdateAddressRequest } from "@/types/address";

export const accountApi = {
  getProfile: (): Promise<ApiResponse<UserProfile>> => {
    return apiClient.get(API_ENDPOINTS.USER.PROFILE);
  },

  updateProfile: (data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> => {
    return apiClient.put(API_ENDPOINTS.USER.PROFILE, data);
  },

  getAddresses: (): Promise<ApiResponse<AddressResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.ADDRESSES.BASE);
  },

  createAddress: (data: CreateAddressRequest): Promise<ApiResponse<AddressResponse>> => {
    return apiClient.post(API_ENDPOINTS.ADDRESSES.BASE, data);
  },

  updateAddress: (id: string, data: UpdateAddressRequest): Promise<ApiResponse<AddressResponse>> => {
    return apiClient.put(API_ENDPOINTS.ADDRESSES.DETAIL(id), data);
  },

  deleteAddress: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(API_ENDPOINTS.ADDRESSES.DETAIL(id));
  },

  setDefaultAddress: (id: string): Promise<ApiResponse<AddressResponse>> => {
    return apiClient.patch(API_ENDPOINTS.ADDRESSES.DEFAULT(id));
  },
};
