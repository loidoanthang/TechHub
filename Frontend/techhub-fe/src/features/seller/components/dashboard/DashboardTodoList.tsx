"use client";

import React from "react";
import Link from "next/link";
import {
  PackageCheck,
  Truck,
  RotateCcw,
  Clock,
  ArrowRight,
} from "lucide-react";
import type { SellerOrderStatusBreakdownDto } from "@/types/sellerDashboard";

interface DashboardTodoListProps {
  breakdown?: SellerOrderStatusBreakdownDto;
  isLoading?: boolean;
}

export function DashboardTodoList({
  breakdown,
  isLoading,
}: DashboardTodoListProps) {
  const pendingConfirmation = breakdown?.pendingConfirmation ?? 0;
  const preparing = (breakdown?.confirmed ?? 0) + (breakdown?.preparing ?? 0);
  const waitingPickup = breakdown?.waitingForPickup ?? 0;
  const returnRequested = breakdown?.returnRequested ?? 0;

  const totalUrgent = pendingConfirmation + preparing + returnRequested;

  if (isLoading) {
    return (
      <div className="h-12 w-full bg-slate-100 rounded-2xl animate-pulse" />
    );
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 bg-white px-4 py-2.5 rounded-2xl border border-[#e2e8f0] shadow-2xs">
      <div className="flex items-center gap-2">
        <span className="flex h-2 w-2 rounded-full bg-amber-500 animate-pulse" />
        <span className="text-xs font-bold text-[#0b1c30]">
          Cần xử lý hôm nay ({totalUrgent}):
        </span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {/* Chờ xác nhận */}
        <Link
          href="/seller/orders?status=PENDING_CONFIRMATION"
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-medium bg-amber-50 text-amber-800 border border-amber-200/60 hover:bg-amber-100 transition-colors"
        >
          <Clock className="w-3.5 h-3.5 text-amber-600" />
          <span>Chờ duyệt:</span>
          <strong className="font-bold">{pendingConfirmation}</strong>
        </Link>

        {/* Chờ đóng gói */}
        <Link
          href="/seller/orders?status=PREPARING"
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-medium bg-blue-50 text-[#0045cc] border border-blue-200/60 hover:bg-blue-100 transition-colors"
        >
          <PackageCheck className="w-3.5 h-3.5 text-[#0045cc]" />
          <span>Chờ đóng gói:</span>
          <strong className="font-bold">{preparing}</strong>
        </Link>

        {/* Chờ giao hàng */}
        <Link
          href="/seller/orders?status=WAITING_FOR_PICKUP"
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-medium bg-slate-50 text-slate-700 border border-slate-200 hover:bg-slate-100 transition-colors"
        >
          <Truck className="w-3.5 h-3.5 text-slate-500" />
          <span>Chờ lấy:</span>
          <strong className="font-bold">{waitingPickup}</strong>
        </Link>

        {/* Khiếu nại / Trả hàng */}
        {returnRequested > 0 && (
          <Link
            href="/seller/orders?status=RETURN_REQUESTED"
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-medium bg-red-50 text-red-700 border border-red-200/60 hover:bg-red-100 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5 text-red-600" />
            <span>Đổi trả:</span>
            <strong className="font-bold">{returnRequested}</strong>
          </Link>
        )}
      </div>

      <Link
        href="/seller/orders"
        className="text-[11px] font-bold text-[#0045cc] hover:underline flex items-center gap-1"
      >
        <span>Xem tất cả đơn</span>
        <ArrowRight className="w-3 h-3" />
      </Link>
    </div>
  );
}
