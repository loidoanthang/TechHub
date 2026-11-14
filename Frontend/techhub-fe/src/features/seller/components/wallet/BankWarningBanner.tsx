"use client";

import React from "react";
import Link from "next/link";
import { AlertTriangle, ArrowRight, CreditCard } from "lucide-react";
import { ROUTES } from "@/lib/constants";

interface BankWarningBannerProps {
  isBankIncomplete: boolean;
}

export function BankWarningBanner({ isBankIncomplete }: BankWarningBannerProps) {
  if (!isBankIncomplete) return null;

  return (
    <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 animate-in fade-in duration-200">
      <div className="flex items-start sm:items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center shrink-0 text-amber-600">
          <AlertTriangle className="w-5 h-5" />
        </div>
        <div>
          <h4 className="text-sm font-bold text-amber-900">
            Chưa hoàn tất thông tin tài khoản ngân hàng
          </h4>
          <p className="text-xs text-amber-700 mt-0.5">
            Bạn cần liên kết tài khoản ngân hàng chính chủ để có thể gửi yêu cầu rút tiền về tài khoản.
          </p>
        </div>
      </div>
      <Link
        href={`${ROUTES.SELLER.SETTINGS}?tab=payout`}
        className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition-colors flex items-center gap-1.5 shrink-0 shadow-sm"
      >
        <CreditCard className="w-4 h-4" />
        <span>Liên kết ngay</span>
        <ArrowRight className="w-3.5 h-3.5" />
      </Link>
    </div>
  );
}
