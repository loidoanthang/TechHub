"use client";

import React from "react";
import { Eye, Store, User, CreditCard } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { AdminOrderResponse } from "@/types/admin";

interface AdminOrderMobileCardProps {
  order: AdminOrderResponse;
  onViewDetails: (orderId: string) => void;
}

export function AdminOrderMobileCard({
  order,
  onViewDetails,
}: AdminOrderMobileCardProps) {
  const getStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "CANCELLED":
      case "REFUNDED":
        return "bg-rose-100 text-rose-800 border-rose-200";
      case "PENDING_CONFIRMATION":
        return "bg-purple-100 text-purple-800 border-purple-200";
      case "PREPARING":
        return "bg-indigo-100 text-indigo-800 border-indigo-200";
      case "WAITING_FOR_PICKUP":
        return "bg-teal-100 text-teal-800 border-teal-200";
      case "SHIPPING":
        return "bg-sky-100 text-sky-800 border-sky-200";
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  const getCleanPaymentName = () => {
    const raw = (order.paymentMethodName || order.paymentMethod || "").toUpperCase();
    if (raw.includes("VNPAY")) return "VNPAY";
    if (raw.includes("COD")) return "COD";
    return order.paymentMethod || "COD";
  };

  const buyerDisplayName =
    order.buyer?.recipientName || order.buyer?.buyerName || "Khách hàng";

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Order number, date & Status */}
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0">
          <span className="font-mono font-bold text-xs text-[#0045cc]">
            #{order.orderNumber}
          </span>
          <p className="text-[10px] text-slate-500">
            {new Date(order.createdAt).toLocaleDateString("vi-VN", {
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
        </div>

        <span
          className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getStatusBadge(
            order.status
          )}`}
        >
          {order.status}
        </span>
      </div>

      {/* 2. Shop & Buyer Info */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1.5 text-xs">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 text-slate-700 min-w-0">
            <Store className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="font-semibold text-slate-900 truncate">
              {order.shop.shopName}
            </span>
          </div>
          <span className="text-[10px] font-medium text-slate-500 shrink-0">
            {order.totalItemsCount} món
          </span>
        </div>

        <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-200/60">
          <div className="flex items-center gap-1.5 text-slate-700 min-w-0">
            <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="truncate font-medium text-slate-800">
              {buyerDisplayName}
            </span>
          </div>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-white text-slate-700 border border-slate-200 shrink-0">
            {getCleanPaymentName()}
          </span>
        </div>
      </div>

      {/* 3. Financials & Action */}
      <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
        <div>
          <div className="text-[10px] text-slate-500 font-medium">Tổng tiền & Hoa hồng sàn:</div>
          <div className="flex items-center gap-2 mt-0.5">
            <span className="font-mono font-bold text-slate-900">
              {formatPrice(order.buyerTotalAmount)}
            </span>
            <span className="text-slate-300">•</span>
            <span className="font-mono font-bold text-emerald-600 text-[11px]" title="Hoa hồng sàn">
              +{formatPrice(order.totalCommission)}
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => onViewDetails(order.orderId)}
          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-[#0045cc] hover:text-white text-slate-700 text-xs font-semibold transition-colors cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Chi tiết</span>
        </button>
      </div>
    </div>
  );
}
