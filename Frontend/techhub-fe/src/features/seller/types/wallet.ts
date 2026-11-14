export type SellerWalletTransactionType =
  | "ORDER_SETTLEMENT"
  | "WITHDRAWAL"
  | "WITHDRAWAL_REFUND";

export type DateFilterPreset = "ALL" | "TODAY" | "7DAYS" | "30DAYS" | "CUSTOM";

export type PayoutStatus = "PENDING" | "COMPLETED" | "REJECTED";

export interface SellerWalletTransactionResponse {
  id: string;
  type: SellerWalletTransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  referenceId?: string;
  description?: string;
  createdAt: string;
}

export interface PayoutResponse {
  id: string;
  sellerId?: string;
  shopName?: string;
  amount: number;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  status: PayoutStatus;
  rejectionReason?: string | null;
  remainingBalance: number;
  createdAt: string;
  processedAt?: string | null;
}

export interface WithdrawalRequest {
  amount: number;
}

export interface WalletTransactionQueryParams {
  type?: SellerWalletTransactionType;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  direction?: "ASC" | "DESC";
}

export interface SellerBankAccountSummary {
  bankName?: string;
  bankAccountNumber?: string;
  bankAccountHolder?: string;
}

export const WALLET_LIMITS = {
  MIN_WITHDRAWAL: 50_000,
  MAX_DAILY_WITHDRAWAL: 100_000_000,
} as const;
