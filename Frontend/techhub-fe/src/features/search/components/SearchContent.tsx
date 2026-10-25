"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  RotateCcw,
  SlidersHorizontal,
} from "lucide-react";
import { ProductCard } from "@/components/shared";
import { SearchFilterSidebar, type FilterState } from "./SearchFilterSidebar";
import { MOCK_SEARCH_PRODUCTS } from "@/mocks";

export function SearchContent() {
  const [filters, setFilters] = useState<FilterState>({
    categoryId: "laptops",
    subcategoryId: "",
    minPrice: "",
    maxPrice: "",
    selectedBrands: ["asus", "keychron"],
    attributes: {
      ram: ["16GB", "32GB"],
    },
  });

  const [sortBy, setSortBy] = useState("recommended");
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  const handleResetFilters = () => {
    setFilters({
      categoryId: "laptops",
      subcategoryId: "",
      minPrice: "",
      maxPrice: "",
      selectedBrands: [],
      attributes: {},
    });
  };

  const removeBrand = (brandId: string) => {
    setFilters({
      ...filters,
      selectedBrands: filters.selectedBrands.filter((b) => b !== brandId),
    });
  };

  const removeAttribute = (key: string, val: string) => {
    const list = filters.attributes[key] || [];
    setFilters({
      ...filters,
      attributes: {
        ...filters.attributes,
        [key]: list.filter((v) => v !== val),
      },
    });
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full py-6 space-y-6">
      {/* 1. Header Overview & Breadcrumb */}
      <div className="bg-white rounded-2xl shadow-sm p-5 border border-[#eff4ff]">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs text-slate-500 mb-2">
          <Link href="/" className="hover:text-[#0045cc] transition-colors">
            Home
          </Link>
          <span className="text-slate-300">/</span>
          <span className="text-slate-800 font-semibold">Search Results</span>
        </nav>

        {/* Search Title & Count */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pt-1">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
              Results for{" "}
              <span className="text-[#0045cc] italic">
                &quot;Mechanical Keyboards & Gaming Laptops&quot;
              </span>
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Showing <strong>{MOCK_SEARCH_PRODUCTS.length}</strong> verified enthusiast products
            </p>
          </div>

          {/* Mobile Filter Button */}
          <button
            onClick={() => setMobileFilterOpen(!mobileFilterOpen)}
            className="lg:hidden flex items-center justify-center gap-2 px-4 py-2 bg-[#0045cc] text-white rounded-lg text-xs font-bold"
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span>Filters ({filters.selectedBrands.length + Object.values(filters.attributes).flat().length})</span>
          </button>
        </div>

        {/* Controls & Active Filters Row */}
        <div className="mt-4 pt-4 flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-t border-slate-100">
          {/* Active Filter Badges */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Active:
            </span>

            {filters.selectedBrands.map((b) => (
              <span
                key={b}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]"
              >
                <span>Brand: {b.toUpperCase()}</span>
                <button
                  onClick={() => removeBrand(b)}
                  className="hover:text-[#a90900]"
                  aria-label="Remove filter"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            ))}

            {Object.entries(filters.attributes).map(([key, vals]) =>
              vals.map((v) => (
                <span
                  key={`${key}-${v}`}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-100"
                >
                  <span>{key.toUpperCase()}: {v}</span>
                  <button
                    onClick={() => removeAttribute(key, v)}
                    className="hover:text-[#a90900]"
                    aria-label="Remove filter"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </span>
              ))
            )}

            {(filters.selectedBrands.length > 0 ||
              Object.values(filters.attributes).flat().length > 0) && (
              <button
                onClick={handleResetFilters}
                className="text-xs font-bold text-[#a90900] hover:underline flex items-center gap-1 ml-2"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reset all</span>
              </button>
            )}
          </div>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-3 self-end lg:self-auto">
            <div className="relative">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="appearance-none bg-[#eff4ff] text-slate-700 text-xs font-semibold pl-3 pr-8 py-2 rounded-lg border border-slate-200 focus:outline-none cursor-pointer"
              >
                <option value="recommended">Sort by: Recommended</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="rating">Highest Rated</option>
                <option value="newest">Newest Arrivals</option>
              </select>
              <ChevronDown className="w-3.5 h-3.5 absolute right-2.5 top-2.5 text-slate-500 pointer-events-none" />
            </div>
          </div>
        </div>
      </div>

      {/* 2. Main Two-Column Layout */}
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        {/* LEFT SIDEBAR: Comprehensive Filters */}
        <div className={`w-full lg:w-72 ${mobileFilterOpen ? "block" : "hidden lg:block"}`}>
          <SearchFilterSidebar
            filters={filters}
            onFilterChange={setFilters}
            onReset={handleResetFilters}
          />
        </div>

        {/* RIGHT SIDE: Product Grid & Live Feed */}
        <div className="flex-1 w-full space-y-6">
          {/* Product Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {MOCK_SEARCH_PRODUCTS.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          {/* Pagination */}
          <div className="pt-8 flex items-center justify-center gap-2">
            <button
              aria-label="Previous page"
              className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button className="w-9 h-9 rounded-lg bg-[#0045cc] text-white text-xs font-bold shadow-sm flex items-center justify-center">
              1
            </button>
            <button className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm">
              2
            </button>
            <button className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm">
              3
            </button>
            <span className="text-slate-400 px-1 text-xs">...</span>
            <button
              aria-label="Next page"
              className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
