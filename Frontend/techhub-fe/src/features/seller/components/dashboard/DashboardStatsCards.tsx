"use client";

import React from "react";
import { TrendingUp, ShoppingBag, ArrowUpRight, DollarSign } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { SellerDashboardSummaryDto } from "@/types/sellerDashboard";
import Link from "next/link";

interface DashboardStatsCardsProps {
  summary?: SellerDashboardSummaryDto;
  isLoading?: boolean;
}

export function DashboardStatsCards({ summary, isLoading }: DashboardStatsCardsProps) {
  const averageOrderValue = summary?.averageOrderValue ?? 0;
  const totalOrders = summary?.totalOrders ?? 0;
  const completedOrders = summary?.completedOrders ?? 0;
  const netRevenue = summary?.netRevenue ?? 0;
  const walletBalance = summary?.walletBalance ?? 0;

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-36 rounded-3xl bg-white border border-[#e2e8f0] p-6 animate-pulse flex flex-col justify-between"
          >
            <div className="h-4 bg-slate-200 rounded w-1/3" />
            <div className="h-8 bg-slate-200 rounded w-2/3" />
            <div className="h-3 bg-slate-200 rounded w-1/2" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
      {/* 1. AVG. Order Value - Dark Hero Card (Màu Navy tối #0b1c30 chuẩn TechHub) */}
      <div className="relative overflow-hidden rounded-3xl bg-[#0b1c30] text-white p-6 shadow-sm flex flex-col justify-between group transition-all duration-300 hover:shadow-md">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-300 tracking-wide uppercase">
            AVG . Order Value
          </span>
          <div className="w-10 h-10 rounded-2xl bg-white/10 flex items-center justify-center text-white backdrop-blur-sm border border-white/10">
            <TrendingUp className="w-5 h-5 text-[#b6c4ff]" />
          </div>
        </div>

        <div className="my-3">
          <h2 className="text-3xl font-extrabold tracking-tight font-heading text-white">
            {formatPrice(averageOrderValue)}
          </h2>
        </div>

        <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 font-medium">
          <span className="inline-flex items-center px-1.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold">
            +3.16%
          </span>
          <span className="text-slate-400">So với tháng trước</span>
        </div>

        {/* Subtle decorative glow */}
        <div className="absolute -bottom-10 -right-10 w-32 h-32 rounded-full bg-[#0045cc]/20 blur-2xl pointer-events-none" />
      </div>

      {/* 2. Total Orders Card - Clean Light */}
      <div className="rounded-3xl bg-white border border-[#e2e8f0] p-6 shadow-2xs flex flex-col justify-between transition-all duration-300 hover:shadow-sm hover:border-[#cbd5e1]">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-[#737688] tracking-wide uppercase">
            Total Orders
          </span>
          <div className="w-10 h-10 rounded-2xl bg-blue-50 flex items-center justify-center text-[#0045cc]">
            <ShoppingBag className="w-5 h-5" />
          </div>
        </div>

        <div className="my-3">
          <h2 className="text-3xl font-extrabold tracking-tight font-heading text-[#0b1c30]">
            {totalOrders.toLocaleString("vi-VN")}
          </h2>
        </div>

        <div className="flex items-center justify-between text-[11px]">
          <div className="flex items-center gap-1 text-slate-500">
            <span>{completedOrders.toLocaleString("vi-VN")} đơn đã hoàn thành</span>
          </div>
          <Link
            href="/seller/orders"
            className="text-[#0045cc] font-semibold hover:underline inline-flex items-center gap-0.5"
          >
            Chi tiết <ArrowUpRight className="w-3 h-3" />
          </Link>
        </div>
      </div>

      {/* 3. Lifetime Value & Wallet Card */}
      <div className="rounded-3xl bg-white border border-[#e2e8f0] p-6 shadow-2xs flex flex-col justify-between transition-all duration-300 hover:shadow-sm hover:border-[#cbd5e1]">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-[#737688] tracking-wide uppercase">
            Lifetime Value / Doanh Thu
          </span>
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 flex items-center justify-center text-indigo-600">
            <DollarSign className="w-5 h-5" />
          </div>
        </div>

        <div className="my-3">
          <h2 className="text-3xl font-extrabold tracking-tight font-heading text-[#0b1c30]">
            {formatPrice(netRevenue)}
          </h2>
        </div>

        <div className="flex items-center justify-between text-[11px] pt-1 border-t border-slate-100">
          <span className="text-slate-500">
            Số dư ví: <strong className="text-emerald-600 font-bold">{formatPrice(walletBalance)}</strong>
          </span>
          <Link
            href="/seller/wallet"
            className="text-[#0045cc] font-semibold hover:underline inline-flex items-center gap-0.5"
          >
            Ví tiền <ArrowUpRight className="w-3 h-3" />
          </Link>
        </div>
      </div>
    </div>
  );
}
