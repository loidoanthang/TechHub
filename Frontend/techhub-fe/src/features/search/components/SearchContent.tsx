"use client";

import React, { useState, useEffect, useMemo } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  RotateCcw,
  SlidersHorizontal,
  Loader2,
  PackageSearch,
  Search,
  AlertCircle,
} from "lucide-react";
import { ProductCard } from "@/components/shared";
import { SearchFilterSidebar, type FilterState } from "./SearchFilterSidebar";
import {
  useProductsQuery,
  useCategoriesQuery,
} from "@/features/products/hooks/useProductQueries";
import { mapProductSummaryToProduct } from "@/features/products/utils/productMapper";

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const isUuid = (val?: string): boolean => {
  return typeof val === "string" && UUID_REGEX.test(val);
};

export function SearchContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const queryParam = searchParams.get("q") || searchParams.get("keyword") || "";
  const categoryParam =
    searchParams.get("categoryId") || searchParams.get("category") || "";

  const [filters, setFilters] = useState<FilterState>({
    categoryId: categoryParam,
    subcategoryId: "",
    minPrice: searchParams.get("minPrice") || "",
    maxPrice: searchParams.get("maxPrice") || "",
    selectedBrands: [],
    attributes: {},
  });

  const [keyword, setKeyword] = useState(queryParam);
  const [searchInput, setSearchInput] = useState(queryParam);
  const [sortBy, setSortBy] = useState("newest");
  const [currentPage, setCurrentPage] = useState(0);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  // Fetch categories to resolve category names/slugs to UUID
  const { data: serverCategories = [] } = useCategoriesQuery();

  // Sync keyword & searchInput when queryParam changes
  useEffect(() => {
    setKeyword(queryParam);
    setSearchInput(queryParam);
    setCurrentPage(0);
  }, [queryParam]);

  // Sync categoryId when categoryParam changes
  useEffect(() => {
    if (categoryParam) {
      setFilters((prev) => ({ ...prev, categoryId: categoryParam }));
      setCurrentPage(0);
    }
  }, [categoryParam]);

  // Resolve category UUID safely
  const resolvedCategoryId = useMemo(() => {
    if (!filters.categoryId) return undefined;
    if (isUuid(filters.categoryId)) return filters.categoryId;

    const matched = serverCategories.find(
      (c) =>
        c.id === filters.categoryId ||
        c.name.toLowerCase() === filters.categoryId.toLowerCase() ||
        (c as unknown as { slug?: string }).slug === filters.categoryId
    );
    return matched ? matched.id : undefined;
  }, [filters.categoryId, serverCategories]);

  // Map frontend sort to Backend whitelisted sortBy
  const backendSortBy = useMemo(() => {
    switch (sortBy) {
      case "price-asc":
        return "price_asc";
      case "price-desc":
        return "price_desc";
      case "popular":
        return "popular";
      case "newest":
      default:
        return "newest";
    }
  }, [sortBy]);

  // TanStack Query calling Backend GET /api/products
  const {
    data: serverPage,
    isLoading,
    isError,
    refetch,
  } = useProductsQuery({
    keyword: keyword.trim() || undefined,
    categoryId: resolvedCategoryId,
    brandName: filters.selectedBrands[0] || undefined,
    minPrice: filters.minPrice ? Number(filters.minPrice) : undefined,
    maxPrice: filters.maxPrice ? Number(filters.maxPrice) : undefined,
    sortBy: backendSortBy,
    page: currentPage,
    size: 12,
  });

  const serverProducts =
    serverPage?.content?.map(mapProductSummaryToProduct) || [];
  const totalElements = serverPage?.totalElements ?? 0;
  const totalPages = serverPage?.totalPages ?? 1;

  // Selected Category Name for Badge
  const selectedCategoryName = useMemo(() => {
    if (!filters.categoryId) return null;
    const cat = serverCategories.find(
      (c) => c.id === filters.categoryId || c.id === resolvedCategoryId
    );
    return cat ? cat.name : "Danh mục đã chọn";
  }, [filters.categoryId, resolvedCategoryId, serverCategories]);

  // Handle in-page search submit
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = searchInput.trim();
    setKeyword(trimmed);
    setCurrentPage(0);
    if (trimmed) {
      router.push(`/search?q=${encodeURIComponent(trimmed)}`);
    } else {
      router.push("/search");
    }
  };

  const handleClearKeyword = () => {
    setSearchInput("");
    setKeyword("");
    setCurrentPage(0);
    router.push("/search");
  };

  const handleResetFilters = () => {
    setFilters({
      categoryId: "",
      subcategoryId: "",
      minPrice: "",
      maxPrice: "",
      selectedBrands: [],
      attributes: {},
    });
    setKeyword("");
    setSearchInput("");
    setSortBy("newest");
    setCurrentPage(0);
    router.push("/search");
  };

  const removeBrand = (brandName: string) => {
    setFilters((prev) => ({
      ...prev,
      selectedBrands: prev.selectedBrands.filter((b) => b !== brandName),
    }));
    setCurrentPage(0);
  };

  const removeCategory = () => {
    setFilters((prev) => ({ ...prev, categoryId: "" }));
    setCurrentPage(0);
  };

  const removePrice = () => {
    setFilters((prev) => ({ ...prev, minPrice: "", maxPrice: "" }));
    setCurrentPage(0);
  };

  const hasActiveFilters = Boolean(
    keyword ||
      filters.categoryId ||
      filters.selectedBrands.length > 0 ||
      filters.minPrice ||
      filters.maxPrice
  );

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full py-6 space-y-6">
      {/* 1. Header Overview & Breadcrumb */}
      <div className="bg-white rounded-2xl shadow-sm p-5 border border-[#eff4ff]">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs text-slate-500 mb-3">
          <Link href="/" className="hover:text-[#0045cc] transition-colors">
            Trang chủ
          </Link>
          <span className="text-slate-300">/</span>
          <span className="text-slate-800 font-semibold">Tìm kiếm sản phẩm</span>
        </nav>

        {/* Search Bar & Title Row */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pt-1">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
              {keyword ? (
                <>
                  Kết quả tìm kiếm cho <span className="text-[#0045cc]">&quot;{keyword}&quot;</span>
                </>
              ) : selectedCategoryName ? (
                <>
                  Sản phẩm trong <span className="text-[#0045cc]">{selectedCategoryName}</span>
                </>
              ) : (
                "Tất cả sản phẩm công nghệ"
              )}
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              {isLoading ? (
                "Đang tải sản phẩm..."
              ) : (
                <>
                  Tìm thấy <strong>{totalElements}</strong> sản phẩm chính hãng
                </>
              )}
            </p>
          </div>

          {/* In-Page Quick Search Input */}
          <form
            onSubmit={handleSearchSubmit}
            className="flex items-center gap-2 w-full lg:max-w-md"
          >
            <div className="relative flex-1">
              <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Tìm iPhone, MacBook, Sony, màn hình..."
                className="w-full pl-9 pr-8 py-2 bg-[#eff4ff] rounded-xl text-xs sm:text-sm font-medium text-[#0b1c30] placeholder:text-[#737688] border border-[#dce9ff] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/30"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={handleClearKeyword}
                  aria-label="Xóa từ khóa"
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            <button
              type="submit"
              className="px-4 py-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white rounded-xl text-xs font-bold transition-colors cursor-pointer shadow-sm flex-shrink-0"
            >
              Tìm kiếm
            </button>

            {/* Mobile Filter Toggle */}
            <button
              type="button"
              onClick={() => setMobileFilterOpen(!mobileFilterOpen)}
              className="lg:hidden flex items-center justify-center p-2 bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff] rounded-xl text-xs font-bold cursor-pointer flex-shrink-0"
              aria-label="Mở bộ lọc"
            >
              <SlidersHorizontal className="w-4 h-4" />
            </button>
          </form>
        </div>

        {/* Controls & Active Filters Row */}
        <div className="mt-4 pt-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-slate-100">
          {/* Active Filter Badges */}
          <div className="flex flex-wrap items-center gap-2">
            {hasActiveFilters && (
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Đang lọc:
              </span>
            )}

            {/* Keyword Badge */}
            {keyword && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]">
                <span>Từ khóa: &quot;{keyword}&quot;</span>
                <button
                  onClick={handleClearKeyword}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Bỏ từ khóa"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            )}

            {/* Category Badge */}
            {filters.categoryId && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]">
                <span>Danh mục: {selectedCategoryName}</span>
                <button
                  onClick={removeCategory}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Bỏ danh mục"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            )}

            {/* Brand Badges */}
            {filters.selectedBrands.map((b) => (
              <span
                key={b}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]">
                <span>Hãng: {b}</span>
                <button
                  onClick={() => removeBrand(b)}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Remove brand filter"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            ))}

            {/* Price Badge */}
            {(filters.minPrice || filters.maxPrice) && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]">
                <span>
                  Giá: {filters.minPrice ? `${Number(filters.minPrice).toLocaleString("vi-VN")}₫` : "0₫"}
                  {" - "}
                  {filters.maxPrice ? `${Number(filters.maxPrice).toLocaleString("vi-VN")}₫` : "Tối đa"}
                </span>
                <button
                  onClick={removePrice}
                  className="hover:text-[#a90900] cursor-pointer"
                  aria-label="Remove price filter"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            )}

            {hasActiveFilters && (
              <button
                onClick={handleResetFilters}
                className="text-xs font-bold text-[#a90900] hover:underline flex items-center gap-1 ml-1 cursor-pointer"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Xóa tất cả bộ lọc</span>
              </button>
            )}
          </div>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-2 self-end sm:self-auto flex-shrink-0">
            <span className="text-xs text-slate-500 font-medium">Sắp xếp:</span>
            <div className="relative">
              <select
                value={sortBy}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setCurrentPage(0);
                }}
                className="appearance-none bg-[#eff4ff] text-slate-700 text-xs font-semibold pl-3 pr-8 py-2 rounded-lg border border-[#dce9ff] focus:outline-none cursor-pointer"
              >
                <option value="newest">Mới nhất</option>
                <option value="price-asc">Giá: Thấp đến cao</option>
                <option value="price-desc">Giá: Cao đến thấp</option>
                <option value="popular">Bán chạy nhất</option>
              </select>
              <ChevronDown className="w-3.5 h-3.5 absolute right-2.5 top-2.5 text-slate-500 pointer-events-none" />
            </div>
          </div>
        </div>
      </div>

      {/* 2. Main Two-Column Layout */}
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        {/* LEFT SIDEBAR: Comprehensive Filters */}
        <div
          className={`w-full lg:w-72 ${
            mobileFilterOpen ? "block" : "hidden lg:block"
          }`}
        >
          <SearchFilterSidebar
            filters={filters}
            onFilterChange={(next) => {
              setFilters(next);
              setCurrentPage(0);
            }}
            onReset={handleResetFilters}
          />
        </div>

        {/* RIGHT SIDE: Product Grid */}
        <div className="flex-1 w-full space-y-6">
          {isLoading ? (
            <div className="py-24 flex flex-col items-center justify-center gap-3 text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
              <span className="text-xs font-medium">
                Đang tìm kiếm sản phẩm từ hệ thống...
              </span>
            </div>
          ) : isError ? (
            <div className="bg-white rounded-2xl border border-red-100 p-12 text-center flex flex-col items-center justify-center gap-3 shadow-sm">
              <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center">
                <AlertCircle className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-base text-[#0b1c30]">
                Không thể tải sản phẩm lúc này
              </h3>
              <p className="text-xs text-[#737688] max-w-sm">
                Đã xảy ra lỗi khi kết nối với máy chủ. Vui lòng thử lại.
              </p>
              <button
                onClick={() => refetch()}
                className="mt-2 px-4 py-2 bg-[#0045cc] text-white text-xs font-bold rounded-lg hover:bg-[#0d5bff] transition-colors cursor-pointer"
              >
                Thử lại
              </button>
            </div>
          ) : serverProducts.length === 0 ? (
            <div className="bg-white rounded-2xl border border-slate-100 p-16 text-center flex flex-col items-center justify-center gap-3 shadow-sm">
              <div className="w-14 h-14 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
                <PackageSearch className="w-7 h-7" />
              </div>
              <h3 className="font-bold text-base text-[#0b1c30]">
                Không tìm thấy sản phẩm phù hợp
              </h3>
              <p className="text-xs text-[#737688] max-w-sm">
                {keyword
                  ? `Không có sản phẩm nào khớp với từ khóa "${keyword}". Hãy thử tìm với từ khóa khác.`
                  : "Không có sản phẩm nào trong tiêu chí lọc này. Hãy thử nới lỏng bộ lọc."}
              </p>
              <button
                onClick={handleResetFilters}
                className="mt-2 px-5 py-2.5 bg-[#0045cc] text-white text-xs font-bold rounded-xl hover:bg-[#0d5bff] transition-colors shadow-sm cursor-pointer"
              >
                Xem tất cả sản phẩm
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {serverProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pt-8 flex items-center justify-center gap-2">
              <button
                disabled={currentPage === 0}
                onClick={() => {
                  setCurrentPage((p) => Math.max(0, p - 1));
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
                aria-label="Trang trước"
                className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              {Array.from({ length: totalPages }, (_, i) => i).map((pageIdx) => (
                <button
                  key={pageIdx}
                  onClick={() => {
                    setCurrentPage(pageIdx);
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  className={`w-9 h-9 rounded-lg text-xs font-bold shadow-sm flex items-center justify-center cursor-pointer transition-colors ${
                    currentPage === pageIdx
                      ? "bg-[#0045cc] text-white"
                      : "bg-white border border-slate-200 text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {pageIdx + 1}
                </button>
              ))}
              <button
                disabled={currentPage >= totalPages - 1}
                onClick={() => {
                  setCurrentPage((p) => Math.min(totalPages - 1, p + 1));
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
                aria-label="Trang sau"
                className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 flex items-center justify-center transition-colors shadow-sm cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
