"use client";

import React from "react";
import { Landmark, Store, CheckCircle2, XCircle } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { AdminPayoutResponse } from "@/types/adminFinance";

interface AdminPayoutMobileCardProps {
  payout: AdminPayoutResponse;
  onApprove: (payout: AdminPayoutResponse) => void;
  onReject: (payout: AdminPayoutResponse) => void;
  isProcessing?: boolean;
}

export function AdminPayoutMobileCard({
  payout,
  onApprove,
  onReject,
  isProcessing = false,
}: AdminPayoutMobileCardProps) {
  const isPending = payout.status === "PENDING";

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      case "REJECTED":
        return "bg-rose-50 text-rose-700 border-rose-200";
      case "PENDING":
        return "bg-amber-50 text-amber-800 border-amber-200";
      default:
        return "bg-slate-100 text-slate-700 border-slate-200";
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
              {payout.shopName || payout.sellerName}
            </h4>
          </div>
          <p className="text-[10px] font-mono text-[#0045cc]">
            @{payout.sellerName}
          </p>
        </div>

        <span
          className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getStatusBadge(
            payout.status
          )}`}
        >
          {payout.status}
        </span>
      </div>

      {/* 2. Bank Details */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1 text-xs">
        <div className="flex items-center gap-1.5 text-slate-700 font-semibold">
          <Landmark className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <span className="truncate">{payout.bankName}</span>
        </div>
        <div className="flex items-center justify-between text-slate-600 pl-5">
          <span className="font-mono">{payout.bankAccountNumber}</span>
          <span className="truncate max-w-[150px] font-medium uppercase">
            {payout.bankAccountHolder}
          </span>
        </div>
      </div>

      {/* 3. Amount & Action Buttons */}
      <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
        <div>
          <span className="text-[10px] text-slate-500 font-medium block">Số tiền rút:</span>
          <span className="font-mono font-bold text-slate-900 text-sm">
            {formatPrice(payout.amount)}
          </span>
        </div>

        {isPending ? (
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={isProcessing}
              onClick={() => onReject(payout)}
              className="px-2.5 py-1.5 rounded-lg border border-rose-200 text-rose-700 hover:bg-rose-50 text-xs font-semibold inline-flex items-center gap-1 transition-colors cursor-pointer"
            >
              <XCircle className="w-3.5 h-3.5" />
              <span>Từ chối</span>
            </button>
            <button
              type="button"
              disabled={isProcessing}
              onClick={() => onApprove(payout)}
              className="px-2.5 py-1.5 rounded-lg bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-semibold inline-flex items-center gap-1 shadow-2xs transition-colors cursor-pointer"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Duyệt tiền</span>
            </button>
          </div>
        ) : (
          <span className="text-[11px] text-slate-400 font-medium">
            Đã xử lý
          </span>
        )}
      </div>
    </div>
  );
}
