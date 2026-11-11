"use client";

import React from "react";
import { Ticket, ChevronLeft, ChevronRight, Loader2, Plus } from "lucide-react";
import type { ShopVoucherResponse } from "@/types/sellerVoucher";
import { SellerVoucherRow } from "./SellerVoucherRow";

interface SellerVouchersTableProps {
  vouchers: ShopVoucherResponse[];
  isLoading: boolean;
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (newPage: number) => void;
  onEdit: (voucher: ShopVoucherResponse) => void;
  onDelete: (voucher: ShopVoucherResponse) => void;
  onToggleStatus: (voucher: ShopVoucherResponse) => void;
  updatingVoucherId?: string | null;
  onOpenCreateModal: () => void;
}

export function SellerVouchersTable({
  vouchers,
  isLoading,
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  onEdit,
  onDelete,
  onToggleStatus,
  updatingVoucherId,
  onOpenCreateModal,
}: SellerVouchersTableProps) {
  if (isLoading) {
    return (
      <div className="bg-white border border-slate-200/80 rounded-2xl p-12 flex flex-col items-center justify-center min-h-[350px]">
        <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
        <p className="text-xs text-slate-500 font-medium">Đang tải danh sách mã giảm giá...</p>
      </div>
    );
  }

  if (vouchers.length === 0) {
    return (
      <div className="bg-white border border-slate-200/80 rounded-2xl p-12 flex flex-col items-center justify-center text-center">
        <div className="w-16 h-16 rounded-2xl bg-blue-50 text-[#0045cc] flex items-center justify-center mb-4">
          <Ticket className="w-8 h-8" />
        </div>
        <h3 className="font-headline font-bold text-base text-slate-900">
          Chưa có mã giảm giá nào
        </h3>
        <p className="text-xs text-slate-500 max-w-sm mt-1.5 leading-relaxed">
          Hiện tại chưa có mã voucher nào khớp với bộ lọc của bạn. Hãy tạo chương trình khuyến mãi đầu tiên để kích thích người mua!
        </p>
        <button
          type="button"
          onClick={onOpenCreateModal}
          className="mt-5 inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#0045cc] text-white text-xs font-semibold shadow-sm hover:bg-[#0038a8] transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Tạo Mã Giảm Giá Mới
        </button>
      </div>
    );
  }

  const startRecord = page * size + 1;
  const endRecord = Math.min((page + 1) * size, totalElements);

  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden flex flex-col">
      {/* Table container with responsive horizontal scroll handling */}
      <div className="w-full overflow-x-auto no-scrollbar">
        <table className="w-full min-w-[850px] text-left text-xs border-collapse table-fixed">
          <thead>
            <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold select-none">
              <th className="py-2.5 px-3 w-[26%]">Mã & Tên Voucher</th>
              <th className="py-2.5 px-2 w-[18%] text-left">Ưu đãi & Đơn tối thiểu</th>
              <th className="py-2.5 px-2 w-[11%] text-center">Phạm vi</th>
              <th className="py-2.5 px-2 w-[12%] text-center">Lượt dùng</th>
              <th className="py-2.5 px-2 w-[17%] text-left">Thời hạn & Trạng thái</th>
              <th className="py-2.5 px-1 w-[6%] text-center">Kích hoạt</th>
              <th className="py-2.5 px-3 w-[10%] text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {vouchers.map((voucher) => (
              <SellerVoucherRow
                key={voucher.id}
                voucher={voucher}
                onEdit={onEdit}
                onDelete={onDelete}
                onToggleStatus={onToggleStatus}
                isUpdatingStatus={updatingVoucherId === voucher.id}
              />
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="px-4 py-3 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 bg-slate-50/50">
        <span className="text-[11px] text-slate-500">
          Hiển thị{" "}
          <strong className="text-slate-800">
            {startRecord} - {endRecord}
          </strong>{" "}
          trên tổng số <strong className="text-slate-800">{totalElements}</strong> voucher
        </span>

        {totalPages > 1 && (
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
              className="p-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              title="Trang trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            {Array.from({ length: totalPages }).map((_, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => onPageChange(idx)}
                className={`min-w-7 h-7 px-2 rounded-lg text-xs font-semibold transition-all ${
                  page === idx
                    ? "bg-[#0045cc] text-white shadow-2xs"
                    : "border border-slate-200 bg-white hover:bg-slate-50 text-slate-700"
                }`}
              >
                {idx + 1}
              </button>
            ))}

            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              className="p-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              title="Trang sau"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
