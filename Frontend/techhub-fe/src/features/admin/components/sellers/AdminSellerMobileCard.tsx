"use client";

import React from "react";
import { Eye, Store, User, CreditCard } from "lucide-react";
import type { AdminSellerResponse } from "@/types/admin";

interface AdminSellerMobileCardProps {
  seller: AdminSellerResponse;
  onInspect: (sellerId: string) => void;
}

export function AdminSellerMobileCard({
  seller,
  onInspect,
}: AdminSellerMobileCardProps) {
  const getBadgeClass = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "PENDING":
        return "bg-amber-100 text-amber-800 border-amber-200";
      default:
        return "bg-rose-100 text-rose-800 border-rose-200";
    }
  };

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Shop Name & Status */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <Store className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <h4 className="font-bold text-xs text-slate-900 truncate">
              {seller.shopName || "Chưa đặt tên Shop"}
            </h4>
          </div>
          <p className="text-[10px] font-mono text-[#0045cc]">
            @{seller.sellerName}
          </p>
        </div>

        <span
          className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getBadgeClass(
            seller.verificationStatus
          )}`}
        >
          {seller.verificationStatus}
        </span>
      </div>

      {/* 2. Owner & Bank Details */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1 text-xs">
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">Chủ sở hữu:</span>
          <span className="font-medium text-slate-900 truncate max-w-[180px]">
            {seller.ownerName}
          </span>
        </div>
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">Email:</span>
          <span className="text-slate-600 truncate max-w-[180px]">
            {seller.ownerEmail}
          </span>
        </div>
        {seller.bankName && (
          <div className="flex items-center justify-between text-slate-700 pt-1 border-t border-slate-200/60">
            <span className="text-slate-500 text-[11px]">Ngân hàng:</span>
            <span className="font-mono text-slate-800 text-[11px] truncate max-w-[180px]">
              {seller.bankName} - {seller.bankAccountNumber}
            </span>
          </div>
        )}
      </div>

      {/* 3. Actions */}
      <div className="flex items-center justify-end pt-1 border-t border-slate-100 text-xs">
        <button
          type="button"
          onClick={() => onInspect(seller.id)}
          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Thẩm định hồ sơ</span>
        </button>
      </div>
    </div>
  );
}
