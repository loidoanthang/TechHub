"use client";

import React from "react";
import Link from "next/link";
import { Wallet, ShieldCheck, ArrowUpRight, Clock, Info } from "lucide-react";
import { formatPrice } from "@/lib/utils";

interface DashboardWalletCardProps {
  walletBalance?: number;
  pendingPayoutAmount?: number;
  isLoading?: boolean;
}

export function DashboardWalletCard({
  walletBalance = 0,
  pendingPayoutAmount = 0,
  isLoading,
}: DashboardWalletCardProps) {
  return (
    <div className="bg-gradient-to-br from-[#0037a3] to-[#0045cc] rounded-3xl p-6 text-white shadow-md flex flex-col justify-between gap-5 relative overflow-hidden">
      {/* Decorative background glow */}
      <div className="absolute -top-12 -right-12 w-36 h-36 bg-[#0d5bff]/40 rounded-full blur-2xl pointer-events-none" />

      {/* Top: Wallet Header & Shield Badge */}
      <div className="flex items-center justify-between z-10">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-2xl bg-white/10 text-white backdrop-blur-md flex items-center justify-center border border-white/20">
            <Wallet className="w-4 h-4 text-emerald-300" />
          </div>
          <div>
            <h3 className="font-heading text-sm font-bold text-white">
              Tài Chính &amp; Ví Shop
            </h3>
            <span className="text-[10px] text-blue-200">Bảo chứng Escrow TechHub</span>
          </div>
        </div>
        <div className="flex items-center gap-1 bg-white/15 px-2.5 py-1 rounded-full text-[10px] font-bold text-emerald-300 border border-white/20">
          <ShieldCheck className="w-3 h-3" />
          <span>Bảo mật</span>
        </div>
      </div>

      {/* Center: Balances */}
      <div className="flex flex-col gap-3.5 z-10">
        {/* Available Balance */}
        <div className="flex flex-col gap-0.5">
          <span className="text-xs text-blue-200 font-medium">Số dư khả dụng</span>
          {isLoading ? (
            <div className="h-8 w-36 bg-white/20 animate-pulse rounded-md" />
          ) : (
            <span className="font-heading text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              {formatPrice(walletBalance)}
            </span>
          )}
        </div>

        {/* Pending Escrow */}
        <div className="flex items-center justify-between pt-2 border-t border-white/15 text-xs">
          <div className="flex items-center gap-1.5 text-blue-200">
            <Clock className="w-3.5 h-3.5 text-amber-300" />
            <span>Chờ quyết toán (Escrow):</span>
          </div>
          {isLoading ? (
            <div className="h-4 w-20 bg-white/20 animate-pulse rounded-md" />
          ) : (
            <span className="font-mono font-bold text-amber-300">
              {formatPrice(pendingPayoutAmount)}
            </span>
          )}
        </div>
      </div>

      {/* Bottom Note & CTA */}
      <div className="z-10 flex flex-col gap-3">
        <p className="text-[11px] text-blue-100/90 leading-relaxed bg-black/15 p-2.5 rounded-xl border border-white/10 flex items-start gap-1.5">
          <Info className="w-3.5 h-3.5 text-blue-200 shrink-0 mt-0.5" />
          <span>
            Doanh thu đơn hàng sẽ tự động giải ngân vào ví khả dụng sau khi người mua xác nhận hoặc hoàn tất kỳ đối soát khiếu nại.
          </span>
        </p>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className="flex-1 py-2 px-3 rounded-xl bg-white text-[#0045cc] hover:bg-blue-50 text-xs font-bold transition-all shadow-xs text-center"
            onClick={() => alert("Tính năng rút tiền ví đối tác đang được liên kết tự động qua cổng thanh toán.")}
          >
            Yêu cầu rút tiền
          </button>
          <Link
            href="/seller/orders"
            className="py-2 px-3 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-all border border-white/20 flex items-center justify-center gap-1"
          >
            <span>Chi tiết</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}
