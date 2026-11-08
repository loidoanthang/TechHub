"use client";

import React, { useState } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { useSellerDashboardMetricsQuery } from "../hooks/useSellerDashboard";
import { DashboardPeriodFilter } from "./dashboard/DashboardPeriodFilter";
import { DashboardTodoList } from "./dashboard/DashboardTodoList";
import { DashboardStatsCards } from "./dashboard/DashboardStatsCards";
import { DashboardWalletCard } from "./dashboard/DashboardWalletCard";
import { DashboardTopProducts } from "./dashboard/DashboardTopProducts";
import { DashboardLowStockAlerts } from "./dashboard/DashboardLowStockAlerts";
import { DashboardHighlightsNews } from "./dashboard/DashboardHighlightsNews";
import type { DashboardPeriod } from "@/types/sellerDashboard";

export function SellerDashboardView() {
  const [period, setPeriod] = useState<DashboardPeriod>("TODAY");
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [lastUpdated, setLastUpdated] = useState<string>(() =>
    new Date().toLocaleTimeString("vi-VN")
  );

  const queryParams =
    period === "CUSTOM"
      ? { period, startDate: startDate || undefined, endDate: endDate || undefined }
      : { period };

  const {
    data: metrics,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useSellerDashboardMetricsQuery(queryParams);

  const handleRefresh = async () => {
    await refetch();
    setLastUpdated(new Date().toLocaleTimeString("vi-VN"));
  };

  const handlePeriodChange = (newPeriod: DashboardPeriod) => {
    setPeriod(newPeriod);
    setLastUpdated(new Date().toLocaleTimeString("vi-VN"));
  };

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto">
      {/* 1. Header & Period Filter */}
      <div className="flex flex-col gap-1.5">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] tracking-tight">
          Tổng Quan Kênh Bán Hàng
        </h1>
        <p className="text-xs text-[#737688]">
          Theo dõi doanh số thời gian thực, quản lý đơn hàng cần đóng gói và tình trạng kho linh kiện.
        </p>
      </div>

      {/* 2. Period Filter Component */}
      <DashboardPeriodFilter
        period={period}
        onPeriodChange={handlePeriodChange}
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
        onRefresh={handleRefresh}
        isFetching={isFetching}
        lastUpdated={lastUpdated}
      />

      {/* 3. Error Banner (if query fails) */}
      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-center justify-between gap-3 animate-in fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span>
              Không thể tải chỉ số kinh doanh từ máy chủ: {(error as Error).message}
            </span>
          </div>
          <button
            type="button"
            onClick={handleRefresh}
            className="px-3 py-1.5 rounded-xl bg-red-600 text-white font-bold hover:bg-red-700 transition-colors shrink-0 flex items-center gap-1.5"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Thử lại</span>
          </button>
        </div>
      )}

      {/* 4. Top Row: To-Do List */}
      <DashboardTodoList
        breakdown={metrics?.orderStatusBreakdown}
        isLoading={isLoading}
      />

      {/* 5. Business Analytics: 4 Bento Stats Cards */}
      <DashboardStatsCards
        summary={metrics?.summary}
        isLoading={isLoading}
      />

      {/* 6. Main 2-Column Bento Grid (8 cols / 4 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (8 cols): Top Products & Future Analytics */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <DashboardTopProducts
            products={metrics?.topSellingProducts}
            isLoading={isLoading}
          />
        </div>

        {/* Right Column (4 cols): Wallet, Low Stock Alerts, Highlights */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          <DashboardWalletCard
            walletBalance={metrics?.summary?.walletBalance}
            pendingPayoutAmount={metrics?.summary?.pendingPayoutAmount}
            isLoading={isLoading}
          />

          <DashboardLowStockAlerts
            alerts={metrics?.lowStockAlerts}
            isLoading={isLoading}
          />

          <DashboardHighlightsNews />
        </div>
      </div>
    </div>
  );
}
