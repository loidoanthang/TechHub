"use client";

import React, { useState, useMemo } from "react";
import {
  Wallet,
  RefreshCw,
} from "lucide-react";
import { BankWarningBanner } from "./BankWarningBanner";
import { WalletMetricsBento } from "./WalletMetricsBento";
import {
  TransactionFilterTabs,
  type DateFilterPreset,
} from "./TransactionFilterTabs";
import { TransactionTable } from "./TransactionTable";
import { WithdrawalModal } from "./WithdrawalModal";
import { useSellerProfileQuery } from "../../hooks/useSellerQueries";
import { useSellerDashboardMetricsQuery } from "../../hooks/useSellerDashboard";
import { useSellerWalletTransactionsQuery } from "../../hooks/useSellerWalletQueries";
import {
  isBankAccountIncomplete,
  getDateRangePreset,
} from "../../utils/walletUtils";
import { toast } from "@/lib/toast";
import {
  type SellerWalletTransactionType,
  type WalletTransactionQueryParams,
  type SellerBankAccountSummary,
} from "../../types/wallet";

export function SellerWalletView() {
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [selectedType, setSelectedType] = useState<
    SellerWalletTransactionType | undefined
  >(undefined);
  const [dateFilter, setDateFilter] = useState<DateFilterPreset>("ALL");
  const [customStartDate, setCustomStartDate] = useState<string | undefined>();
  const [customEndDate, setCustomEndDate] = useState<string | undefined>();
  const [page, setPage] = useState(0);
  const pageSize = 20;

  // 1. Fetch Seller Profile for bank account info
  const { data: profile, isLoading: isProfileLoading } =
    useSellerProfileQuery();

  // 2. Fetch Dashboard Metrics for wallet balance and pending payouts
  const {
    data: metrics,
    isLoading: isMetricsLoading,
    refetch: refetchMetrics,
  } = useSellerDashboardMetricsQuery();

  // Compute date range from preset using centralized wallet helper
  const dateRange = useMemo(
    () => getDateRangePreset(dateFilter, customStartDate, customEndDate),
    [dateFilter, customStartDate, customEndDate]
  );

  // 3. Query transactions
  const queryParams: WalletTransactionQueryParams = useMemo(
    () => ({
      type: selectedType,
      startDate: dateRange.startDate,
      endDate: dateRange.endDate,
      page,
      size: pageSize,
      direction: "DESC",
    }),
    [selectedType, dateRange, page]
  );

  const {
    data: txResponse,
    isLoading: isTxLoading,
    refetch: refetchTx,
    isFetching: isTxFetching,
  } = useSellerWalletTransactionsQuery(queryParams);

  const transactions = txResponse?.data?.content || [];
  const totalElements = txResponse?.data?.totalElements || 0;
  const totalPages = txResponse?.data?.totalPages || 0;

  const bankInfo: SellerBankAccountSummary = useMemo(
    () => ({
      bankName: profile?.bankName,
      bankAccountNumber: profile?.bankAccountNumber,
      bankAccountHolder: profile?.bankAccountHolder,
    }),
    [profile?.bankName, profile?.bankAccountNumber, profile?.bankAccountHolder]
  );

  const isBankIncomplete = isBankAccountIncomplete(bankInfo);

  const walletBalance = Number(metrics?.summary?.walletBalance || 0);
  const pendingPayoutAmount = Number(
    metrics?.summary?.pendingPayoutAmount || 0
  );
  const netRevenue = Number(metrics?.summary?.netRevenue || 0);

  const handleRefresh = () => {
    refetchMetrics();
    refetchTx();
  };

  const handleResetFilters = () => {
    setSelectedType(undefined);
    setDateFilter("ALL");
    setCustomStartDate(undefined);
    setCustomEndDate(undefined);
    setPage(0);
  };

  const handleSelectType = (type?: SellerWalletTransactionType) => {
    setSelectedType(type);
    setPage(0);
  };

  const handleSelectDateFilter = (preset: DateFilterPreset) => {
    setDateFilter(preset);
    setPage(0);
  };

  const handleChangeCustomDates = (start?: string, end?: string) => {
    if (start && end && start > end) {
      toast.error("Ngày bắt đầu không thể lớn hơn ngày kết thúc");
      return;
    }
    setCustomStartDate(start);
    setCustomEndDate(end);
    setPage(0);
  };

  return (
    <div className="w-full space-y-6 pb-12">
      {/* 1. Header with Title & Refresh Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold font-heading text-slate-900 tracking-tight flex items-center gap-2.5">
            <Wallet className="w-6 h-6 text-[#0045cc]" />
            <span>Ví &amp; Doanh thu Người Bán</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Theo dõi số dư khả dụng, quản lý rút tiền và đối soát giao dịch bán hàng
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={handleRefresh}
            disabled={isTxFetching}
            className="p-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 transition-colors cursor-pointer flex items-center gap-2 text-xs font-semibold"
            title="Làm mới dữ liệu"
          >
            <RefreshCw
              className={`w-4 h-4 ${isTxFetching ? "animate-spin text-[#0045cc]" : ""}`}
            />
            <span className="hidden sm:inline">Làm mới</span>
          </button>
        </div>
      </div>

      {/* 2. Bank Warning Banner */}
      {!isProfileLoading && (
        <BankWarningBanner isBankIncomplete={isBankIncomplete} />
      )}

      {/* 3. Bento KPI Cards (Chứa CTA Rút tiền chính) */}
      <WalletMetricsBento
        walletBalance={walletBalance}
        pendingPayoutAmount={pendingPayoutAmount}
        netRevenue={netRevenue}
        isBankIncomplete={isBankIncomplete}
        onOpenWithdrawModal={() => setIsWithdrawModalOpen(true)}
        isLoading={isMetricsLoading}
      />

      {/* 4. Transaction History Section */}
      <div className="space-y-4 pt-2">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold font-heading text-slate-900">
              Lịch sử biến động số dư
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Danh sách các khoản quyết toán đơn bán, lệnh rút tiền và hoàn tiền ví
            </p>
          </div>
        </div>

        <TransactionFilterTabs
          selectedType={selectedType}
          onSelectType={handleSelectType}
          dateFilter={dateFilter}
          onSelectDateFilter={handleSelectDateFilter}
          customStartDate={customStartDate}
          customEndDate={customEndDate}
          onChangeCustomDates={handleChangeCustomDates}
        />

        <TransactionTable
          transactions={transactions}
          isLoading={isTxLoading}
          page={page}
          size={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={setPage}
          onResetFilters={handleResetFilters}
        />
      </div>

      {/* 5. Withdrawal Modal */}
      <WithdrawalModal
        isOpen={isWithdrawModalOpen}
        onClose={() => setIsWithdrawModalOpen(false)}
        availableBalance={walletBalance}
        bankInfo={bankInfo}
        isBankIncomplete={isBankIncomplete}
      />
    </div>
  );
}
