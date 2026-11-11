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

interface SellerVoucherRowProps {
  voucher: ShopVoucherResponse;
  onEdit: (voucher: ShopVoucherResponse) => void;
  onDelete: (voucher: ShopVoucherResponse) => void;
  onToggleStatus: (voucher: ShopVoucherResponse) => void;
  isUpdatingStatus?: boolean;
}

export function SellerVoucherRow({
  voucher,
  onEdit,
  onDelete,
  onToggleStatus,
  isUpdatingStatus = false,
}: SellerVoucherRowProps) {
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
        .padStart(2, "0")}/${d.getFullYear()} ${d
        .getHours()
        .toString()
        .padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;
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

  // Status Badge Metadata
  const getStatusMeta = (status: VoucherStatus) => {
    switch (status) {
      case "ONGOING":
        return {
          label: "Đang diễn ra",
          badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
          dotClass: "bg-emerald-500",
        };
      case "UPCOMING":
        return {
          label: "Sắp diễn ra",
          badgeClass: "bg-blue-50 text-[#0045cc] border-blue-200",
          dotClass: "bg-[#0045cc]",
        };
      case "EXPIRED":
        return {
          label: "Đã kết thúc",
          badgeClass: "bg-slate-100 text-slate-600 border-slate-200",
          dotClass: "bg-slate-400",
        };
      case "DISABLED":
        return {
          label: "Đã tạm dừng",
          badgeClass: "bg-amber-50 text-amber-700 border-amber-200",
          dotClass: "bg-amber-500",
        };
      default:
        return {
          label: status,
          badgeClass: "bg-slate-100 text-slate-700 border-slate-200",
          dotClass: "bg-slate-400",
        };
    }
  };

  const statusMeta = getStatusMeta(voucher.status);

  // Usage progress percentage
  const totalLimit = voucher.usageLimit || 0;
  const used = voucher.usedCount || 0;
  const usagePercent =
    totalLimit > 0 ? Math.min(100, Math.round((used / totalLimit) * 100)) : 0;

  return (
    <tr className="border-b border-slate-100 hover:bg-slate-50/70 transition-colors group">
      {/* 1. Mã & Tên Voucher */}
      <td className="py-3 px-3 overflow-hidden max-w-0">
        <div className="flex flex-col gap-1 min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="font-mono font-bold text-xs bg-blue-50 text-[#0045cc] border border-blue-200/80 px-2 py-0.5 rounded-lg flex items-center gap-1">
              <Tag className="w-3 h-3 shrink-0" />
              <span>{voucher.code}</span>
            </span>

            <button
              type="button"
              onClick={handleCopyCode}
              title="Sao chép mã"
              className="p-1 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
            >
              {copied ? (
                <Check className="w-3 h-3 text-emerald-600" />
              ) : (
                <Copy className="w-3 h-3" />
              )}
            </button>
          </div>

          <span className="text-xs font-semibold text-slate-900 truncate" title={voucher.name}>
            {voucher.name}
          </span>
        </div>
      </td>

      {/* 2. Mức giảm & Đơn tối thiểu */}
      <td className="py-3 px-2 whitespace-nowrap text-left">
        <div className="flex flex-col gap-0.5 leading-tight">
          <span className="font-headline font-bold text-xs text-slate-900 flex items-center gap-1">
            {voucher.discountType === "PERCENTAGE" ? (
              <>
                <Percent className="w-3 h-3 text-[#0045cc]" />
                <span>Giảm {voucher.discountValue}%</span>
              </>
            ) : (
              <>
                <DollarSign className="w-3 h-3 text-emerald-600" />
                <span>Giảm {formatVND(voucher.discountValue)}</span>
              </>
            )}
          </span>
          {voucher.discountType === "PERCENTAGE" &&
            voucher.maxDiscountAmount &&
            voucher.maxDiscountAmount > 0 && (
              <span className="text-[10px] text-slate-500">
                Tối đa: {formatVND(voucher.maxDiscountAmount)}
              </span>
            )}
          <span className="text-[10px] text-slate-400">
            Đơn tối thiểu: {voucher.minOrderAmount > 0 ? formatVND(voucher.minOrderAmount) : "0 đ"}
          </span>
        </div>
      </td>

      {/* 3. Phạm vi áp dụng */}
      <td className="py-3 px-2 whitespace-nowrap text-center">
        {voucher.appliesTo === "SHIPPING_FEE" ? (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-50 text-purple-700 border border-purple-200">
            <Truck className="w-3 h-3" />
            Vận chuyển
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-50 text-[#0045cc] border border-blue-200">
            <Package className="w-3 h-3" />
            Toàn bộ SP
          </span>
        )}
      </td>

      {/* 4. Lượt dùng & Tiến độ */}
      <td className="py-3 px-2 whitespace-nowrap text-center">
        <div className="flex flex-col items-center gap-1 max-w-[120px] mx-auto">
          <span className="text-xs font-bold text-slate-800">
            {used} / {totalLimit > 0 ? totalLimit : "∞"}
          </span>
          {totalLimit > 0 && (
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${
                  usagePercent >= 90
                    ? "bg-rose-500"
                    : usagePercent >= 70
                    ? "bg-amber-500"
                    : "bg-[#0045cc]"
                }`}
                style={{ width: `${usagePercent}%` }}
              />
            </div>
          )}
          <span className="text-[10px] text-slate-400">
            Tối đa {voucher.usageLimitPerUser} lượt/khách
          </span>
        </div>
      </td>

      {/* 5. Thời hạn & Trạng thái */}
      <td className="py-3 px-2 whitespace-nowrap text-left">
        <div className="flex flex-col gap-1 leading-tight">
          <span
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border w-fit ${statusMeta.badgeClass}`}
          >
            <span className={`w-1.5 h-1.5 rounded-full ${statusMeta.dotClass}`} />
            {statusMeta.label}
          </span>
          <div className="text-[10px] text-slate-500 flex flex-col gap-0.5 mt-0.5">
            <span>Bắt đầu: {formatDate(voucher.startDate)}</span>
            <span>Kết thúc: {formatDate(voucher.endDate)}</span>
          </div>
        </div>
      </td>

      {/* 6. Switch Bật / Tắt kích hoạt */}
      <td className="py-3 px-1 whitespace-nowrap text-center">
        <label
          className={`relative inline-flex items-center justify-center cursor-pointer ${
            isUpdatingStatus ? "opacity-60 cursor-wait" : ""
          }`}
          title={voucher.isActive ? "Đang bật (áp dụng)" : "Đang tạm dừng"}
        >
          <input
            type="checkbox"
            checked={voucher.isActive}
            disabled={isUpdatingStatus || voucher.status === "EXPIRED"}
            onChange={() => onToggleStatus(voucher)}
            className="sr-only peer"
          />
          <div className="w-7 h-4 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#0045cc]"></div>
        </label>
      </td>

      {/* 7. Thao tác */}
      <td className="py-3 px-3 whitespace-nowrap text-right">
        <div className="inline-flex items-center justify-end gap-1.5">
          {/* Edit button */}
          <button
            type="button"
            disabled={voucher.status === "EXPIRED"}
            onClick={() => onEdit(voucher)}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 hover:text-slate-900 transition-colors disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
            title="Chỉnh sửa voucher"
          >
            <Edit className="w-3 h-3 text-slate-500" />
            <span>Sửa</span>
          </button>

          {/* Delete button */}
          <button
            type="button"
            onClick={() => onDelete(voucher)}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors shrink-0 cursor-pointer"
            title="Xóa voucher"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </td>
    </tr>
  );
}
