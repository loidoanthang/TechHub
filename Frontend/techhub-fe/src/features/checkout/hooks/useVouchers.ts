import { useQuery } from "@tanstack/react-query";
import { voucherApi } from "../api/voucherApi";
import type { PlatformVoucherQueryParams } from "@/types/voucher";

export const VOUCHER_KEYS = {
  platform: (params?: PlatformVoucherQueryParams) =>
    ["vouchers", "platform", params?.orderAmount, params?.shippingFee, params?.appliesTo] as const,
};

export function usePlatformVouchersQuery(params?: PlatformVoucherQueryParams) {
  return useQuery({
    queryKey: VOUCHER_KEYS.platform(params),
    queryFn: async () => {
      const res = await voucherApi.getPlatformVouchers(params);
      return res.data ?? [];
    },
    staleTime: 30_000,
  });
}
