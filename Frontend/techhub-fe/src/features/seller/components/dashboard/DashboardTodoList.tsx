"use client";

import React from "react";
import Link from "next/link";
import {
  Clock,
  Package,
  Truck,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  FileX,
  ChevronRight,
} from "lucide-react";
import type { SellerOrderStatusBreakdownDto } from "@/types/sellerDashboard";

interface DashboardTodoListProps {
  breakdown?: SellerOrderStatusBreakdownDto;
  isLoading?: boolean;
}

export function DashboardTodoList({ breakdown, isLoading }: DashboardTodoListProps) {
  const pendingConfirmation = breakdown?.pendingConfirmation ?? 0;
  const preparing = breakdown?.preparing ?? 0;
  const waitingForPickup = breakdown?.waitingForPickup ?? 0;
  const shipping = breakdown?.shipping ?? 0;
  const returnOrRefund = (breakdown?.returnRequested ?? 0) + (breakdown?.refunded ?? 0);
  const cancelled = breakdown?.cancelled ?? 0;

  const todoItems = [
    {
      label: "Chờ xác nhận",
      count: pendingConfirmation,
      href: "/seller/orders?status=PENDING_CONFIRMATION",
      icon: Clock,
      urgent: pendingConfirmation > 0,
      badgeColor: "text-amber-600 bg-amber-50 border-amber-200",
    },
    {
      label: "Đang đóng gói",
      count: preparing,
      href: "/seller/orders?status=PREPARING",
      icon: Package,
      urgent: preparing > 0,
      badgeColor: "text-[#0045cc] bg-[#eff4ff] border-[#0045cc]/20",
    },
    {
      label: "Chờ bưu tá lấy",
      count: waitingForPickup,
      href: "/seller/orders?status=WAITING_FOR_PICKUP",
      icon: Truck,
      urgent: waitingForPickup > 0,
      badgeColor: "text-emerald-700 bg-emerald-50 border-emerald-200",
    },
    {
      label: "Đang giao vận",
      count: shipping,
      href: "/seller/orders?status=SHIPPING",
      icon: Truck,
      urgent: false,
      badgeColor: "text-slate-600 bg-slate-100 border-slate-200",
    },
    {
      label: "Yêu cầu trả hàng",
      count: returnOrRefund,
      href: "/seller/orders?tab=returns",
      icon: RotateCcw,
      urgent: returnOrRefund > 0,
      badgeColor: "text-red-700 bg-red-50 border-red-200",
    },
    {
      label: "Đơn bị hủy",
      count: cancelled,
      href: "/seller/orders?tab=cancellations",
      icon: FileX,
      urgent: false,
      badgeColor: "text-slate-600 bg-slate-100 border-slate-200",
    },
  ];

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[#f1f5f9] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center font-bold">
            <Package className="w-4 h-4" />
          </div>
          <div>
            <h2 className="font-heading text-sm sm:text-base font-bold text-[#0b1c30]">
              Danh Sách Cần Xử Lý (To-Do List)
            </h2>
            <p className="text-[11px] text-[#737688]">
              Các đầu việc vận hành đơn hàng cần sự chú ý của bạn ngay hôm nay.
            </p>
          </div>
        </div>
        <Link
          href="/seller/orders"
          className="text-xs font-bold text-[#0045cc] hover:text-[#0d5bff] flex items-center gap-1 transition-colors"
        >
          <span>Xem tất cả đơn</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Grid of Todo Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {todoItems.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.label}
              href={item.href}
              className={`p-3.5 rounded-2xl border transition-all flex flex-col justify-between gap-2.5 group hover:shadow-xs hover:-translate-y-0.5 ${
                item.urgent
                  ? "bg-white border-[#0045cc]/20 hover:border-[#0045cc]"
                  : "bg-[#f8f9fc]/80 border-[#e2e8f0] hover:bg-white hover:border-[#cbd5e1]"
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-[#737688] line-clamp-1">
                  {item.label}
                </span>
                <Icon
                  className={`w-3.5 h-3.5 ${
                    item.urgent ? "text-[#0045cc]" : "text-[#94a3b8]"
                  }`}
                />
              </div>

              <div className="flex items-baseline gap-1">
                {isLoading ? (
                  <div className="h-7 w-12 bg-slate-200 animate-pulse rounded-md" />
                ) : (
                  <span
                    className={`font-heading text-xl sm:text-2xl font-extrabold tracking-tight ${
                      item.urgent ? "text-[#0045cc]" : "text-[#0b1c30]"
                    }`}
                  >
                    {item.count}
                  </span>
                )}
                <span className="text-[10px] text-[#94a3b8] font-medium">đơn</span>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
