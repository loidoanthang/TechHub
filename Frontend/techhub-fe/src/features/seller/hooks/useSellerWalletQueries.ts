import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sellerWalletApi } from "../api/sellerWalletApi";
import { SELLER_KEYS } from "./useSellerQueries";
import { SELLER_DASHBOARD_KEYS } from "./useSellerDashboard";
import type {
  WalletTransactionQueryParams,
  WithdrawalRequest,
} from "../types/wallet";

export const sellerWalletKeys = {
  all: ["seller", "wallet"] as const,
  transactions: (params?: WalletTransactionQueryParams) =>
    [...sellerWalletKeys.all, "transactions", params] as const,
};

export function useSellerWalletTransactionsQuery(
  params?: WalletTransactionQueryParams,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: sellerWalletKeys.transactions(params),
    queryFn: () => sellerWalletApi.getWalletTransactions(params),
    enabled: options?.enabled ?? true,
  });
}

export function useRequestWithdrawalMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: WithdrawalRequest) =>
      sellerWalletApi.requestWithdrawal(data),
    onSuccess: () => {
      // Invalidate wallet transactions, dashboard metrics, and seller profile
      queryClient.invalidateQueries({
        queryKey: sellerWalletKeys.all,
      });
      queryClient.invalidateQueries({
        queryKey: SELLER_DASHBOARD_KEYS.metrics(),
      });
      queryClient.invalidateQueries({
        queryKey: SELLER_KEYS.profile,
      });
    },
  });
}
