"use client";

import React, { useState } from "react";
import {
  ChevronDown,
  ChevronRight,
  RotateCcw,
  SlidersHorizontal,
  Check,
} from "lucide-react";
import { MOCK_CATEGORIES_DATA, MOCK_BRANDS_DATA } from "@/mocks";

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

export function SearchFilterSidebar({
  filters,
  onFilterChange,
  onReset,
}: SearchFilterSidebarProps) {
  const SLIDER_MIN = 0;
  const SLIDER_MAX = 4000;

  const [minInput, setMinInput] = useState(filters.minPrice || "0");
  const [maxInput, setMaxInput] = useState(filters.maxPrice || "3500");

  const minVal = Math.max(SLIDER_MIN, Math.min(Number(minInput) || 0, SLIDER_MAX));
  const maxVal = Math.min(SLIDER_MAX, Math.max(Number(maxInput) || SLIDER_MAX, minVal));

  const minPercent = ((minVal - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)) * 100;
  const maxPercent = ((maxVal - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)) * 100;

  const handleSliderMin = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Math.min(Number(e.target.value), maxVal - 50);
    setMinInput(val.toString());
  };

  const handleSliderMax = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Math.max(Number(e.target.value), minVal + 50);
    setMaxInput(val.toString());
  };

  const currentCategory =
    MOCK_CATEGORIES_DATA.find((c) => c.id === filters.categoryId) || MOCK_CATEGORIES_DATA[0];

  const handleCategorySelect = (catId: string) => {
    onFilterChange({
      ...filters,
      categoryId: catId,
      subcategoryId: "",
      attributes: {}, // Reset attributes when category changes
    });
  };

  const handleSubcategorySelect = (subId: string) => {
    onFilterChange({
      ...filters,
      subcategoryId: filters.subcategoryId === subId ? "" : subId,
    });
  };

  const handleApplyPrice = (e: React.FormEvent) => {
    e.preventDefault();
    onFilterChange({
      ...filters,
      minPrice: minInput,
      maxPrice: maxInput,
    });
  };

  const handleBrandToggle = (brandId: string) => {
    const nextBrands = filters.selectedBrands.includes(brandId)
      ? filters.selectedBrands.filter((b) => b !== brandId)
      : [...filters.selectedBrands, brandId];

    onFilterChange({
      ...filters,
      selectedBrands: nextBrands,
    });
  };

  const handleAttributeToggle = (attrKey: string, option: string) => {
    const currentList = filters.attributes[attrKey] || [];
    const nextList = currentList.includes(option)
      ? currentList.filter((o) => o !== option)
      : [...currentList, option];

    onFilterChange({
      ...filters,
      attributes: {
        ...filters.attributes,
        [attrKey]: nextList,
      },
    });
  };

  return (
    <aside className="w-full lg:w-72 flex-shrink-0 space-y-6">
      {/* Header bar of Sidebar */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm flex items-center justify-between">
        <div className="flex items-center gap-2 text-slate-800 font-bold text-sm">
          <SlidersHorizontal className="w-4 h-4 text-[#0045cc]" />
          <span>FILTERS</span>
        </div>
        <button
          onClick={onReset}
          className="text-xs font-semibold text-[#a90900] hover:underline flex items-center gap-1"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Clear All</span>
        </button>
      </div>

      {/* 1. Category & Subcategory Filter */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-3">
        <h3 className="font-bold text-sm text-[#0b1c30]">
          Categories
        </h3>

        {/* Main Categories List */}
        <div className="space-y-1">
          {MOCK_CATEGORIES_DATA.map((cat) => {
            const isSelected = filters.categoryId === cat.id;
            return (
              <div key={cat.id} className="space-y-1">
                <button
                  type="button"
                  onClick={() => handleCategorySelect(cat.id)}
                  className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-xs font-semibold transition-colors ${
                    isSelected
                      ? "bg-[#0045cc] text-white"
                      : "text-slate-700 hover:bg-[#eff4ff]"
                  }`}
                >
                  <span>{cat.name}</span>
                  {isSelected ? (
                    <ChevronDown className="w-4 h-4" />
                  ) : (
                    <ChevronRight className="w-4 h-4 text-slate-400" />
                  )}
                </button>

                {/* Subcategories */}
                {isSelected && cat.subcategories.length > 0 && (
                  <div className="pl-4 pr-1 py-1.5 space-y-1 border-l-2 border-[#0045cc]/30 ml-2">
                    {cat.subcategories.map((sub) => {
                      const isSubSelected = filters.subcategoryId === sub.id;
                      return (
                        <button
                          key={sub.id}
                          type="button"
                          onClick={() => handleSubcategorySelect(sub.id)}
                          className={`w-full text-left px-2 py-1.5 rounded text-xs transition-colors ${
                            isSubSelected
                              ? "font-bold text-[#0045cc] bg-[#eff4ff]"
                              : "text-slate-600 hover:text-[#0045cc]"
                          }`}
                        >
                          • {sub.name}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 2. Price Range Filter */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-sm text-[#0b1c30]">Price Range</h3>
        </div>

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

          {/* Dual Range Inputs with perfectly centered thumbs */}
          <input
            type="range"
            min={SLIDER_MIN}
            max={SLIDER_MAX}
            step={25}
            value={minVal}
            onChange={handleSliderMin}
            className="range-slider-thumb absolute inset-x-0 w-full opacity-100 bg-transparent pointer-events-none appearance-none z-20 cursor-pointer"
            aria-label="Minimum price slider"
          />
          <input
            type="range"
            min={SLIDER_MIN}
            max={SLIDER_MAX}
            step={25}
            value={maxVal}
            onChange={handleSliderMax}
            className="range-slider-thumb absolute inset-x-0 w-full opacity-100 bg-transparent pointer-events-none appearance-none z-20 cursor-pointer"
            aria-label="Maximum price slider"
          />
        </div>

        <form onSubmit={handleApplyPrice} className="space-y-3 pt-1">
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[11px] text-slate-500 block mb-1">Minimum</label>
              <div className="relative">
                <span className="absolute left-2.5 top-2 text-xs text-slate-400">$</span>
                <input
                  type="number"
                  placeholder="Min"
                  value={minInput}
                  onChange={(e) => setMinInput(e.target.value)}
                  className="w-full pl-6 pr-2 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:border-[#0045cc] bg-[#f8f9ff] font-mono"
                />
              </div>
            </div>

            <div>
              <label className="text-[11px] text-slate-500 block mb-1">Maximum</label>
              <div className="relative">
                <span className="absolute left-2.5 top-2 text-xs text-slate-400">$</span>
                <input
                  type="number"
                  placeholder="Max"
                  value={maxInput}
                  onChange={(e) => setMaxInput(e.target.value)}
                  className="w-full pl-6 pr-2 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:border-[#0045cc] bg-[#f8f9ff] font-mono"
                />
              </div>
            </div>
          </div>

          <button
            type="submit"
            className="w-full py-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white rounded-lg text-xs font-bold transition-colors shadow-sm active:scale-[0.99]"
          >
            Apply Price Filter
          </button>
        </form>
      </div>

      {/* 3. Brand Filter */}
      <div className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-3">
        <h3 className="font-bold text-sm text-[#0b1c30]">Brands</h3>
        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
          {MOCK_BRANDS_DATA.map((brand) => {
            const isChecked = filters.selectedBrands.includes(brand.id);
            return (
              <label
                key={brand.id}
                className="flex items-center justify-between cursor-pointer group text-xs text-slate-700 select-none"
              >
                <div className="flex items-center gap-2">
                  <div
                    onClick={() => handleBrandToggle(brand.id)}
                    className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${
                      isChecked
                        ? "bg-[#0045cc] border-[#0045cc] text-white"
                        : "border-slate-300 bg-white group-hover:border-[#0045cc]"
                    }`}
                  >
                    {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                  </div>
                  <span className={isChecked ? "font-bold text-[#0b1c30]" : ""}>
                    {brand.name}
                  </span>
                </div>
                <span className="text-[10px] text-slate-400">({brand.count})</span>
              </label>
            );
          })}
        </div>
      </div>

      {/* 4. Dynamic Product Attributes */}
      {currentCategory.attributes.map((attr) => (
        <div
          key={attr.key}
          className="bg-white rounded-xl p-4 border border-[#eff4ff] shadow-sm space-y-2.5"
        >
          <h4 className="font-bold text-xs uppercase tracking-wider">
            {attr.label}
          </h4>
          <div className="flex flex-wrap gap-1.5">
            {attr.options.map((opt) => {
              const isSelected = (filters.attributes[attr.key] || []).includes(opt);
              return (
                <button
                  key={opt}
                  type="button"
                  onClick={() => handleAttributeToggle(attr.key, opt)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
                    isSelected
                      ? "bg-[#0045cc] text-white shadow-sm"
                      : "bg-[#eff4ff] text-slate-700 hover:bg-[#dce9ff]"
                  }`}
                >
                  {opt}
                </button>
              );
            })}
          </div>
        </div>
      ))}

      {/* Bottom Clear Button */}
      <button
        type="button"
        onClick={onReset}
        className="w-full py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors flex items-center justify-center gap-1.5"
      >
        <RotateCcw className="w-3.5 h-3.5" />
        <span>Clear All Filters</span>
      </button>
    </aside>
  );
}
