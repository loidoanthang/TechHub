"use client";

import React, { useState, useEffect } from "react";
import {
  RotateCcw,
  SlidersHorizontal,
  Check,
} from "lucide-react";
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

  return (
    <aside className="w-full lg:w-72 flex-shrink-0 space-y-6">
      {/* Header bar of Sidebar */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm flex items-center justify-between">
        <div className="flex items-center gap-2 text-slate-800 font-bold text-sm">
          <SlidersHorizontal className="w-4 h-4 text-[#0045cc]" />
          <span>BỘ LỌC TÌM KIẾM</span>
        </div>
        <button
          onClick={onReset}
          className="text-xs font-semibold text-[#a90900] hover:underline flex items-center gap-1 cursor-pointer"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Đặt lại</span>
        </button>
      </div>

      {/* 1. Category Filter */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-3">
        <h3 className="font-bold text-sm text-[#0b1c30]">
          Danh mục sản phẩm
        </h3>

        {/* Categories List */}
        <div className="space-y-1">
          <button
            type="button"
            onClick={() => handleCategorySelect("")}
            className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
              !filters.categoryId
                ? "bg-[#0045cc] text-white"
                : "text-slate-700 hover:bg-[#eff4ff]"
            }`}
          >
            <span>Tất cả danh mục</span>
            {!filters.categoryId && <Check className="w-4 h-4" />}
          </button>

          {serverCategories.map((cat) => {
            const isSelected = filters.categoryId === cat.id;
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => handleCategorySelect(cat.id)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer text-left ${
                  isSelected
                    ? "bg-[#0045cc] text-white"
                    : "text-slate-700 hover:bg-[#eff4ff]"
                }`}
              >
                <span className="truncate">{cat.name}</span>
                {isSelected && <Check className="w-4 h-4 flex-shrink-0 ml-1" />}
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Price Range Filter with 2 Núm kéo (Dual Range Slider) */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-4">
        <h3 className="font-bold text-sm text-[#0b1c30]">Khoảng giá (VNĐ)</h3>

        {/* Interactive Dual Slider with colored selected range */}
        <div className="relative h-6 flex items-center px-1 my-1">
          {/* Base track */}
          <div className="relative w-full h-1.5 rounded-full bg-slate-200">
            {/* Active Highlight Range (Xanh dương) */}
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
              type="number"
              value={minInput}
              onChange={(e) => setMinInput(e.target.value)}
              placeholder="Từ"
              className="w-full pl-2 pr-2 py-1.5 bg-[#eff4ff] rounded-lg text-xs font-semibold text-slate-800 border border-slate-200 focus:outline-none"
            />
          </div>
          <span className="text-slate-400 text-xs">-</span>
          <div className="relative flex-1">
            <input
              type="number"
              value={maxInput}
              onChange={(e) => setMaxInput(e.target.value)}
              placeholder="Đến"
              className="w-full pl-2 pr-2 py-1.5 bg-[#eff4ff] rounded-lg text-xs font-semibold text-slate-800 border border-slate-200 focus:outline-none"
            />
          </div>
        </div>

        <button
          onClick={handlePriceApply}
          className="w-full py-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white rounded-lg text-xs font-bold transition-colors cursor-pointer"
        >
          Áp dụng khoảng giá
        </button>
      </div>

      {/* 3. Brands Selection */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-3">
        <h3 className="font-bold text-sm text-[#0b1c30]">Thương hiệu</h3>
        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
          {BRANDS_DATA.map((brand) => {
            const isChecked = filters.selectedBrands.includes(brand.id);
            return (
              <label
                key={brand.id}
                className="flex items-center justify-between text-xs font-medium text-slate-700 hover:text-[#0045cc] cursor-pointer"
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
                <span className="text-[11px] text-slate-400 font-mono">
                  ({brand.count})
                </span>
              </label>
            );
          })}
        </div>
      </div>
    </aside>
  );
}
