import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { orderApi, type BuyerOrderQueryParams } from "../api/orderApi";
import type {
  CreateCancellationRequest,
  CancelOrderRequest,
  CreateReturnRequest,
} from "@/types/order";

export const ORDER_KEYS = {
  all: ["orders"] as const,
  list: (params?: BuyerOrderQueryParams) => ["orders", "list", params] as const,
  detail: (id: string) => ["orders", "detail", id] as const,
  returnRequest: (orderId: string) => ["orders", "return-request", orderId] as const,
};

export function useBuyerOrdersQuery(params?: BuyerOrderQueryParams) {
  return useQuery({
    queryKey: ORDER_KEYS.list(params),
    queryFn: async () => {
      const res = await orderApi.getBuyerOrders(params);
      return res.data;
    },
  });
}

export function useBuyerOrderDetailQuery(orderId: string) {
  return useQuery({
    queryKey: ORDER_KEYS.detail(orderId),
    queryFn: async () => {
      const res = await orderApi.getBuyerOrderDetail(orderId);
      return res.data;
    },
    enabled: Boolean(orderId),
  });
}

export function useCompleteOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => orderApi.completeOrder(orderId),
    onSuccess: (_, orderId) => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.detail(orderId) });
    },
  });
}

export function useCancelOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, data }: { orderId: string; data: CancelOrderRequest }) =>
      orderApi.cancelOrder(orderId, data),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.detail(orderId) });
    },
  });
}

export function useRequestCancellationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      data,
    }: {
      orderId: string;
      data: CreateCancellationRequest;
    }) => orderApi.requestCancellation(orderId, data),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.detail(orderId) });
    },
  });
}

export function useCreateReturnRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      data,
    }: {
      orderId: string;
      data: CreateReturnRequest;
    }) => orderApi.createReturnRequest(orderId, data),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.detail(orderId) });
    },
  });
}

export function useCancelReturnRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (requestId: string) => orderApi.cancelReturnRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}
