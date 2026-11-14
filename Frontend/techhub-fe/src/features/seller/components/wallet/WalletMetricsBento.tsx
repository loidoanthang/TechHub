"use client";

import React from "react";
import {
  Wallet,
  ArrowDownToLine,
  Clock,
  TrendingUp,
  ShieldCheck,
} from "lucide-react";
import { formatPrice } from "@/lib/utils";
import { WALLET_LIMITS } from "../../types/wallet";

interface WalletMetricsBentoProps {
  walletBalance: number;
  pendingPayoutAmount: number;
  netRevenue: number;
  isBankIncomplete: boolean;
  onOpenWithdrawModal: () => void;
  isLoading?: boolean;
}

export function WalletMetricsBento({
  walletBalance,
  pendingPayoutAmount,
  netRevenue,
  isBankIncomplete,
  onOpenWithdrawModal,
  isLoading = false,
}: WalletMetricsBentoProps) {
  const canWithdraw =
    walletBalance >= WALLET_LIMITS.MIN_WITHDRAWAL && !isBankIncomplete;

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="p-6 rounded-2xl bg-white border border-slate-100 shadow-xs animate-pulse space-y-4"
          >
            <div className="h-4 bg-slate-200 rounded w-1/3" />
            <div className="h-8 bg-slate-200 rounded w-2/3" />
            <div className="h-4 bg-slate-100 rounded w-1/2" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
      {/* 1. Số dư khả dụng (Hero Card) */}
      <div className="p-6 rounded-2xl bg-gradient-to-br from-[#0b1c30] to-[#122842] text-white shadow-md relative overflow-hidden flex flex-col justify-between">
        {/* Background Decorative Element */}
        <div className="absolute -right-6 -bottom-6 w-32 h-32 rounded-full bg-blue-500/10 pointer-events-none blur-2xl" />

        <div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
              Số dư ví khả dụng
            </span>
            <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center text-blue-400">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <h3 className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-white">
              {formatPrice(walletBalance)}
            </h3>
            <p className="text-[11px] text-slate-400 mt-1">
              {isBankIncomplete
                ? "Cần liên kết ngân hàng để rút tiền"
                : walletBalance < WALLET_LIMITS.MIN_WITHDRAWAL
                ? `Tối thiểu ${formatPrice(WALLET_LIMITS.MIN_WITHDRAWAL)} để tạo lệnh rút`
                : "Sẵn sàng rút về tài khoản ngân hàng"}
            </p>
          </div>
        </div>

        <div className="mt-5 pt-4 border-t border-white/10">
          <button
            type="button"
            onClick={onOpenWithdrawModal}
            disabled={!canWithdraw}
            title={
              !canWithdraw
                ? isBankIncomplete
                  ? "Cần liên kết ngân hàng để rút tiền"
                  : `Cần tối thiểu ${formatPrice(WALLET_LIMITS.MIN_WITHDRAWAL)} để rút`
                : "Tạo yêu cầu rút tiền về ngân hàng"
            }
            className={`w-full py-2.5 px-4 rounded-xl text-xs font-bold transition-all shadow-sm flex items-center justify-center gap-2 ${
              canWithdraw
                ? "bg-[#0045cc] hover:bg-[#0d5bff] text-white cursor-pointer active:scale-[0.99]"
                : "bg-slate-700/60 text-slate-400 cursor-not-allowed opacity-60"
            }`}
          >
            <ArrowDownToLine className="w-4 h-4" />
            <span>Rút tiền về ngân hàng</span>
          </button>
        </div>
      </div>

      {/* 2. Tiền chờ duyệt */}
      <div className="p-6 rounded-2xl bg-white border border-[#eff4ff] shadow-xs flex flex-col justify-between">
        <div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Chờ duyệt
            </span>
            <div className="w-8 h-8 rounded-xl bg-amber-50 flex items-center justify-center text-amber-600">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <h3 className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-slate-900">
              {formatPrice(pendingPayoutAmount)}
            </h3>
            <p className="text-[11px] text-slate-500 mt-1">
              Lệnh rút đang chờ TechHub phê duyệt &amp; giải ngân
            </p>
          </div>
        </div>

        <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <span>Thời gian xử lý</span>
          <span className="font-semibold text-slate-700">Trong 24h làm việc</span>
        </div>
      </div>

      {/* 3. Doanh thu thuần tích lũy */}
      <div className="p-6 rounded-2xl bg-white border border-[#eff4ff] shadow-xs flex flex-col justify-between">
        <div>
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Doanh thu lũy kế
            </span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center text-emerald-600">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <h3 className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-slate-900">
              {formatPrice(netRevenue)}
            </h3>
            <p className="text-[11px] text-slate-500 mt-1">
              Tổng số tiền đã quyết toán từ các đơn giao thành công
            </p>
          </div>
        </div>

        <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <span>Đảm bảo thanh toán</span>
          <span className="font-semibold text-emerald-700 flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5" /> TechHub Escrow
          </span>
        </div>
      </div>
    </div>
  );
}
