"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  TrendingUp,
  Coins,
  ShoppingBag,
  Wallet,
  PackageCheck,
  Store,
  Users,
  ArrowRight,
  Loader2,
  AlertCircle,
  RefreshCw,
  Award,
  Activity,
} from "lucide-react";
import { useAdminDashboardMetricsQuery } from "../../hooks/useAdminDashboard";
import {
  useAdminProductsQuery,
  useAdminSellersQuery,
} from "../../hooks/useAdminModeration";
import { ROUTES } from "@/lib/constants";
import type { DashboardPeriod } from "@/types/enums";

const PERIOD_OPTIONS: { value: DashboardPeriod; label: string }[] = [
  { value: "TODAY", label: "Hôm nay" },
  { value: "LAST_7_DAYS", label: "7 ngày qua" },
  { value: "THIS_MONTH", label: "Tháng này" },
  { value: "CUSTOM", label: "Tùy chọn" },
];

const VND_CURRENCY_FORMATTER = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
});

const formatVND = (val: number | undefined | null) =>
  VND_CURRENCY_FORMATTER.format(val || 0);

export function AdminDashboardView() {
  const [period, setPeriod] = useState<DashboardPeriod>("THIS_MONTH");
  const [customStart, setCustomStart] = useState("");
  const [customEnd, setCustomEnd] = useState("");

  const isCustomValid =
    period !== "CUSTOM" || (Boolean(customStart) && Boolean(customEnd));

  const {
    data: metrics,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useAdminDashboardMetricsQuery(
    period === "CUSTOM"
      ? { period, startDate: customStart, endDate: customEnd }
      : { period },
    isCustomValid
  );

  // Quick moderation counters
  const { data: pendingProductsData } = useAdminProductsQuery({
    approvalStatus: "PENDING_APPROVAL",
    page: 0,
    size: 1,
  });
  const { data: pendingSellersData } = useAdminSellersQuery({
    status: "PENDING",
    page: 0,
    size: 1,
  });

  const pendingProductsCount = pendingProductsData?.totalElements ?? 0;
  const pendingSellersCount = pendingSellersData?.totalElements ?? 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-5 max-w-[1440px] mx-auto">
      {/* 1. HEADER & PERIOD FILTER */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
        <div>
          <h1 className="font-heading text-lg sm:text-xl font-bold text-[#0b1c30]">
            Tổng quan Điều hành Sàn TechHub
          </h1>
          <p className="text-[11px] text-slate-500 mt-0.5">
            Theo dõi GMV, doanh thu hoa hồng, tăng trưởng gian hàng và các tác vụ cần kiểm duyệt.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="bg-white border border-slate-200 rounded-xl p-1 flex items-center gap-1 overflow-x-auto no-scrollbar">
            {PERIOD_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => setPeriod(opt.value)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors duration-150 whitespace-nowrap cursor-pointer ${
                  period === opt.value
                    ? "bg-[#0b1c30] text-white shadow-2xs"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {period === "CUSTOM" && (
            <div className="flex items-center gap-2 flex-wrap">
              <input
                type="date"
                aria-label="Từ ngày"
                value={customStart}
                onChange={(e) => setCustomStart(e.target.value)}
                className="h-9 px-2.5 rounded-xl border border-slate-200 bg-white text-xs text-slate-800"
              />
              <span aria-hidden="true" className="text-xs text-slate-500">
                →
              </span>
              <input
                type="date"
                aria-label="Đến ngày"
                value={customEnd}
                onChange={(e) => setCustomEnd(e.target.value)}
                className="h-9 px-2.5 rounded-xl border border-slate-200 bg-white text-xs text-slate-800"
              />
            </div>
          )}

          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Làm mới số liệu"
            className="h-9 px-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 flex items-center gap-1.5 cursor-pointer"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${isFetching ? "animate-spin motion-reduce:animate-none" : ""}`}
            />
            <span className="hidden sm:inline">Làm mới</span>
          </button>
        </div>
      </div>

      {/* 2. URGENT MODERATION QUEUE BAR */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link
          href={`${ROUTES.ADMIN.PRODUCTS}?approvalStatus=PENDING_APPROVAL`}
          className={`p-4 rounded-2xl border transition-colors duration-150 flex items-center justify-between gap-3 ${
            pendingProductsCount > 0
              ? "bg-amber-50/80 border-amber-200 hover:border-amber-300"
              : "bg-white border-slate-200/80 hover:border-[#0045cc]/30"
          }`}
        >
          <div className="flex items-center gap-3 min-w-0">
            <div
              className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                pendingProductsCount > 0
                  ? "bg-amber-500 text-white"
                  : "bg-slate-100 text-slate-500"
              }`}
            >
              <PackageCheck className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-[#0b1c30]">
                Hàng chờ kiểm duyệt Sản phẩm
              </p>
              <p className="text-[11px] text-slate-600 truncate">
                {pendingProductsCount > 0
                  ? `Có ${pendingProductsCount} sản phẩm đang chờ Admin phê duyệt lên sàn`
                  : "Không có sản phẩm nào tồn đọng cần duyệt"}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-extrabold ${
                pendingProductsCount > 0
                  ? "bg-amber-600 text-white"
                  : "bg-slate-100 text-slate-600"
              }`}
            >
              {pendingProductsCount}
            </span>
            <ArrowRight className="w-4 h-4 text-slate-400" />
          </div>
        </Link>

        <Link
          href={`${ROUTES.ADMIN.SELLERS}?status=PENDING`}
          className={`p-4 rounded-2xl border transition-colors duration-150 flex items-center justify-between gap-3 ${
            pendingSellersCount > 0
              ? "bg-[#eff4ff] border-[#0045cc]/30 hover:border-[#0045cc]/50"
              : "bg-white border-slate-200/80 hover:border-[#0045cc]/30"
          }`}
        >
          <div className="flex items-center gap-3 min-w-0">
            <div
              className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                pendingSellersCount > 0
                  ? "bg-[#0045cc] text-white"
                  : "bg-slate-100 text-slate-500"
              }`}
            >
              <Store className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-[#0b1c30]">
                Hồ sơ đăng ký mở Shop Seller
              </p>
              <p className="text-[11px] text-slate-600 truncate">
                {pendingSellersCount > 0
                  ? `Có ${pendingSellersCount} hồ sơ đối tác mới chờ thẩm định`
                  : "Tất cả hồ sơ đối tác đã được xử lý"}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-extrabold ${
                pendingSellersCount > 0
                  ? "bg-[#0045cc] text-white"
                  : "bg-slate-100 text-slate-600"
              }`}
            >
              {pendingSellersCount}
            </span>
            <ArrowRight className="w-4 h-4 text-slate-400" />
          </div>
        </Link>
      </div>

      {/* 3. METRICS BODY */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500 font-medium">
            Đang tổng hợp số liệu toàn sàn...
          </span>
        </div>
      ) : isError || !metrics ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <p className="text-sm font-bold text-slate-800">
            Không thể tải dữ liệu Dashboard hệ thống
          </p>
          <button
            type="button"
            onClick={() => refetch()}
            className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : (
        <>
          {/* 4 BENTO KPI CARDS */}
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            {/* Card 1: GMV */}
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-500">
                  Tổng GMV Toàn sàn
                </span>
                <div className="w-8 h-8 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
                  <TrendingUp className="w-4 h-4" />
                </div>
              </div>
              <div className="font-heading text-xl sm:text-2xl font-extrabold text-[#0b1c30] font-mono">
                {formatVND(metrics.summary.totalGmv)}
              </div>
              <div className="text-[11px] text-slate-500 border-t border-slate-100 pt-2.5 flex items-center justify-between">
                <span>GMV đã hoàn tất:</span>
                <span className="font-bold text-emerald-700 font-mono">
                  {formatVND(metrics.summary.completedGmv)}
                </span>
              </div>
            </div>

            {/* Card 2: Commission */}
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-500">
                  Doanh thu Hoa hồng Sàn
                </span>
                <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                  <Coins className="w-4 h-4" />
                </div>
              </div>
              <div className="font-heading text-xl sm:text-2xl font-extrabold text-emerald-700 font-mono">
                {formatVND(metrics.summary.platformCommissionRevenue)}
              </div>
              <div className="text-[11px] text-slate-500 border-t border-slate-100 pt-2.5 flex items-center justify-between">
                <span>Giá trị đơn TB (AOV):</span>
                <span className="font-bold text-slate-800 font-mono">
                  {formatVND(metrics.summary.averageOrderValue)}
                </span>
              </div>
            </div>

            {/* Card 3: Orders */}
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-500">
                  Tổng Đơn hàng & Sản lượng
                </span>
                <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                  <ShoppingBag className="w-4 h-4" />
                </div>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="font-heading text-xl sm:text-2xl font-extrabold text-[#0b1c30] font-mono">
                  {metrics.summary.totalOrders}
                </span>
                <span className="text-xs text-slate-500 font-medium">
                  đơn ({metrics.summary.totalUnitsSold} sản phẩm)
                </span>
              </div>
              <div className="text-[11px] text-slate-500 border-t border-slate-100 pt-2.5 flex items-center justify-between">
                <span>Hoàn tất / Tỉ lệ hủy:</span>
                <span className="font-bold text-slate-800">
                  {metrics.summary.completedOrders} đơn •{" "}
                  <span className="text-rose-600">
                    {metrics.summary.cancellationRate.toFixed(1)}%
                  </span>
                </span>
              </div>
            </div>

            {/* Card 4: Wallet & Payouts */}
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-500">
                  Quỹ Ví Seller & Payout
                </span>
                <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                  <Wallet className="w-4 h-4" />
                </div>
              </div>
              <div className="font-heading text-xl sm:text-2xl font-extrabold text-[#0b1c30] font-mono">
                {formatVND(metrics.summary.totalSellersWalletBalance)}
              </div>
              <div className="text-[11px] text-slate-500 border-t border-slate-100 pt-2.5 flex items-center justify-between">
                <span>Chờ giải ngân:</span>
                <span className="font-bold text-amber-700 font-mono">
                  {formatVND(metrics.summary.pendingPayoutAmount)}
                </span>
              </div>
            </div>
          </div>

          {/* 4. BREAKDOWN & SHOP HEALTH */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Order Status Breakdown */}
            <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 shadow-2xs flex flex-col gap-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-[#0045cc]" />
                  <h2 className="text-sm font-bold text-[#0b1c30]">
                    Phân bổ Trạng thái Đơn hàng Toàn sàn
                  </h2>
                </div>
                <span className="text-[11px] text-slate-600 font-mono">
                  {metrics.startDate} → {metrics.endDate}
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {[
                  {
                    label: "Chờ xác nhận",
                    val: metrics.orderStatusBreakdown.pendingConfirmation,
                    tone: "bg-amber-50 text-amber-800 border-amber-200/60",
                  },
                  {
                    label: "Đang chuẩn bị",
                    val: metrics.orderStatusBreakdown.preparing,
                    tone: "bg-sky-50 text-sky-800 border-sky-200/60",
                  },
                  {
                    label: "Chờ lấy hàng",
                    val: metrics.orderStatusBreakdown.waitingForPickup,
                    tone: "bg-indigo-50 text-indigo-800 border-indigo-200/60",
                  },
                  {
                    label: "Đang giao hàng",
                    val: metrics.orderStatusBreakdown.shipping,
                    tone: "bg-blue-50 text-blue-800 border-blue-200/60",
                  },
                  {
                    label: "Đã giao",
                    val: metrics.orderStatusBreakdown.delivered,
                    tone: "bg-teal-50 text-teal-800 border-teal-200/60",
                  },
                  {
                    label: "Hoàn thành",
                    val: metrics.orderStatusBreakdown.completed,
                    tone: "bg-emerald-50 text-emerald-800 border-emerald-200/60",
                  },
                  {
                    label: "Đã hủy",
                    val: metrics.orderStatusBreakdown.cancelled,
                    tone: "bg-slate-50 text-slate-700 border-slate-200",
                  },
                  {
                    label: "Yêu cầu trả hàng",
                    val: metrics.orderStatusBreakdown.returnRequested,
                    tone: "bg-orange-50 text-orange-800 border-orange-200/60",
                  },
                  {
                    label: "Đã hoàn tiền",
                    val: metrics.orderStatusBreakdown.refunded,
                    tone: "bg-rose-50 text-rose-800 border-rose-200/60",
                  },
                ].map((item) => (
                  <div
                    key={item.label}
                    className={`p-3 rounded-xl border flex flex-col gap-1 ${item.tone}`}
                  >
                    <span className="text-[11px] font-semibold opacity-80">
                      {item.label}
                    </span>
                    <span className="font-heading text-lg font-extrabold font-mono">
                      {item.val}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Growth & Shop Health */}
            <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 shadow-2xs flex flex-col gap-4">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <Users className="w-4 h-4 text-[#0045cc]" />
                <h2 className="text-sm font-bold text-[#0b1c30]">
                  Tăng trưởng & Sức khỏe Gian hàng
                </h2>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-3.5 rounded-xl bg-[#eff4ff]/70 border border-[#0045cc]/15">
                  <span className="text-[11px] font-semibold text-slate-600">
                    Người dùng mới
                  </span>
                  <p className="font-heading text-xl font-extrabold text-[#0045cc] font-mono mt-0.5">
                    +{metrics.growthMetrics.newUsersCount}
                  </p>
                </div>
                <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200/70">
                  <span className="text-[11px] font-semibold text-slate-600">
                    Shop mới mở
                  </span>
                  <p className="font-heading text-xl font-extrabold text-emerald-700 font-mono mt-0.5">
                    +{metrics.growthMetrics.newShopsCount}
                  </p>
                </div>
              </div>

              <div className="flex flex-col gap-2 text-xs pt-1">
                <div className="flex items-center justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-600">Shop đang hoạt động (ACTIVE)</span>
                  <span className="font-bold text-emerald-700 font-mono">
                    {metrics.growthMetrics.totalActiveShops}
                  </span>
                </div>
                <div className="flex items-center justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-600">Shop đang tạm nghỉ (PAUSED)</span>
                  <span className="font-bold text-amber-600 font-mono">
                    {metrics.growthMetrics.totalPausedShops}
                  </span>
                </div>
                <div className="flex items-center justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-600">Shop bị đình chỉ (SUSPENDED)</span>
                  <span className="font-bold text-orange-600 font-mono">
                    {metrics.growthMetrics.totalSuspendedShops}
                  </span>
                </div>
                <div className="flex items-center justify-between py-2">
                  <span className="text-slate-600">Shop bị cấm vĩnh viễn (BANNED)</span>
                  <span className="font-bold text-rose-600 font-mono">
                    {metrics.growthMetrics.totalBannedShops}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* 5. TOP PERFORMING SHOPS */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 shadow-2xs flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Award className="w-4 h-4 text-amber-500" />
                <h2 className="text-sm font-bold text-[#0b1c30]">
                  Top Gian hàng Doanh thu Cao nhất Kỳ
                </h2>
              </div>
              <Link
                href={ROUTES.ADMIN.SELLERS}
                className="text-xs font-bold text-[#0045cc] hover:underline"
              >
                Quản lý gian hàng →
              </Link>
            </div>

            {metrics.topPerformingShops.length === 0 ? (
              <p className="text-xs text-slate-600 text-center py-6">
                Chưa có dữ liệu đơn hàng phát sinh trong khoảng thời gian này.
              </p>
            ) : (
              <div className="overflow-x-auto no-scrollbar">
                <table className="w-full min-w-[600px] text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                      <th className="py-2.5 px-3">Gian hàng</th>
                      <th className="py-2.5 px-3 text-center">Tổng đơn</th>
                      <th className="py-2.5 px-3 text-right">Tổng GMV</th>
                      <th className="py-2.5 px-3 text-right">Hoa hồng đóng góp</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {metrics.topPerformingShops.map((shop, idx) => (
                      <tr key={shop.shopId} className="hover:bg-slate-50/70">
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-2.5">
                            <span className="w-5 h-5 rounded-md bg-[#eff4ff] text-[#0045cc] font-bold text-[10px] flex items-center justify-center shrink-0">
                              #{idx + 1}
                            </span>
                            <span className="font-bold text-slate-900">
                              {shop.shopName}
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-3 text-center font-mono font-semibold text-slate-700">
                          {shop.totalOrders}
                        </td>
                        <td className="py-3 px-3 text-right font-mono font-bold text-slate-900">
                          {formatVND(shop.totalGmv)}
                        </td>
                        <td className="py-3 px-3 text-right font-mono font-bold text-emerald-700">
                          {formatVND(shop.commissionContributed)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
