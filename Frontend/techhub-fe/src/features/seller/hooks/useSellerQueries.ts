import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  sellerApi,
  type UpdateWarehouseAddressRequest,
  type UpdateShippingOptionRequest,
  type UpdateSellerProfileRequest,
} from "../api/sellerApi";
import type { RegisterSellerRequest, UpdateSellerShopRequest } from "@/types/seller";
import type { ShopStatus } from "@/types/enums";

export const SELLER_KEYS = {
  verificationStatus: ["seller", "verification-status"] as const,
  shop: ["seller", "shop"] as const,
  warehouseAddress: ["seller", "warehouse-address"] as const,
  shippingOption: ["seller", "shipping-option"] as const,
  profile: ["seller", "profile"] as const,
};

export function useSellerVerificationStatusQuery() {
  return useQuery({
    queryKey: SELLER_KEYS.verificationStatus,
    queryFn: async () => {
      const res = await sellerApi.getVerificationStatus();
      return res.data;
    },
  });
}

export function useSellerShopQuery() {
  return useQuery({
    queryKey: SELLER_KEYS.shop,
    queryFn: async () => {
      const res = await sellerApi.getShopInfo();
      return res.data;
    },
  });
}

export function useRegisterSellerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: RegisterSellerRequest) => sellerApi.registerSeller(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.verificationStatus });
    },
  });
}

export function useUpdateSellerShopMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateSellerShopRequest) => sellerApi.updateShopInfo(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.shop });
    },
  });
}

export function useUpdateShopStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (status: ShopStatus) => sellerApi.updateShopStatus(status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.shop });
    },
  });
}

export function useWarehouseAddressQuery() {
  return useQuery({
    queryKey: SELLER_KEYS.warehouseAddress,
    queryFn: async () => {
      const res = await sellerApi.getWarehouseAddress();
      return res.data;
    },
  });
}

export function useUpdateWarehouseAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateWarehouseAddressRequest) =>
      sellerApi.updateWarehouseAddress(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.warehouseAddress });
    },
  });
}

export function useShippingOptionQuery() {
  return useQuery({
    queryKey: SELLER_KEYS.shippingOption,
    queryFn: async () => {
      const res = await sellerApi.getShippingOption();
      return res.data;
    },
  });
}

export function useUpdateShippingOptionMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateShippingOptionRequest) =>
      sellerApi.updateShippingOption(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.shippingOption });
    },
  });
}

export function useSellerProfileQuery() {
  return useQuery({
    queryKey: SELLER_KEYS.profile,
    queryFn: async () => {
      const res = await sellerApi.getSellerProfile();
      return res.data;
    },
  });
}

export function useUpdateSellerProfileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateSellerProfileRequest) =>
      sellerApi.updateSellerProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_KEYS.profile });
    },
  });
}
