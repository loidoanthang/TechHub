import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  SellerOrderResponse,
  SellerOrderDetailResponse,
  UpdateSellerOrderStatusRequest,
  CancelOrderRequest,
  CancellationRequestResponse,
  ProcessCancellationRequest,
  ReturnRequestResponse,
  ProcessReturnRequest,
  SellerOrderFilterParams,
} from "@/types/sellerOrders";

export const sellerOrderApi = {
  getOrders: (
    params?: SellerOrderFilterParams
  ): Promise<ApiResponse<PageResponse<SellerOrderResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.ORDERS, { params });
  },

  getOrderDetail: (
    orderId: string
  ): Promise<ApiResponse<SellerOrderDetailResponse>> => {
    return apiClient.get(ENDPOINTS.SELLER.ORDER_DETAIL(orderId));
  },

  updateOrderStatus: (
    orderId: string,
    data: UpdateSellerOrderStatusRequest
  ): Promise<ApiResponse<SellerOrderDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.SELLER.ORDER_STATUS(orderId), data);
  },

  cancelOrder: (
    orderId: string,
    data: CancelOrderRequest
  ): Promise<ApiResponse<SellerOrderDetailResponse>> => {
    return apiClient.post(ENDPOINTS.SELLER.ORDER_CANCEL(orderId), data);
  },

  getCancellationRequests: (
    page = 0,
    size = 10
  ): Promise<ApiResponse<PageResponse<CancellationRequestResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.CANCELLATION_REQUESTS, {
      params: { page, size },
    });
  },

  processCancellationRequest: (
    requestId: string,
    data: ProcessCancellationRequest
  ): Promise<ApiResponse<CancellationRequestResponse>> => {
    return apiClient.patch(
      ENDPOINTS.SELLER.PROCESS_CANCELLATION(requestId),
      data
    );
  },

  getReturnRequests: (
    page = 0,
    size = 10,
    status?: string
  ): Promise<ApiResponse<PageResponse<ReturnRequestResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.RETURN_REQUESTS, {
      params: { page, size, status },
    });
  },

  getReturnRequestDetail: (
    requestId: string
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.get(ENDPOINTS.SELLER.RETURN_REQUEST_DETAIL(requestId));
  },

  processReturnRequest: (
    requestId: string,
    data: ProcessReturnRequest
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.patch(ENDPOINTS.SELLER.PROCESS_RETURN(requestId), data);
  },
};
