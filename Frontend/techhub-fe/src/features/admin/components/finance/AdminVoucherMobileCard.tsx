"use client";

import React, { useState } from "react";
import { Ticket, Copy, Check, Edit, Trash2 } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { AdminVoucherResponse } from "@/types/adminFinance";

interface AdminVoucherMobileCardProps {
  voucher: AdminVoucherResponse;
  onEdit: (voucher: AdminVoucherResponse) => void;
  onDelete: (voucher: AdminVoucherResponse) => void;
  onToggleStatus: (voucher: AdminVoucherResponse) => void;
  isUpdatingStatus?: boolean;
}

export function AdminVoucherMobileCard({
  voucher,
  onEdit,
  onDelete,
  onToggleStatus,
  isUpdatingStatus = false,
}: AdminVoucherMobileCardProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(voucher.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isPlatform = voucher.type === "PLATFORM";

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Code, Badges */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-1.5 flex-wrap min-w-0">
          <span className="font-mono font-bold text-xs bg-blue-50 text-[#0045cc] px-2 py-0.5 rounded-lg border border-blue-200 tracking-wider">
            {voucher.code}
          </span>
          <button
            type="button"
            onClick={handleCopy}
            className="p-1 rounded-md hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
            title="Sao chép mã"
          >
            {copied ? (
              <Check className="w-3.5 h-3.5 text-emerald-600" />
            ) : (
              <Copy className="w-3.5 h-3.5" />
            )}
          </button>
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">
            {voucher.type}
          </span>
        </div>

        <button
          type="button"
          disabled={!isPlatform || isUpdatingStatus}
          onClick={() => onToggleStatus(voucher)}
          className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold border shrink-0 ${
            voucher.isActive
              ? "bg-emerald-50 text-emerald-700 border-emerald-200"
              : "bg-slate-100 text-slate-600 border-slate-200"
          } ${isPlatform ? "cursor-pointer" : "opacity-80"}`}
        >
          {voucher.status}
        </button>
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
              : `Giảm ${formatPrice(voucher.discountValue)}`}
          </span>
          {voucher.minOrderAmount > 0 && (
            <span className="text-[11px] text-slate-500">
              (Đơn từ {formatPrice(voucher.minOrderAmount)})
            </span>
          )}
        </div>
      </div>

      {/* 3. Scope & Usage */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1 text-xs">
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">Phạm vi áp dụng:</span>
          <span className="font-medium text-slate-900">
            {voucher.appliesTo === "SHIPPING_FEE" ? "Phí vận chuyển (FreeShip)" : "Tiền hàng"}
          </span>
        </div>
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">Lượt sử dụng:</span>
          <span className="font-mono font-medium text-slate-800">
            {voucher.usedCount} / {voucher.usageLimit ?? "Không giới hạn"}
          </span>
        </div>
        {!isPlatform && voucher.shopName && (
          <div className="flex items-center justify-between text-slate-700 pt-1 border-t border-slate-200/60">
            <span className="text-slate-500 text-[11px]">Gian hàng:</span>
            <span className="font-medium text-slate-900 truncate max-w-[180px]">
              {voucher.shopName}
            </span>
          </div>
        )}
      </div>

      {/* 4. Actions for PLATFORM vouchers */}
      {isPlatform && (
        <div className="flex items-center justify-end gap-1.5 pt-1 border-t border-slate-100 text-xs">
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
      )}
    </div>
  );
}
