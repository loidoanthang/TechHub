import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sellerOrderApi } from "../api/sellerOrderApi";
import { SELLER_DASHBOARD_KEYS } from "./useSellerDashboard";
import type {
  SellerOrderFilterParams,
  UpdateSellerOrderStatusRequest,
  CancelOrderRequest,
  ProcessCancellationRequest,
  ProcessReturnRequest,
} from "@/types/sellerOrders";

export const SELLER_ORDER_KEYS = {
  all: ["seller", "orders"] as const,
  list: (params?: SellerOrderFilterParams) =>
    ["seller", "orders", "list", params] as const,
  detail: (id: string) => ["seller", "orders", "detail", id] as const,
  cancellations: (page: number, size: number) =>
    ["seller", "orders", "cancellations", page, size] as const,
  returns: (page: number, size: number, status?: string) =>
    ["seller", "orders", "returns", page, size, status] as const,
};

export function useSellerOrdersQuery(params?: SellerOrderFilterParams) {
  return useQuery({
    queryKey: SELLER_ORDER_KEYS.list(params),
    queryFn: async () => {
      const res = await sellerOrderApi.getOrders(params);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useSellerOrderDetailQuery(orderId?: string) {
  return useQuery({
    queryKey: SELLER_ORDER_KEYS.detail(orderId || ""),
    queryFn: async () => {
      if (!orderId) throw new Error("Order ID is required");
      const res = await sellerOrderApi.getOrderDetail(orderId);
      return res.data;
    },
    enabled: !!orderId,
  });
}

export function useUpdateSellerOrderStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      data,
    }: {
      orderId: string;
      data: UpdateSellerOrderStatusRequest;
    }) => sellerOrderApi.updateOrderStatus(orderId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: SELLER_ORDER_KEYS.all });
      queryClient.invalidateQueries({
        queryKey: SELLER_ORDER_KEYS.detail(variables.orderId),
      });
      queryClient.invalidateQueries({ queryKey: ["seller", "dashboard"] });
    },
  });
}

export function useSellerCancelOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      data,
    }: {
      orderId: string;
      data: CancelOrderRequest;
    }) => sellerOrderApi.cancelOrder(orderId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: SELLER_ORDER_KEYS.all });
      queryClient.invalidateQueries({
        queryKey: SELLER_ORDER_KEYS.detail(variables.orderId),
      });
      queryClient.invalidateQueries({ queryKey: ["seller", "dashboard"] });
    },
  });
}

export function useSellerCancellationRequestsQuery(page = 0, size = 10) {
  return useQuery({
    queryKey: SELLER_ORDER_KEYS.cancellations(page, size),
    queryFn: async () => {
      const res = await sellerOrderApi.getCancellationRequests(page, size);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useProcessCancellationRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      requestId,
      data,
    }: {
      requestId: string;
      data: ProcessCancellationRequest;
    }) => sellerOrderApi.processCancellationRequest(requestId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ["seller", "dashboard"] });
    },
  });
}

export function useSellerReturnRequestsQuery(page = 0, size = 10, status?: string) {
  return useQuery({
    queryKey: SELLER_ORDER_KEYS.returns(page, size, status),
    queryFn: async () => {
      const res = await sellerOrderApi.getReturnRequests(page, size, status);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useProcessReturnRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      requestId,
      data,
    }: {
      requestId: string;
      data: ProcessReturnRequest;
    }) => sellerOrderApi.processReturnRequest(requestId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ["seller", "dashboard"] });
    },
  });
}
