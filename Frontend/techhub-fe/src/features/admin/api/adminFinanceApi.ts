import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminPayoutResponse,
  AdminPayoutsQueryParams,
  AdminVoucherDetailResponse,
  AdminVoucherResponse,
  AdminVouchersQueryParams,
  BroadcastNotificationRequest,
  BroadcastNotificationResponse,
  CreatePlatformVoucherRequest,
  PlatformPolicyResponse,
  ProcessPayoutRequest,
  ShippingSimulationRequest,
  ShippingSimulationResponse,
  UpdatePlatformPolicyRequest,
  UpdatePlatformVoucherRequest,
  UpdatePlatformVoucherStatusRequest,
} from "@/types/adminFinance";

function cleanParams(params?: object): Record<string, unknown> {
  if (!params) return {};
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, v]) => v !== undefined && v !== null && v !== ""
    )
  );
}

export const adminFinanceApi = {
  // === VOUCHERS ===
  getVouchers: (
    params?: AdminVouchersQueryParams
  ): Promise<ApiResponse<PageResponse<AdminVoucherResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.VOUCHERS, {
      params: cleanParams(params),
    });
  },

  getVoucherDetail: (
    id: string
  ): Promise<ApiResponse<AdminVoucherDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.VOUCHER_DETAIL(id));
  },

  createPlatformVoucher: (
    data: CreatePlatformVoucherRequest
  ): Promise<ApiResponse<AdminVoucherResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.VOUCHERS, data);
  },

  updatePlatformVoucher: (
    id: string,
    data: UpdatePlatformVoucherRequest
  ): Promise<ApiResponse<AdminVoucherDetailResponse>> => {
    return apiClient.put(ENDPOINTS.ADMIN.VOUCHER_DETAIL(id), data);
  },

  updatePlatformVoucherStatus: (
    id: string,
    data: UpdatePlatformVoucherStatusRequest
  ): Promise<ApiResponse<AdminVoucherDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.VOUCHER_STATUS(id), data);
  },

  deletePlatformVoucher: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.ADMIN.VOUCHER_DETAIL(id));
  },

  // === PAYOUTS ===
  getPayouts: (
    params?: AdminPayoutsQueryParams
  ): Promise<ApiResponse<PageResponse<AdminPayoutResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.PAYOUTS, {
      params: cleanParams(params),
    });
  },

  processPayout: (
    id: string,
    data: ProcessPayoutRequest
  ): Promise<ApiResponse<AdminPayoutResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.PAYOUT_PROCESS(id), data);
  },

  // === POLICIES ===
  getPolicies: (): Promise<ApiResponse<PlatformPolicyResponse[]>> => {
    return apiClient.get(ENDPOINTS.ADMIN.POLICIES);
  },

  updatePolicy: (
    data: UpdatePlatformPolicyRequest
  ): Promise<ApiResponse<PlatformPolicyResponse>> => {
    return apiClient.put(ENDPOINTS.ADMIN.POLICIES, data);
  },

  // === NOTIFICATIONS BROADCAST ===
  broadcastNotification: (
    data: BroadcastNotificationRequest
  ): Promise<ApiResponse<BroadcastNotificationResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.NOTIFICATIONS_BROADCAST, data);
  },

  // === SHIPPING SIMULATOR ===
  simulateShippingAdvance: (
    data?: ShippingSimulationRequest
  ): Promise<ApiResponse<ShippingSimulationResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.SHIPPING_SIMULATE, data ?? {});
  },
};
