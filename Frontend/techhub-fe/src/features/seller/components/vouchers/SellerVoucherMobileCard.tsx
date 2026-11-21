"use client";

import React, { useState } from "react";
import {
  Tag,
  Copy,
  Check,
  Edit,
  Trash2,
  Calendar,
  Package,
  Truck,
  Percent,
  DollarSign,
  Loader2,
} from "lucide-react";
import type { ShopVoucherResponse, VoucherStatus } from "@/types/sellerVoucher";

interface SellerVoucherMobileCardProps {
  voucher: ShopVoucherResponse;
  onEdit: (voucher: ShopVoucherResponse) => void;
  onDelete: (voucher: ShopVoucherResponse) => void;
  onToggleStatus: (voucher: ShopVoucherResponse) => void;
  isUpdatingStatus?: boolean;
}

export function SellerVoucherMobileCard({
  voucher,
  onEdit,
  onDelete,
  onToggleStatus,
  isUpdatingStatus = false,
}: SellerVoucherMobileCardProps) {
  const [copied, setCopied] = useState(false);

  const formatVND = (val: number) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(val);

  const formatDate = (isoString: string) => {
    try {
      const d = new Date(isoString);
      return `${d.getDate().toString().padStart(2, "0")}/${(d.getMonth() + 1)
        .toString()
        .padStart(2, "0")}/${d.getFullYear()}`;
    } catch {
      return isoString;
    }
  };

  const handleCopyCode = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(voucher.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getStatusMeta = (status: VoucherStatus) => {
    switch (status) {
      case "ONGOING":
        return {
          label: "Đang diễn ra",
          badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
        };
      case "UPCOMING":
        return {
          label: "Sắp diễn ra",
          badgeClass: "bg-blue-50 text-[#0045cc] border-blue-200",
        };
      case "EXPIRED":
        return {
          label: "Đã kết thúc",
          badgeClass: "bg-slate-100 text-slate-600 border-slate-200",
        };
      case "DISABLED":
        return {
          label: "Đã tạm dừng",
          badgeClass: "bg-amber-50 text-amber-700 border-amber-200",
        };
      default:
        return {
          label: status,
          badgeClass: "bg-slate-100 text-slate-700 border-slate-200",
        };
    }
  };

  const statusMeta = getStatusMeta(voucher.status);

  return (
    <div className="p-4 space-y-3 bg-white">
      {/* 1. Header: Code, Copy & Status */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="font-mono font-bold text-xs bg-slate-100 text-[#0045cc] px-2 py-0.5 rounded-lg border border-slate-200 tracking-wider">
            {voucher.code}
          </span>
          <button
            type="button"
            onClick={handleCopyCode}
            className="p-1 rounded-md hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
            title="Sao chép mã"
          >
            {copied ? (
              <Check className="w-3.5 h-3.5 text-emerald-600" />
            ) : (
              <Copy className="w-3.5 h-3.5" />
            )}
          </button>
        </div>

        <span
          className={`px-2 py-0.5 rounded-full text-[10px] font-bold border shrink-0 ${statusMeta.badgeClass}`}
        >
          {statusMeta.label}
        </span>
      </div>

      {/* 2. Voucher Name & Discount */}
      <div>
        <h4 className="text-xs font-bold text-slate-900 line-clamp-1">
          {voucher.name}
        </h4>
        <div className="mt-1 flex items-center gap-2 flex-wrap text-xs">
          <span className="font-bold text-[#0045cc]">
            {voucher.discountType === "PERCENTAGE"
              ? `Giảm ${voucher.discountValue}%`
              : `Giảm ${formatVND(voucher.discountValue)}`}
          </span>
          {voucher.maxDiscountAmount && (
            <span className="text-[11px] text-slate-500">
              (Tối đa {formatVND(voucher.maxDiscountAmount)})
            </span>
          )}
        </div>
      </div>

      {/* 3. Conditions & Usage info */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1.5 text-[11px] text-slate-600">
        <div className="flex items-center justify-between">
          <span className="text-slate-500">Đơn tối thiểu:</span>
          <span className="font-medium text-slate-800">
            {formatVND(voucher.minOrderAmount)}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-slate-500">Đã dùng:</span>
          <span className="font-medium text-slate-800">
            {voucher.usedCount}
            {voucher.usageLimit ? ` / ${voucher.usageLimit}` : " (Không giới hạn)"}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-slate-500">Hạn áp dụng:</span>
          <span className="font-medium text-slate-800">
            {formatDate(voucher.startDate)} - {formatDate(voucher.endDate)}
          </span>
        </div>
      </div>

      {/* 4. Action bar */}
      <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-100">
        <label className="flex items-center gap-2 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={voucher.isActive}
            onChange={() => onToggleStatus(voucher)}
            disabled={isUpdatingStatus}
            className="w-4 h-4 rounded text-[#0045cc] focus:ring-[#0045cc] border-slate-300"
          />
          <span className="text-xs text-slate-600 font-medium">
            {voucher.isActive ? "Bật" : "Tắt"}
          </span>
        </label>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => onEdit(voucher)}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:text-[#0045cc] hover:bg-blue-50 transition-colors"
            title="Chỉnh sửa voucher"
          >
            <Edit className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            onClick={() => onDelete(voucher)}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:text-rose-600 hover:bg-rose-50 transition-colors"
            title="Xóa voucher"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
