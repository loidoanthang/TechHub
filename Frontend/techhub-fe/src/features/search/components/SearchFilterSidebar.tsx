"use client";

import React, { useState, useEffect } from "react";
import {
  RotateCcw,
  SlidersHorizontal,
  Check,
  X,
} from "lucide-react";
import { formatCurrencyVND, parseCurrencyVND } from "@/lib/utils";
import { useCategoriesQuery } from "@/features/products/hooks/useProductQueries";

export interface FilterState {
  categoryId: string;
  subcategoryId: string;
  minPrice: string;
  maxPrice: string;
  selectedBrands: string[];
  attributes: Record<string, string[]>;
}

interface SearchFilterSidebarProps {
  filters: FilterState;
  onFilterChange: (filters: FilterState) => void;
  onReset: () => void;
}

export const BRANDS_DATA = [
  { id: "Apple", name: "Apple", count: 4 },
  { id: "Samsung", name: "Samsung", count: 1 },
  { id: "Xiaomi", name: "Xiaomi", count: 2 },
  { id: "Sony", name: "Sony", count: 1 },
  { id: "ASUS", name: "ASUS", count: 1 },
  { id: "Dell", name: "Dell", count: 1 },
  { id: "Anker", name: "Anker", count: 1 },
  { id: "Marshall", name: "Marshall", count: 1 },
  { id: "Keychron", name: "Keychron", count: 1 },
  { id: "Garmin", name: "Garmin", count: 1 },
  { id: "Roborock", name: "Roborock", count: 1 },
  { id: "Ezviz", name: "Ezviz", count: 1 },
  { id: "Aqara", name: "Aqara", count: 1 },
];

export function SearchFilterSidebar({
  filters,
  onFilterChange,
  onReset,
}: SearchFilterSidebarProps) {
  const { data: serverCategories = [] } = useCategoriesQuery();

  const SLIDER_MIN = 0;
  const SLIDER_MAX = 60000000;
  const STEP = 500000;

  const [minInput, setMinInput] = useState(filters.minPrice || "0");
  const [maxInput, setMaxInput] = useState(filters.maxPrice || "60000000");

  const minVal = Math.max(SLIDER_MIN, Math.min(Number(minInput) || 0, SLIDER_MAX));
  const maxVal = Math.min(SLIDER_MAX, Math.max(Number(maxInput) || SLIDER_MAX, minVal));

  const minPercent = ((minVal - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)) * 100;
  const maxPercent = ((maxVal - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)) * 100;

  // Sync inputs when parent filters change (e.g. onReset)
  useEffect(() => {
    setMinInput(filters.minPrice || "0");
    setMaxInput(filters.maxPrice || "60000000");
  }, [filters.minPrice, filters.maxPrice]);

  const handleSliderMin = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Math.min(Number(e.target.value), maxVal - STEP);
    setMinInput(val.toString());
  };

  const handleSliderMax = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Math.max(Number(e.target.value), minVal + STEP);
    setMaxInput(val.toString());
  };

  const handleCategorySelect = (catId: string) => {
    onFilterChange({
      ...filters,
      categoryId: filters.categoryId === catId ? "" : catId,
      subcategoryId: "",
    });
  };

  const handlePriceApply = () => {
    onFilterChange({
      ...filters,
      minPrice: minInput === "0" ? "" : minInput,
      maxPrice: maxInput === "60000000" ? "" : maxInput,
    });
  };

  const handleBrandToggle = (brandId: string) => {
    const list = filters.selectedBrands;
    const next = list.includes(brandId)
      ? list.filter((b) => b !== brandId)
      : [...list, brandId];
    onFilterChange({
      ...filters,
      selectedBrands: next,
    });
  };

  const hasActiveFilters = Boolean(
    filters.categoryId ||
      filters.selectedBrands.length > 0 ||
      filters.minPrice ||
      filters.maxPrice
  );

  const selectedCategoryName = serverCategories.find(
    (c) => c.id === filters.categoryId
  )?.name;

  return (
    <aside className="w-full lg:w-72 flex-shrink-0 bg-white rounded-2xl border border-[#dce9ff] shadow-sm divide-y divide-[#eff4ff] overflow-hidden sticky top-[132px]">
      {/* Sidebar Header */}
      <div className="p-4 flex items-center justify-between bg-[#eff4ff]">
        <div className="flex items-center gap-2 text-[#0b1c30] font-bold text-sm">
          <SlidersHorizontal className="w-4 h-4 text-[#0045cc]" />
          <span>Bộ lọc tìm kiếm</span>
        </div>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={onReset}
            className="text-xs font-semibold text-[#a90900] hover:underline transition-colors flex items-center gap-1 cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Đặt lại</span>
          </button>
        )}
      </div>

      {/* Active Filter Chips (Inside Sidebar) */}
      {hasActiveFilters && (
        <div className="p-3 bg-blue-50/40 space-y-2">
          <span className="text-[10px] font-bold text-[#737688] uppercase tracking-wider block">
            Đang áp dụng:
          </span>
          <div className="flex flex-wrap gap-1.5">
            {/* Category Chip */}
            {filters.categoryId && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-white text-[#0045cc] border border-[#dce9ff] shadow-2xs">
                <span className="truncate max-w-[140px]">{selectedCategoryName || "Danh mục"}</span>
                <button
                  type="button"
                  onClick={() => handleCategorySelect("")}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Bỏ chọn danh mục"
                >
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}

            {/* Price Chip */}
            {(filters.minPrice || filters.maxPrice) && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-white text-[#0045cc] border border-[#dce9ff] shadow-2xs">
                <span>
                  {filters.minPrice ? `${Number(filters.minPrice).toLocaleString("vi-VN")}₫` : "0₫"}
                  {" - "}
                  {filters.maxPrice ? `${Number(filters.maxPrice).toLocaleString("vi-VN")}₫` : "Max"}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setMinInput("0");
                    setMaxInput("60000000");
                    onFilterChange({ ...filters, minPrice: "", maxPrice: "" });
                  }}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Bỏ chọn giá"
                >
                  <X className="w-3 h-3" />
                </button>
              </span>
            )}

            {/* Brand Chips */}
            {filters.selectedBrands.map((b) => (
              <span
                key={b}
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-white text-[#0045cc] border border-[#dce9ff] shadow-2xs"
              >
                <span>{b}</span>
                <button
                  type="button"
                  onClick={() => handleBrandToggle(b)}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label={`Bỏ chọn hãng ${b}`}
                >
                  <X className="w-3 h-3" />
                </button>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* 1. Category Filter */}
      <div className="p-4 space-y-3">
        <h3 className="font-bold text-xs text-[#0b1c30] uppercase tracking-wider">
          Danh mục sản phẩm
        </h3>

        {/* Categories List */}
        <div className="space-y-0.5 max-h-56 overflow-y-auto pr-1">
          <button
            type="button"
            onClick={() => handleCategorySelect("")}
            className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer text-left ${
              !filters.categoryId
                ? "bg-[#0045cc] text-white font-semibold shadow-xs"
                : "text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
            }`}
          >
            <span>Tất cả danh mục</span>
            {!filters.categoryId && <Check className="w-3.5 h-3.5" />}
          </button>

          {serverCategories.map((cat) => {
            const isSelected = filters.categoryId === cat.id;
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => handleCategorySelect(cat.id)}
                className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer text-left ${
                  isSelected
                    ? "bg-[#0045cc] text-white font-semibold shadow-xs"
                    : "text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
                }`}
              >
                <span className="truncate">{cat.name}</span>
                {isSelected && <Check className="w-3.5 h-3.5 flex-shrink-0 ml-1" />}
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Price Range Filter with Dual Range Slider */}
      <div className="p-4 space-y-3">
        <h3 className="font-bold text-xs text-[#0b1c30] uppercase tracking-wider">
          Khoảng giá (VNĐ)
        </h3>

        {/* Interactive Dual Slider with colored selected range */}
        <div className="relative h-6 flex items-center px-1 my-1">
          {/* Base track */}
          <div className="relative w-full h-1.5 rounded-full bg-[#dce9ff]">
            {/* Active Highlight Range */}
            <div
              className="absolute top-0 bottom-0 rounded-full bg-[#0045cc]"
              style={{
                left: `${minPercent}%`,
                width: `${Math.max(0, maxPercent - minPercent)}%`,
              }}
            />
          </div>

          {/* Dual Range Inputs with 2 centered thumbs */}
          <input
            type="range"
            min={SLIDER_MIN}
            max={SLIDER_MAX}
            step={STEP}
            value={minVal}
            onChange={handleSliderMin}
            className="range-slider-thumb absolute inset-x-0 w-full opacity-100 bg-transparent pointer-events-none appearance-none z-20 cursor-pointer"
            aria-label="Giá tối thiểu"
          />
          <input
            type="range"
            min={SLIDER_MIN}
            max={SLIDER_MAX}
            step={STEP}
            value={maxVal}
            onChange={handleSliderMax}
            className="range-slider-thumb absolute inset-x-0 w-full opacity-100 bg-transparent pointer-events-none appearance-none z-20 cursor-pointer"
            aria-label="Giá tối đa"
          />
        </div>

        {/* Min/Max Input Boxes */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              inputMode="numeric"
              value={formatCurrencyVND(minInput, true)}
              onChange={(e) => {
                const num = parseCurrencyVND(e.target.value);
                setMinInput(num.toString());
              }}
              placeholder="Từ"
              className="w-full px-2.5 py-1.5 bg-[#eff4ff] rounded-lg text-xs font-semibold text-[#0b1c30] border border-[#dce9ff] focus:outline-none focus:border-[#0045cc]"
            />
          </div>
          <span className="text-[#737688] text-xs">-</span>
          <div className="relative flex-1">
            <input
              type="text"
              inputMode="numeric"
              value={formatCurrencyVND(maxInput, true)}
              onChange={(e) => {
                const num = parseCurrencyVND(e.target.value);
                setMaxInput(num.toString());
              }}
              placeholder="Đến"
              className="w-full px-2.5 py-1.5 bg-[#eff4ff] rounded-lg text-xs font-semibold text-[#0b1c30] border border-[#dce9ff] focus:outline-none focus:border-[#0045cc]"
            />
          </div>
        </div>

        <button
          onClick={handlePriceApply}
          className="w-full py-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white rounded-xl text-xs font-bold transition-colors cursor-pointer shadow-sm"
        >
          Áp dụng khoảng giá
        </button>
      </div>

      {/* 3. Brands Selection */}
      <div className="p-4 space-y-3">
        <h3 className="font-bold text-xs text-[#0b1c30] uppercase tracking-wider">
          Thương hiệu
        </h3>
        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
          {BRANDS_DATA.map((brand) => {
            const isChecked = filters.selectedBrands.includes(brand.id);
            return (
              <label
                key={brand.id}
                className="flex items-center justify-between text-xs font-medium text-[#434656] hover:text-[#0045cc] cursor-pointer select-none"
              >
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => handleBrandToggle(brand.id)}
                    className="w-3.5 h-3.5 rounded text-[#0045cc] accent-[#0045cc]"
                  />
                  <span>{brand.name}</span>
                </div>
              </label>
            );
          })}
        </div>
      </div>
    </aside>
  );
}
