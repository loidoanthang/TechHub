"use client";

import React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

export interface PaginationProps {
  page: number; // 0-indexed
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (newPage: number) => void;
  onSizeChange?: (newSize: number) => void;
  sizeOptions?: number[];
  variant?: "top-mini" | "bottom-full";
  isLoading?: boolean;
  className?: string;
}

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
  sizeOptions = [5, 10, 25, 50],
  variant = "bottom-full",
  isLoading = false,
  className = "",
}: PaginationProps) {
  const safeTotalPages = Math.max(1, totalPages || 1);
  const startRecord = totalElements === 0 ? 0 : page * size + 1;
  const endRecord = Math.min((page + 1) * size, totalElements);
  const isFirstPage = page === 0;
  const isLastPage = page >= safeTotalPages - 1;

  if (variant === "top-mini") {
    return (
      <div
        className={`flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1 text-xs text-slate-600 shrink-0 ${className}`}
      >
        <span className="font-mono text-[11px] font-semibold text-slate-500 hidden sm:inline">
          {startRecord}-{endRecord} / {totalElements}
        </span>
        <span className="text-slate-300 hidden sm:inline">|</span>
        <span className="text-[11px] font-bold text-slate-800">
          {page + 1}/{safeTotalPages}
        </span>
        <div className="flex items-center gap-0.5">
          <button
            type="button"
            disabled={isFirstPage || isLoading}
            onClick={() => onPageChange(Math.max(0, page - 1))}
            title="Trang trước"
            className="p-1 rounded-md hover:bg-slate-200/80 disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer disabled:cursor-not-allowed transition-colors text-slate-700"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            disabled={isLastPage || isLoading}
            onClick={() => onPageChange(page + 1)}
            title="Trang sau"
            className="p-1 rounded-md hover:bg-slate-200/80 disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer disabled:cursor-not-allowed transition-colors text-slate-700"
          >
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`px-4 py-2.5 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-600 bg-white ${className}`}
    >
      <div className="flex items-center gap-3 flex-wrap">
        <span>
          Hiển thị{" "}
          <strong className="text-slate-900 font-bold">
            {startRecord} - {endRecord}
          </strong>{" "}
          trong số{" "}
          <strong className="text-slate-900 font-bold">{totalElements}</strong>{" "}
          mục
        </span>

        {onSizeChange && (
          <div className="flex items-center gap-1.5 border-l border-slate-200 pl-3">
            <span className="text-[11px] text-slate-400">Hiển thị:</span>
            <select
              value={size}
              onChange={(e) => {
                onSizeChange(Number(e.target.value));
                onPageChange(0);
              }}
              className="h-7 px-2 bg-white border border-slate-200 rounded-lg text-slate-700 text-xs font-semibold focus:outline-none focus:border-[#0045cc]"
            >
              {sizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt} / trang
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="flex items-center gap-2">
        <span className="text-slate-500 text-[11px]">
          Trang <strong>{page + 1}</strong> / {safeTotalPages}
        </span>
        <div className="flex items-center gap-1">
          <button
            type="button"
            disabled={isFirstPage || isLoading}
            onClick={() => onPageChange(Math.max(0, page - 1))}
            className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:hover:bg-transparent disabled:cursor-not-allowed cursor-pointer transition-colors text-slate-700"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            disabled={isLastPage || isLoading}
            onClick={() => onPageChange(page + 1)}
            className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:hover:bg-transparent disabled:cursor-not-allowed cursor-pointer transition-colors text-slate-700"
          >
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}

// Backward compatibility alias for admin files
export const AdminTablePagination = Pagination;
