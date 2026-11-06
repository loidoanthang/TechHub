"use client";

import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { checkoutApi } from "../api/checkoutApi";
import type { CheckoutPreviewRequest, CheckoutPreviewResponse } from "@/types/checkout";

export function useCheckoutPreview(params: CheckoutPreviewRequest, enabled = true) {
  const itemKey = [...params.selectedCartItemIds].sort().join(",");
  const shopVoucherKey = [...(params.shopVoucherIds ?? [])].sort().join(",");

  return useQuery({
    queryKey: [
      "checkout",
      "preview",
      itemKey,
      params.addressId ?? null,
      params.platformVoucherId ?? null,
      params.platformShippingVoucherId ?? null,
      shopVoucherKey,
    ],
    queryFn: async (): Promise<CheckoutPreviewResponse | null> => {
      if (!params.selectedCartItemIds || params.selectedCartItemIds.length === 0) {
        return null;
      }
      const res = await checkoutApi.previewCheckout(params);
      return res.data;
    },
    enabled: enabled && params.selectedCartItemIds.length > 0,
    placeholderData: keepPreviousData,
    staleTime: 1000 * 30, // 30 seconds
    retry: 1,
  });
}
