"use client";

import { useState, useEffect } from "react";
import { X, Tag, Check, AlertCircle, Loader2, Sparkles, Clock, HelpCircle, Truck } from "lucide-react";
import type { CartPlatformVoucherResponse } from "@/types/voucher";

interface VoucherSelectModalProps {
  isOpen: boolean;
  onClose: () => void;
  vouchers: CartPlatformVoucherResponse[];
  isLoading?: boolean;
  selectedVoucher: CartPlatformVoucherResponse | null;
  onApplyVoucher: (voucher: CartPlatformVoucherResponse | null) => void;
  orderAmount: number;
}

export function VoucherSelectModal({
  isOpen,
  onClose,
  vouchers,
  isLoading,
  selectedVoucher,
  onApplyVoucher,
  orderAmount,
}: VoucherSelectModalProps) {
  const [inputCode, setInputCode] = useState("");
  const [tempSelectedId, setTempSelectedId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Đồng bộ voucher đang chọn vào state tạm của modal
  useEffect(() => {
    if (isOpen) {
      setTempSelectedId(selectedVoucher?.id || null);
      setInputCode("");
      setErrorMessage(null);
    }
  }, [isOpen, selectedVoucher]);

  if (!isOpen) return null;

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  const handleManualApply = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    const code = inputCode.trim().toUpperCase();
    if (!code) return;

    const matched = vouchers.find(
      (v) => v.code.toUpperCase() === code
    );

    if (!matched) {
      setErrorMessage(`Mã giảm giá "${code}" không tồn tại hoặc đã hết hạn.`);
      return;
    }

    if (!matched.isEligible) {
      const reason =
        matched.ineligibilityReason ||
        (matched.upsellRemainingAmount
          ? `Đơn hàng chưa đạt mức tối thiểu. Cần mua thêm ${formatPrice(matched.upsellRemainingAmount)}.`
          : "Đơn hàng của bạn chưa đủ điều kiện áp dụng mã này.");
      setErrorMessage(reason);
      return;
    }

    setTempSelectedId(matched.id);
    setInputCode("");
  };

  const handleConfirm = () => {
    if (!tempSelectedId) {
      onApplyVoucher(null);
    } else {
      const matched = vouchers.find((v) => v.id === tempSelectedId);
      onApplyVoucher(matched || null);
    }
    onClose();
  };

  const eligibleVouchers = vouchers.filter((v) => v.isEligible);
  const ineligibleVouchers = vouchers.filter((v) => !v.isEligible);

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="fixed inset-0" onClick={onClose} aria-hidden="true" />

      <div className="relative z-10 w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-5 sm:p-6 flex flex-col gap-4 max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
              <Tag className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900 font-heading">
                Chọn TechHub Voucher
              </h2>
              <p className="text-xs text-slate-500">
                Áp dụng mã giảm giá toàn sàn cho đơn hàng của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Input Redeem Code */}
        <form onSubmit={handleManualApply} className="flex gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              value={inputCode}
              onChange={(e) => {
                setInputCode(e.target.value.toUpperCase());
                if (errorMessage) setErrorMessage(null);
              }}
              placeholder="Nhập mã voucher (VD: TECHHUB50)"
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-mono font-bold text-slate-900 placeholder:font-sans placeholder:font-normal placeholder:text-slate-400 focus:outline-none focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15 uppercase tracking-wide"
            />
          </div>
          <button
            type="submit"
            disabled={!inputCode.trim()}
            className="px-4 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shrink-0"
          >
            Áp dụng
          </button>
        </form>

        {/* Error message */}
        {errorMessage && (
          <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Voucher List Content */}
        <div className="flex-1 overflow-y-auto pr-1 flex flex-col gap-3 min-h-[220px] max-h-[50vh]">
          {isLoading ? (
            <div className="py-12 flex flex-col items-center justify-center gap-2 text-slate-400">
              <Loader2 className="w-6 h-6 animate-spin text-[#0045cc]" />
              <span className="text-xs">Đang tải mã giảm giá...</span>
            </div>
          ) : vouchers.length === 0 ? (
            <div className="py-12 flex flex-col items-center justify-center gap-2 text-center text-slate-400">
              <Tag className="w-8 h-8 text-slate-300" />
              <p className="text-xs font-medium text-slate-600">
                Hiện tại chưa có mã giảm giá nào trên hệ thống
              </p>
              <span className="text-[11px] text-slate-400">
                Hãy theo dõi các chương trình ưu đãi sắp tới của TechHub!
              </span>
            </div>
          ) : (
            <>
              {/* 1. Eligible Vouchers */}
              {eligibleVouchers.length > 0 && (
                <div className="flex flex-col gap-2.5">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                    Mã giảm giá khả dụng ({eligibleVouchers.length})
                  </span>
                  {eligibleVouchers.map((v) => {
                    const isSelected = tempSelectedId === v.id;
                    const discountLabel =
                      v.discountType === "PERCENTAGE"
                        ? `Giảm ${v.discountValue}%`
                        : `Giảm ${formatPrice(v.discountValue)}`;

                    return (
                      <div
                        key={v.id}
                        onClick={() => setTempSelectedId(isSelected ? null : v.id)}
                        className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center gap-3 relative ${
                          isSelected
                            ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/20 shadow-xs"
                            : "border-slate-200 hover:border-slate-300 bg-white"
                        }`}
                      >
                        {/* Left Badge */}
                        <div
                          className={`w-14 h-14 rounded-xl flex flex-col items-center justify-center shrink-0 p-1 text-center shadow-xs text-white ${
                            v.appliesTo === "SHIPPING_FEE"
                              ? "bg-gradient-to-br from-emerald-600 to-teal-500"
                              : "bg-gradient-to-br from-[#0045cc] to-[#0d5bff]"
                          }`}
                        >
                          {v.appliesTo === "SHIPPING_FEE" ? (
                            <Truck className="w-4 h-4 text-emerald-100 mb-0.5" />
                          ) : (
                            <Sparkles className="w-3.5 h-3.5 text-amber-300 mb-0.5" />
                          )}
                          <span className="text-[10px] font-bold leading-tight line-clamp-1">
                            {discountLabel}
                          </span>
                        </div>

                        {/* Middle Info */}
                        <div className="flex-1 min-w-0 flex flex-col gap-0.5">
                          <div className="flex items-center gap-2">
                            <span
                              className={`font-mono text-xs font-bold px-2 py-0.5 rounded border ${
                                v.appliesTo === "SHIPPING_FEE"
                                  ? "text-emerald-700 bg-emerald-50 border-emerald-200"
                                  : "text-[#0045cc] bg-blue-50 border-blue-100"
                              }`}
                            >
                              {v.code}
                            </span>
                            <span className="text-xs font-bold text-slate-800 truncate">
                              {v.name}
                            </span>
                          </div>

                          <p className="text-[11px] text-slate-500 mt-0.5">
                            Đơn tối thiểu {formatPrice(v.minOrderAmount)}
                            {v.maxDiscountAmount
                              ? ` • Giảm tối đa ${formatPrice(v.maxDiscountAmount)}`
                              : ""}
                          </p>

                          <div className="flex items-center gap-1 text-[10px] text-slate-400 mt-1">
                            <Clock className="w-3 h-3 text-slate-400" />
                            <span>HSD: {formatDate(v.endDate)}</span>
                            {v.estimatedDiscount > 0 && (
                              <span className="ml-auto font-bold text-emerald-600 text-xs">
                                Tiết kiệm {formatPrice(v.estimatedDiscount)}
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Radio Indicator */}
                        <div
                          className={`w-5 h-5 rounded-full border flex items-center justify-center shrink-0 transition-all ${
                            isSelected
                              ? "border-[#0045cc] bg-[#0045cc] text-white"
                              : "border-slate-300 bg-white"
                          }`}
                        >
                          {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* 2. Ineligible Vouchers */}
              {ineligibleVouchers.length > 0 && (
                <div className="flex flex-col gap-2.5 mt-2">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Chưa đủ điều kiện ({ineligibleVouchers.length})
                  </span>
                  {ineligibleVouchers.map((v) => {
                    const discountLabel =
                      v.discountType === "PERCENTAGE"
                        ? `Giảm ${v.discountValue}%`
                        : `Giảm ${formatPrice(v.discountValue)}`;

                    return (
                      <div
                        key={v.id}
                        className="p-3.5 rounded-2xl border border-slate-200/70 bg-slate-50/70 opacity-60 flex items-center gap-3 relative cursor-not-allowed select-none"
                      >
                        {/* Left Badge Muted */}
                        <div className="w-14 h-14 rounded-xl bg-slate-300 text-slate-600 flex flex-col items-center justify-center shrink-0 p-1 text-center">
                          <Tag className="w-3.5 h-3.5 mb-0.5 text-slate-400" />
                          <span className="text-[11px] font-bold leading-tight line-clamp-1">
                            {discountLabel}
                          </span>
                        </div>

                        {/* Middle Info */}
                        <div className="flex-1 min-w-0 flex flex-col gap-0.5">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs font-bold text-slate-600 bg-slate-200 px-2 py-0.5 rounded">
                              {v.code}
                            </span>
                            <span className="text-xs font-semibold text-slate-600 truncate">
                              {v.name}
                            </span>
                          </div>

                          <p className="text-[11px] text-slate-400 mt-0.5">
                            Đơn tối thiểu {formatPrice(v.minOrderAmount)}
                          </p>

                          {/* Upsell Alert */}
                          <div className="mt-1 text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded inline-flex items-center gap-1 self-start border border-amber-200/50">
                            <HelpCircle className="w-3 h-3 text-amber-600 shrink-0" />
                            <span>
                              {v.upsellRemainingAmount
                                ? `Mua thêm ${formatPrice(v.upsellRemainingAmount)} để áp dụng mã`
                                : v.ineligibilityReason || "Chưa đạt điều kiện áp dụng"}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer Actions */}
        <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => setTempSelectedId(null)}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors cursor-pointer"
          >
            Bỏ chọn
          </button>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              className="px-5 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold shadow-sm transition-all cursor-pointer"
            >
              Xác nhận
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
