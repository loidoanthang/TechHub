import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { paymentInfoApi } from "../api/paymentInfoApi";

export const PAYMENT_INFO_KEYS = {
  all: ["payment-info"] as const,
  list: () => [...PAYMENT_INFO_KEYS.all, "list"] as const,
};

export function usePaymentInfosQuery() {
  return useQuery({
    queryKey: PAYMENT_INFO_KEYS.list(),
    queryFn: async () => {
      const res = await paymentInfoApi.getPaymentInfos();
      return res.data ?? [];
    },
    staleTime: 5 * 1000,
    refetchOnMount: "always",
  });
}

export function useInitiateTokenLinkMutation() {
  return useMutation({
    mutationFn: () => paymentInfoApi.initiateTokenLink(),
  });
}

export function useSetDefaultPaymentInfoMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => paymentInfoApi.setDefaultPaymentInfo(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAYMENT_INFO_KEYS.all });
    },
  });
}

export function useDeletePaymentInfoMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => paymentInfoApi.deletePaymentInfo(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAYMENT_INFO_KEYS.all });
    },
  });
}
