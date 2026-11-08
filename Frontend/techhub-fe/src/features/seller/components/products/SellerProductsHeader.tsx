"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Search, X, RotateCcw, Plus } from "lucide-react";
import { ROUTES } from "@/lib/constants";
import type { CategorySummaryResponse } from "@/types/category";
import type { SellerProductSortBy, SortDirection } from "@/types/sellerProduct";

interface SellerProductsHeaderProps {
  keyword: string;
  onKeywordChange: (val: string) => void;
  selectedCategoryId: string | undefined;
  onCategoryChange: (val: string | undefined) => void;
  categories: CategorySummaryResponse[];
  sortBy: SellerProductSortBy;
  sortDir: SortDirection;
  onSortChange: (sortBy: SellerProductSortBy, sortDir: SortDirection) => void;
  onResetFilters: () => void;
  totalElements?: number;
}

export function SellerProductsHeader({
  keyword,
  onKeywordChange,
  selectedCategoryId,
  onCategoryChange,
  categories,
  sortBy,
  sortDir,
  onSortChange,
  onResetFilters,
  totalElements = 0,
}: SellerProductsHeaderProps) {
  const [localSearch, setLocalSearch] = useState(keyword);

  // Debounce search input 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      onKeywordChange(localSearch.trim());
    }, 300);
    return () => clearTimeout(timer);
  }, [localSearch, onKeywordChange]);

  const handleSortSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    const [field, dir] = val.split(":") as [SellerProductSortBy, SortDirection];
    onSortChange(field, dir);
  };

  const currentSortVal = `${sortBy}:${sortDir}`;

  return (
    <div className="flex flex-col gap-4">
      {/* 1. Title & Primary CTA row (matching SellerOrdersHeader) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <h1 className="font-heading text-xl sm:text-2xl font-extrabold text-on-surface tracking-tight">
            Quản Lý Sản Phẩm
          </h1>
          <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-heading font-extrabold text-xs">
            {totalElements} Sản phẩm
          </span>
        </div>

        <Link
          href={ROUTES.SELLER.NEW_PRODUCT}
          className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary/90 active:scale-[0.98] transition-all shadow-xs self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Đăng Bán Sản Phẩm</span>
        </Link>
      </div>

      {/* 2. Filter Bar: Search, Category, Sort, Reset (clean toolbar) */}
      <div className="flex flex-wrap items-center gap-2.5">
        {/* Search Input */}
        <div className="relative flex-1 min-w-[240px] sm:min-w-[280px]">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={localSearch}
            onChange={(e) => setLocalSearch(e.target.value)}
            placeholder="Tìm theo tên linh kiện, thương hiệu..."
            className="w-full h-10 pl-9.5 pr-9 bg-white text-xs font-medium text-slate-800 rounded-xl border border-slate-200 shadow-2xs focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 placeholder:text-slate-400"
          />
          {localSearch && (
            <button
              onClick={() => {
                setLocalSearch("");
                onKeywordChange("");
              }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
              title="Xóa tìm kiếm"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Category filter dropdown */}
        <div className="w-full sm:w-48">
          <select
            value={selectedCategoryId || ""}
            onChange={(e) => onCategoryChange(e.target.value || undefined)}
            className="w-full h-10 px-3 bg-white text-xs font-medium text-slate-700 rounded-xl border border-slate-200 shadow-2xs focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 cursor-pointer"
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>

        {/* Sort by dropdown */}
        <div className="w-full sm:w-52">
          <select
            value={currentSortVal}
            onChange={handleSortSelect}
            className="w-full h-10 px-3 bg-white text-xs font-medium text-slate-700 rounded-xl border border-slate-200 shadow-2xs focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 cursor-pointer"
          >
            <option value="createdAt:desc">Mới nhất (Ngày tạo)</option>
            <option value="createdAt:asc">Cũ nhất (Ngày tạo)</option>
            <option value="updatedAt:desc">Cập nhật gần nhất</option>
            <option value="name:asc">Tên sản phẩm: A → Z</option>
            <option value="name:desc">Tên sản phẩm: Z → A</option>
            <option value="brandName:asc">Thương hiệu: A → Z</option>
          </select>
        </div>

        {/* Reset button */}
        <button
          onClick={() => {
            setLocalSearch("");
            onResetFilters();
          }}
          title="Đặt lại bộ lọc"
          className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-slate-800 hover:bg-slate-50 transition-colors shadow-2xs shrink-0"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
