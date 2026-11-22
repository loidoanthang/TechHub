import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type { PaymentInfoResponse, VNPayTokenLinkResponse } from "@/types/paymentInfo";

export const paymentInfoApi = {
  getPaymentInfos: (): Promise<ApiResponse<PaymentInfoResponse[]>> => {
    return apiClient.get(ENDPOINTS.BUYER.PAYMENT_INFO);
  },

  initiateTokenLink: (): Promise<ApiResponse<VNPayTokenLinkResponse>> => {
    return apiClient.post(ENDPOINTS.BUYER.PAYMENT_INFO_LINK_INIT);
  },

  setDefaultPaymentInfo: (id: string): Promise<ApiResponse<PaymentInfoResponse>> => {
    return apiClient.patch(ENDPOINTS.BUYER.PAYMENT_INFO_DEFAULT(id));
  },

  deletePaymentInfo: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.BUYER.PAYMENT_INFO_DELETE(id));
  },
};
