import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  CartPlatformVoucherResponse,
  PlatformVoucherQueryParams,
} from "@/types/voucher";

export const voucherApi = {
  getPlatformVouchers: (
    params?: PlatformVoucherQueryParams
  ): Promise<ApiResponse<CartPlatformVoucherResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.VOUCHERS.PLATFORM, {
      params,
    });
  },
};
