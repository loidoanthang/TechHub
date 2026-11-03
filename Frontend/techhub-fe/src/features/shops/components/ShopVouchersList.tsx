"use client";

import { useState } from "react";
import { Ticket, Copy, Check, Clock, Sparkles } from "lucide-react";
import type { CartShopVoucherResponse } from "../types/shop";

interface ShopVouchersListProps {
  vouchers: CartShopVoucherResponse[];
  shopName: string;
}

export function ShopVouchersList({ vouchers, shopName }: ShopVouchersListProps) {
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  if (!vouchers || vouchers.length === 0) {
    return null;
  }

  const handleCopyCode = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(null), 2500);
    } catch {
      // Fallback
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(null), 2500);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  return (
    <section className="w-full flex flex-col gap-3.5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
            <Ticket className="w-4 h-4" />
          </div>
          <h2 className="text-base font-bold text-[#0b1c30] font-heading">
            Voucher Ưu Đãi Độc Quyền từ {shopName}
          </h2>
        </div>
        <span className="text-xs text-[#737688]">
          {vouchers.length} mã khả dụng
        </span>
      </div>

      {/* Vouchers Grid / Carousel */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {vouchers.map((voucher) => {
          const isCopied = copiedCode === voucher.code;
          const discountLabel =
            voucher.discountType === "PERCENTAGE"
              ? `Giảm ${voucher.discountValue}%`
              : `Giảm ${formatCurrency(voucher.discountValue)}`;

          const expiryText = voucher.endDate
            ? new Date(voucher.endDate).toLocaleDateString("vi-VN")
            : null;

          return (
            <div
              key={voucher.id}
              className="relative bg-gradient-to-r from-blue-50/70 via-white to-white rounded-2xl p-4 border border-[#0045cc]/20 shadow-sm hover:shadow-md transition-all flex items-center justify-between gap-3 overflow-hidden group"
            >
              {/* Left Accent Bar */}
              <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-[#0045cc]" />

              {/* Voucher Content */}
              <div className="flex flex-col gap-1 pl-2 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-extrabold text-[#0045cc] tracking-tight">
                    {discountLabel}
                  </span>
                  {voucher.maxDiscountAmount && voucher.discountType === "PERCENTAGE" && (
                    <span className="text-[10px] text-slate-500 font-medium">
                      (Tối đa {formatCurrency(voucher.maxDiscountAmount)})
                    </span>
                  )}
                </div>

                <p className="text-xs font-semibold text-[#0b1c30] truncate" title={voucher.name}>
                  {voucher.name || `Mã giảm giá ${voucher.code}`}
                </p>

                <div className="flex flex-wrap items-center gap-2 text-[11px] text-[#737688] mt-1">
                  <span>Đơn tối thiểu: <strong>{formatCurrency(voucher.minOrderAmount)}</strong></span>
                  {expiryText && (
                    <span className="flex items-center gap-1 text-[10px] text-slate-400">
                      <Clock className="w-3 h-3" />
                      HSD: {expiryText}
                    </span>
                  )}
                </div>
              </div>

              {/* Right: Code & Copy Action */}
              <div className="flex flex-col items-end gap-1.5 shrink-0">
                <span className="px-2.5 py-1 rounded-lg bg-[#eff4ff] text-[#0045cc] font-mono text-xs font-bold border border-[#0045cc]/20 tracking-wider">
                  {voucher.code}
                </span>

                <button
                  type="button"
                  onClick={() => handleCopyCode(voucher.code)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer active:scale-95 ${
                    isCopied
                      ? "bg-emerald-600 text-white"
                      : "bg-[#0045cc] hover:bg-[#0d5bff] text-white shadow-sm"
                  }`}
                >
                  {isCopied ? (
                    <>
                      <Check className="w-3.5 h-3.5" />
                      <span>Đã chép</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>Sao chép</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
