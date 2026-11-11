"use client";

import React from "react";
import Link from "next/link";
import {
  ChevronRight,
  Plus,
  Ticket,
  TrendingUp,
  Sparkles,
  Percent,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";
import type { ShopVoucherResponse } from "@/types/sellerVoucher";

interface SellerVouchersHeaderProps {
  vouchers: ShopVoucherResponse[];
  onOpenCreateModal: () => void;
}

export function SellerVouchersHeader({
  vouchers,
  onOpenCreateModal,
}: SellerVouchersHeaderProps) {
  // Compute Quick KPI Metrics
  const activeCount = vouchers.filter((v) => v.status === "ONGOING").length;
  const totalUsed = vouchers.reduce((acc, v) => acc + (v.usedCount || 0), 0);
  const totalLimit = vouchers.reduce((acc, v) => acc + (v.usageLimit || 0), 0);
  const remainingUsage = Math.max(0, totalLimit - totalUsed);

  return (
    <div className="flex flex-col gap-5 w-full">
      {/* Breadcrumb & Main Action Bar */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link
            href={ROUTES.SELLER.DASHBOARD}
            className="hover:text-[#0045cc] transition-colors"
          >
            Kênh Người Bán
          </Link>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-slate-900 font-semibold">Mã giảm giá Shop</span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="font-headline font-bold text-xl text-slate-900 tracking-tight flex items-center gap-2">
              <Ticket className="w-6 h-6 text-[#0045cc]" />
              <span>Quản Lý Mã Giảm Giá Shop</span>
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Thiết lập mã coupon khuyến mãi kích cầu mua sắm và gia tăng tỷ lệ chốt đơn cho gian hàng
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenCreateModal}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-bold transition-all shadow-sm hover:shadow-md shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo Voucher Mới</span>
          </button>
        </div>
      </div>

      {/* 3 Bento KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
        {/* Card 1: Active Vouchers */}
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-2xs flex items-center justify-between">
          <div className="flex flex-col gap-1">
            <span className="text-[11px] font-semibold text-slate-500">
              Voucher đang chạy
            </span>
            <div className="flex items-baseline gap-1.5">
              <span className="font-headline font-black text-2xl text-slate-900">
                {activeCount}
              </span>
              <span className="text-[10px] text-emerald-600 font-semibold">
                chương trình
              </span>
            </div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-[#0045cc] flex items-center justify-center">
            <Ticket className="w-5 h-5" />
          </div>
        </div>

        {/* Card 2: Total Used */}
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-2xs flex items-center justify-between">
          <div className="flex flex-col gap-1">
            <span className="text-[11px] font-semibold text-slate-500">
              Tổng lượt đã dùng
            </span>
            <div className="flex items-baseline gap-1.5">
              <span className="font-headline font-black text-2xl text-slate-900">
                {totalUsed}
              </span>
              <span className="text-[10px] text-slate-500 font-medium">
                lượt áp dụng
              </span>
            </div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <TrendingUp className="w-5 h-5" />
          </div>
        </div>

        {/* Card 3: Remaining Usage */}
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-2xs flex items-center justify-between">
          <div className="flex flex-col gap-1">
            <span className="text-[11px] font-semibold text-slate-500">
              Lượt dùng còn lại
            </span>
            <div className="flex items-baseline gap-1.5">
              <span className="font-headline font-black text-2xl text-slate-900">
                {remainingUsage > 0 ? remainingUsage : "Vô hạn"}
              </span>
              <span className="text-[10px] text-blue-600 font-medium">
                lượt sẵn sàng
              </span>
            </div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
            <Sparkles className="w-5 h-5" />
          </div>
        </div>
      </div>
    </div>
  );
}
