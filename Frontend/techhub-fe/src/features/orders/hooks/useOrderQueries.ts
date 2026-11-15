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
  returnStatuses: (ids: string) => ["orders", "return-statuses", ids] as const,
};

// Probe return outcomes for completed cards (list API has no return data)
export function useReturnRequestStatusesQuery(ids: string[]) {
  return useQuery({
    queryKey: ORDER_KEYS.returnStatuses(ids.join(",")),
    queryFn: async () => {
      const entries = await Promise.all(
        ids.map(async (id) => {
          try {
            const res = await orderApi.getReturnRequestByOrderId(id);
            return [id, (res.data?.status as string) ?? null] as const;
          } catch {
            return [id, null] as const;
          }
        })
      );
      return Object.fromEntries(entries) as Record<string, string | null>;
    },
    enabled: ids.length > 0,
    retry: false,
  });
}

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

export function useReturnRequestQuery(orderId: string) {
  return useQuery({
    queryKey: ORDER_KEYS.returnRequest(orderId),
    queryFn: async () => {
      try {
        const res = await orderApi.getReturnRequestByOrderId(orderId);
        return res.data ?? null;
      } catch {
        // 404 = no return request for this order
        return null;
      }
    },
    enabled: Boolean(orderId),
    retry: false,
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
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.returnRequest(orderId) });
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
