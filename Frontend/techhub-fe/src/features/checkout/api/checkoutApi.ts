import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  CheckoutPreviewRequest,
  CheckoutPreviewResponse,
  PlaceOrderRequest,
  PlaceOrderResponse,
  PaymentMethodResponse,
  BuyerPaymentStatusResponse,
  VNPayCallbackResponse,
} from "@/types/checkout";

export const checkoutApi = {
  previewCheckout: (
    data: CheckoutPreviewRequest
  ): Promise<ApiResponse<CheckoutPreviewResponse>> => {
    return apiClient.post(API_ENDPOINTS.CHECKOUT.PREVIEW, data);
  },

  placeOrder: (
    data: PlaceOrderRequest
  ): Promise<ApiResponse<PlaceOrderResponse>> => {
    return apiClient.post(API_ENDPOINTS.CHECKOUT.PLACE_ORDER, data);
  },

  getPaymentMethods: (): Promise<ApiResponse<PaymentMethodResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.BUYER.PAYMENT_METHODS);
  },

  getPaymentStatus: (
    paymentId: string
  ): Promise<ApiResponse<BuyerPaymentStatusResponse>> => {
    return apiClient.get(API_ENDPOINTS.BUYER.PAYMENT_STATUS(paymentId));
  },

  processVNPayCallback: (
    queryParams: Record<string, string>
  ): Promise<ApiResponse<VNPayCallbackResponse>> => {
    return apiClient.get(API_ENDPOINTS.PAYMENT.VNPAY_CALLBACK, {
      params: queryParams,
    });
  },

  retryVNPayPayment: (
    paymentId: string,
    bankCode?: string
  ): Promise<ApiResponse<{ paymentUrl: string }>> => {
    return apiClient.post(
      API_ENDPOINTS.PAYMENT.VNPAY_RETRY(paymentId),
      bankCode ? { bankCode } : {}
    );
  },
};
