"use client";

import React from "react";
import { Package, ChevronLeft, ChevronRight, Loader2, Plus } from "lucide-react";
import Link from "next/link";
import { ROUTES } from "@/lib/constants";
import type { SellerProductSummaryResponse } from "@/types/sellerProduct";
import { SellerProductRow } from "./SellerProductRow";

interface SellerProductsTableProps {
  products: SellerProductSummaryResponse[];
  isLoading: boolean;
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (newPage: number) => void;
  onPageSizeChange: (newSize: number) => void;
  onToggleVisibility: (productId: string, currentStatus: "PUBLISHED" | "UNPUBLISHED") => void;
  onSubmitApproval: (product: SellerProductSummaryResponse) => void;
  onDeleteProduct: (product: SellerProductSummaryResponse) => void;
  updatingProductId?: string | null;
}

export function SellerProductsTable({
  products,
  isLoading,
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  onPageSizeChange,
  onToggleVisibility,
  onSubmitApproval,
  onDeleteProduct,
  updatingProductId,
}: SellerProductsTableProps) {
  if (isLoading) {
    return (
      <div className="bg-white border border-slate-200/80 rounded-2xl p-8 flex flex-col items-center justify-center min-h-[350px]">
        <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
        <p className="text-xs text-slate-500 font-medium">Đang tải danh sách kho sản phẩm...</p>
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="bg-white border border-slate-200/80 rounded-2xl p-12 flex flex-col items-center justify-center text-center">
        <div className="w-16 h-16 rounded-2xl bg-blue-50 text-[#0045cc] flex items-center justify-center mb-4">
          <Package className="w-8 h-8" />
        </div>
        <h3 className="font-headline font-bold text-base text-slate-900">
          Không tìm thấy sản phẩm nào
        </h3>
        <p className="text-xs text-slate-500 max-w-sm mt-1.5 leading-relaxed">
          Không có sản phẩm nào khớp với bộ lọc hiện tại của bạn. Thử thay đổi từ khóa tìm kiếm hoặc đăng bán sản phẩm mới.
        </p>
        <Link
          href={ROUTES.SELLER.NEW_PRODUCT}
          className="mt-5 inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#0045cc] text-white text-xs font-semibold shadow-sm hover:bg-[#0038a8] transition-all"
        >
          <Plus className="w-4 h-4" />
          Đăng Bán Sản Phẩm Mới
        </Link>
      </div>
    );
  }

  const startRecord = page * size + 1;
  const endRecord = Math.min((page + 1) * size, totalElements);

  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden flex flex-col">
      {/* Table container with responsive handling & no ugly scrollbar */}
      <div className="w-full overflow-x-auto no-scrollbar">
        <table className="w-full min-w-[780px] text-left text-xs border-collapse table-fixed">
          <thead>
            <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold select-none">
              <th className="py-2.5 px-3 w-[36%]">Sản phẩm & Linh kiện</th>
              <th className="py-2.5 px-3 w-[13%] text-right">Giá bán (VND)</th>
              <th className="py-2.5 px-1.5 w-[7%] text-center">Tồn kho</th>
              <th className="py-2.5 px-1.5 w-[12%] text-center">Kiểm duyệt</th>
              <th className="py-2.5 px-1 w-[8%] text-center">Mở bán</th>
              <th className="py-2.5 px-3 w-[24%] text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <SellerProductRow
                key={product.id}
                product={product}
                onToggleVisibility={onToggleVisibility}
                onSubmitApproval={onSubmitApproval}
                onDeleteProduct={onDeleteProduct}
                isUpdatingVisibility={updatingProductId === product.id}
              />
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="p-4 bg-slate-50/50 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
        {/* Info */}
        <div className="flex items-center gap-3 text-slate-500">
          <span>
            Hiển thị <strong className="text-slate-900 font-semibold">{startRecord}-{endRecord}</strong> trong số{" "}
            <strong className="text-slate-900 font-semibold">{totalElements}</strong> sản phẩm
          </span>
          <span className="text-slate-300">|</span>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px]">Hiển thị:</span>
            <select
              value={size}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="h-7 px-2 bg-white border border-slate-200 rounded-lg text-slate-700 text-xs focus:outline-none focus:ring-1 focus:ring-[#0045cc]"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex items-center gap-1">
          <button
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0}
            className="w-8 h-8 rounded-lg border border-slate-200 bg-white flex items-center justify-center text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:hover:bg-white transition-colors"
            title="Trang trước"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          {/* Page indicator */}
          <div className="flex items-center gap-1 px-1">
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              let pageNum = i;
              if (totalPages > 5 && page > 2) {
                pageNum = page - 2 + i;
                if (pageNum >= totalPages) pageNum = totalPages - 5 + i;
              }

              const isCurrent = page === pageNum;

              return (
                <button
                  key={pageNum}
                  onClick={() => onPageChange(pageNum)}
                  className={`w-8 h-8 rounded-lg text-xs font-semibold flex items-center justify-center transition-all ${
                    isCurrent
                      ? "bg-[#0045cc] text-white shadow-sm"
                      : "text-slate-600 hover:bg-slate-100 border border-transparent"
                  }`}
                >
                  {pageNum + 1}
                </button>
              );
            })}
          </div>

          <button
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1}
            className="w-8 h-8 rounded-lg border border-slate-200 bg-white flex items-center justify-center text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:hover:bg-white transition-colors"
            title="Trang sau"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
