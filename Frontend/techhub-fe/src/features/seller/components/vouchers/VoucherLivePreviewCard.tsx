"use client";

import React from "react";
import { Ticket, Calendar, ShieldCheck, Tag, Sparkles } from "lucide-react";
import type { DiscountType, VoucherAppliesTo } from "@/types/sellerVoucher";

interface VoucherLivePreviewCardProps {
  code: string;
  name: string;
  appliesTo: VoucherAppliesTo;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number | null;
  startDate?: string;
  endDate?: string;
  shopName?: string;
}

const VND_CURRENCY_FORMATTER = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
});

const formatVND = (val: number) => VND_CURRENCY_FORMATTER.format(val);

export function VoucherLivePreviewCard({
  code,
  name,
  appliesTo,
  discountType,
  discountValue,
  minOrderAmount = 0,
  maxDiscountAmount,
  startDate,
  endDate,
  shopName = "Gian Hàng Chính Hãng",
}: VoucherLivePreviewCardProps) {

  const formatDate = (isoString?: string) => {
    if (!isoString) return "--/--/----";
    try {
      const d = new Date(isoString);
      return `${d.getDate().toString().padStart(2, "0")}/${(d.getMonth() + 1)
        .toString()
        .padStart(2, "0")}/${d.getFullYear()} ${d
        .getHours()
        .toString()
        .padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;
    } catch {
      return "--/--/----";
    }
  };

  const discountBadgeText =
    discountType === "PERCENTAGE"
      ? `GIẢM ${discountValue > 0 ? discountValue : 0}%`
      : `GIẢM ${discountValue > 0 ? formatVND(discountValue) : "0 đ"}`;

  return (
    <div className="flex flex-col gap-2.5 w-full">
      <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
        <span className="flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5 text-amber-500" />
          Xem trước hiển thị (Live Preview)
        </span>
        <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-medium">
          Mô phỏng người mua
        </span>
      </div>

      {/* Modern Coupon Card */}
      <div className="relative overflow-hidden bg-gradient-to-br from-[#0045cc] to-[#002b80] rounded-2xl p-4 text-white shadow-lg border border-blue-400/20">
        {/* Decorative background shapes */}
        <div className="absolute -right-8 -top-8 w-28 h-28 rounded-full bg-white/10 blur-xl pointer-events-none" />
        <div className="absolute -left-6 -bottom-6 w-24 h-24 rounded-full bg-blue-400/20 blur-lg pointer-events-none" />

        <div className="relative z-10 flex flex-col gap-3">
          {/* Top Shop info & Scope badge */}
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 min-w-0">
              <ShieldCheck className="w-3.5 h-3.5 text-blue-200 shrink-0" />
              <span className="text-[11px] font-semibold text-blue-100 truncate">
                {shopName}
              </span>
            </div>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold tracking-wide uppercase bg-white/20 text-white backdrop-blur-xs shrink-0">
              {appliesTo === "SHIPPING_FEE" ? "Phí vận chuyển" : "Toàn bộ sản phẩm"}
            </span>
          </div>

          {/* Big Discount Headline */}
          <div className="flex items-baseline gap-2">
            <h4 className="font-headline font-black text-2xl tracking-tight text-white drop-shadow-xs">
              {discountBadgeText}
            </h4>
            {discountType === "PERCENTAGE" && maxDiscountAmount && maxDiscountAmount > 0 ? (
              <span className="text-[11px] font-medium text-blue-200">
                (Tối đa {formatVND(maxDiscountAmount)})
              </span>
            ) : null}
          </div>

          {/* Voucher Title */}
          <p className="text-xs font-medium text-blue-50 line-clamp-2 leading-relaxed">
            {name || "Tên chương trình khuyến mãi sẽ hiển thị tại đây"}
          </p>

          {/* Dotted separator line */}
          <div className="relative my-0.5">
            <div className="border-t border-dashed border-white/30" />
            <div className="absolute -left-6 -top-2 w-4 h-4 rounded-full bg-slate-900/60" />
            <div className="absolute -right-6 -top-2 w-4 h-4 rounded-full bg-slate-900/60" />
          </div>

          {/* Code & Minimum condition */}
          <div className="flex items-center justify-between gap-2 pt-0.5">
            <div className="flex items-center gap-1.5 bg-black/25 px-2.5 py-1 rounded-lg border border-white/15">
              <Tag className="w-3 h-3 text-amber-300" />
              <span className="font-mono font-bold text-xs tracking-wider text-amber-300 uppercase">
                {code ? code.toUpperCase() : "MÃVOUCHER"}
              </span>
            </div>

            <div className="text-right">
              <span className="text-[10px] text-blue-200 block">Đơn tối thiểu</span>
              <span className="text-xs font-bold text-white">
                {minOrderAmount > 0 ? formatVND(minOrderAmount) : "0 đ"}
              </span>
            </div>
          </div>

          {/* Expiry Date */}
          <div className="flex items-center gap-1.5 text-[10px] text-blue-200/90 pt-1 border-t border-white/10">
            <Calendar className="w-3 h-3 text-blue-300 shrink-0" />
            <span>
              HSD: {startDate ? formatDate(startDate) : "--"} đến {endDate ? formatDate(endDate) : "--"}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
