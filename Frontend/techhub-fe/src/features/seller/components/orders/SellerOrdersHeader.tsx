"use client";

import React from "react";
import { Search, Calendar, X } from "lucide-react";

interface SellerOrdersHeaderProps {
  keyword: string;
  onKeywordChange: (val: string) => void;
  startDate: string;
  endDate: string;
  onStartDateChange: (val: string) => void;
  onEndDateChange: (val: string) => void;
  totalElements?: number;
}

export function SellerOrdersHeader({
  keyword,
  onKeywordChange,
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  totalElements = 0,
}: SellerOrdersHeaderProps) {
  const todayStr = new Date().toISOString().split("T")[0];

  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
      {/* Title & Total count badge */}
      <div className="flex items-center gap-3">
        <h1 className="font-heading text-xl sm:text-2xl font-extrabold text-on-surface tracking-tight">
          Quản Lý Đơn Hàng
        </h1>
        <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-heading font-extrabold text-xs">
          {totalElements} Đơn hàng
        </span>
      </div>

      {/* Search Bar & Date Filter */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <label htmlFor="seller-order-search" className="sr-only">
            Tìm kiếm theo mã đơn, khách hàng hoặc SKU
          </label>
          <Search
            aria-hidden="true"
            className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none"
          />
          <input
            id="seller-order-search"
            type="text"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="Tìm mã đơn, tên khách, SKU..."
            aria-label="Tìm kiếm theo mã đơn, khách hàng hoặc SKU"
            className="w-full h-10 pl-9.5 pr-10 bg-white text-xs font-medium text-on-surface rounded-xl border border-slate-200 shadow-2xs focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 placeholder:text-slate-500"
          />
          {keyword && (
            <button
              type="button"
              onClick={() => onKeywordChange("")}
              aria-label="Xóa từ khóa tìm kiếm"
              className="absolute right-1 top-1/2 -translate-y-1/2 min-w-[36px] min-h-[36px] flex items-center justify-center text-slate-500 hover:text-on-surface rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Date Filter */}
        <div className="w-full sm:w-auto flex items-center justify-between sm:justify-start gap-1.5 bg-white border border-slate-200 rounded-xl px-2.5 py-1.5 shadow-2xs text-xs">
          <Calendar aria-hidden="true" className="w-3.5 h-3.5 text-slate-500 shrink-0" />
          <div className="flex items-center gap-1.5 flex-1 sm:flex-none">
            <input
              type="date"
              id="seller-order-start-date"
              value={startDate}
              max={endDate || todayStr}
              onChange={(e) => onStartDateChange(e.target.value)}
              aria-label="Lọc đơn từ ngày"
              className="bg-transparent text-xs font-semibold text-on-surface focus:outline-none cursor-pointer"
              title="Từ ngày"
            />
            <span aria-hidden="true" className="text-slate-500 font-bold">
              -
            </span>
            <input
              type="date"
              id="seller-order-end-date"
              value={endDate}
              min={startDate}
              max={todayStr}
              onChange={(e) => onEndDateChange(e.target.value)}
              aria-label="Lọc đơn đến ngày"
              className="bg-transparent text-xs font-semibold text-on-surface focus:outline-none cursor-pointer"
              title="Đến ngày"
            />
          </div>
          {(startDate || endDate) && (
            <button
              type="button"
              onClick={() => {
                onStartDateChange("");
                onEndDateChange("");
              }}
              aria-label="Xóa khoảng thời gian lọc"
              title="Xóa lọc ngày"
              className="min-w-[32px] min-h-[32px] flex items-center justify-center text-slate-500 hover:text-on-surface rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary shrink-0"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
