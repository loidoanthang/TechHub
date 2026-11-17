"use client";

import React, { useState } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useSellerDashboardMetricsQuery } from "../hooks/useSellerDashboard";
import { DashboardPeriodFilter } from "./dashboard/DashboardPeriodFilter";
import { DashboardTodoList } from "./dashboard/DashboardTodoList";
import { DashboardStatsCards } from "./dashboard/DashboardStatsCards";
import { DashboardSalesOvertimeChart } from "./dashboard/DashboardSalesOvertimeChart";
import { DashboardTopProducts } from "./dashboard/DashboardTopProducts";
import { DashboardLatestOrdersTable } from "./dashboard/DashboardLatestOrdersTable";
import type { DashboardPeriod } from "@/types/sellerDashboard";

export function SellerDashboardView() {
  const { user } = useAuth();
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

  const sellerDisplayName = user
    ? `${user.lastName || ""} ${user.firstName || ""}`.trim() || user.email
    : "Đối Tác TechHub";

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto pb-10">
      {/* 1. Header Greeting & Period Selector */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] tracking-tight">
            Welcome back , {sellerDisplayName} !
          </h1>
          <p className="text-xs sm:text-sm text-[#737688] mt-0.5">
            Here&apos;s Your Current Sales Overview
          </p>
        </div>

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
      </div>

      {/* 2. Error Banner */}
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

      {/* 3. Mini Quick-Action To-Do Bar */}
      <DashboardTodoList
        breakdown={metrics?.orderStatusBreakdown}
        isLoading={isLoading}
      />

      {/* 4. 3 KPI Metric Bento Cards */}
      <DashboardStatsCards
        summary={metrics?.summary}
        isLoading={isLoading}
      />

      {/* 5. 2-Column Bento Grid: Sales Overtime Chart & Top Selling Product */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {/* Left Column: Sales Overtime Dual-line Chart */}
        <div className="lg:col-span-7 flex flex-col">
          <DashboardSalesOvertimeChart
            totalRevenue={metrics?.summary?.netRevenue}
            totalOrders={metrics?.summary?.totalOrders}
            isLoading={isLoading}
          />
        </div>

        {/* Right Column: Top Selling Product List with Stock Status */}
        <div className="lg:col-span-5 flex flex-col">
          <DashboardTopProducts
            products={metrics?.topSellingProducts}
            lowStockAlerts={metrics?.lowStockAlerts}
            isLoading={isLoading}
          />
        </div>
      </div>

      {/* 6. Latest Orders Table */}
      <DashboardLatestOrdersTable />
    </div>
  );
}
