import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  SellerWalletTransactionResponse,
  PayoutResponse,
  WithdrawalRequest,
  WalletTransactionQueryParams,
} from "../types/wallet";

export const sellerWalletApi = {
  getWalletTransactions: (
    params?: WalletTransactionQueryParams
  ): Promise<ApiResponse<PageResponse<SellerWalletTransactionResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.WALLET_TRANSACTIONS, { params });
  },

  requestWithdrawal: (
    data: WithdrawalRequest
  ): Promise<ApiResponse<PayoutResponse>> => {
    return apiClient.post(ENDPOINTS.SELLER.WALLET_WITHDRAW, data);
  },
};
