import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { OrderStatus } from "@/types/enums";
import type {
  BuyerOrderResponse,
  BuyerOrderDetailResponse,
  CreateCancellationRequest,
  CancelOrderRequest,
  CancellationRequestResponse,
  CreateReturnRequest,
  ReturnRequestResponse,
} from "@/types/order";

export interface BuyerOrderQueryParams {
  status?: OrderStatus | string;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

export const orderApi = {
  getBuyerOrders: (
    params?: BuyerOrderQueryParams
  ): Promise<ApiResponse<PageResponse<BuyerOrderResponse>>> => {
    return apiClient.get(ENDPOINTS.BUYER.ORDERS, {
      params: {
        ...(params?.status && params.status !== "ALL" ? { status: params.status } : {}),
        ...(params?.keyword ? { keyword: params.keyword } : {}),
        page: params?.page ?? 0,
        size: params?.size ?? 10,
        sortBy: params?.sortBy ?? "createdAt",
        sortDir: params?.sortDir ?? "desc",
      },
    });
  },

  getBuyerOrderDetail: (
    orderId: string
  ): Promise<ApiResponse<BuyerOrderDetailResponse>> => {
    return apiClient.get(ENDPOINTS.BUYER.ORDER_DETAIL(orderId));
  },

  completeOrder: (
    orderId: string
  ): Promise<ApiResponse<BuyerOrderDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.BUYER.ORDER_COMPLETE(orderId));
  },

  cancelOrder: (
    orderId: string,
    data: CancelOrderRequest
  ): Promise<ApiResponse<BuyerOrderDetailResponse>> => {
    return apiClient.post(ENDPOINTS.BUYER.ORDER_CANCEL(orderId), data);
  },

  requestCancellation: (
    orderId: string,
    data: CreateCancellationRequest
  ): Promise<ApiResponse<CancellationRequestResponse>> => {
    return apiClient.post(
      ENDPOINTS.BUYER.ORDER_CANCELLATION_REQ(orderId),
      data
    );
  },

  createReturnRequest: (
    orderId: string,
    data: CreateReturnRequest
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.post(
      ENDPOINTS.BUYER.ORDER_RETURN_REQ(orderId),
      data
    );
  },

  getReturnRequest: (
    requestId: string
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.get(
      ENDPOINTS.BUYER.RETURN_REQUEST_DETAIL(requestId)
    );
  },

  getReturnRequestByOrderId: (
    orderId: string
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.get(
      `${ENDPOINTS.BUYER.ORDERS}/${orderId}/return-request`
    );
  },

  cancelReturnRequest: (
    requestId: string
  ): Promise<ApiResponse<ReturnRequestResponse>> => {
    return apiClient.post(
      ENDPOINTS.BUYER.RETURN_REQUEST_CANCEL(requestId)
    );
  },
};
