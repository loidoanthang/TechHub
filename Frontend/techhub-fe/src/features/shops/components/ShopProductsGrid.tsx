"use client";

import { useState } from "react";
import { Search, SlidersHorizontal, PackageX, Loader2 } from "lucide-react";
import { ProductCard } from "@/components/shared";
import { useShopProductsQuery } from "../hooks/useShopQueries";
import { mapProductSummaryToProduct } from "@/features/products/utils/productMapper";

interface ShopProductsGridProps {
  shopId: string;
  shopName: string;
}

export function ShopProductsGrid({ shopId, shopName }: ShopProductsGridProps) {
  const [keyword, setKeyword] = useState("");
  const [submittedKeyword, setSubmittedKeyword] = useState("");
  const [sortBy, setSortBy] = useState<"newest" | "price_asc" | "price_desc">("newest");

  const { data: pageData, isLoading, error } = useShopProductsQuery(
    shopId,
    shopName,
    {
      keyword: submittedKeyword || undefined,
      sortBy,
    }
  );

  const products = (pageData?.content || []).map(mapProductSummaryToProduct);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmittedKeyword(keyword.trim());
  };

  return (
    <section className="w-full flex flex-col gap-6 pt-2">
      {/* Controls Bar: Search & Sort Tabs */}
      <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#c3c5d9]/30 shadow-sm flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        {/* Search inside shop */}
        <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder={`Tìm sản phẩm trong ${shopName}...`}
            className="w-full h-11 pl-10 pr-24 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 transition-all"
          />
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[#737688]" />
          <button
            type="submit"
            className="absolute right-1.5 top-1/2 -translate-y-1/2 px-3.5 py-1.5 rounded-lg bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-colors cursor-pointer"
          >
            Tìm
          </button>
        </form>

        {/* Sort Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
          <span className="text-xs font-semibold text-[#737688] hidden md:inline">
            Sắp xếp theo:
          </span>

          <button
            type="button"
            onClick={() => setSortBy("newest")}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
              sortBy === "newest"
                ? "bg-[#0045cc] text-white shadow-sm"
                : "bg-[#eff4ff]/60 text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
            }`}
          >
            Mới nhất
          </button>

          <button
            type="button"
            onClick={() => setSortBy("price_asc")}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
              sortBy === "price_asc"
                ? "bg-[#0045cc] text-white shadow-sm"
                : "bg-[#eff4ff]/60 text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
            }`}
          >
            Giá: Thấp đến Cao
          </button>

          <button
            type="button"
            onClick={() => setSortBy("price_desc")}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
              sortBy === "price_desc"
                ? "bg-[#0045cc] text-white shadow-sm"
                : "bg-[#eff4ff]/60 text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
            }`}
          >
            Giá: Cao đến Thấp
          </button>
        </div>
      </div>

      {/* Products Grid / State */}
      {isLoading ? (
        <div className="w-full h-80 flex flex-col items-center justify-center gap-3 bg-white rounded-2xl border border-[#c3c5d9]/25">
          <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
          <span className="text-xs text-[#737688]">Đang tải danh sách sản phẩm của Shop...</span>
        </div>
      ) : products.length === 0 ? (
        <div className="w-full bg-white rounded-2xl p-12 text-center border border-[#c3c5d9]/25 flex flex-col items-center justify-center gap-3">
          <div className="w-16 h-16 rounded-2xl bg-[#eff4ff] flex items-center justify-center text-[#737688]">
            <PackageX className="w-8 h-8 text-[#737688]" />
          </div>
          <h3 className="font-heading font-bold text-base text-[#0b1c30]">
            Không tìm thấy sản phẩm nào
          </h3>
          <p className="text-xs text-[#737688] max-w-sm">
            {submittedKeyword
              ? `Không có sản phẩm nào khớp với từ khóa "${submittedKeyword}" trong cửa hàng này.`
              : "Cửa hàng hiện chưa đăng tải sản phẩm nào."}
          </p>
          {submittedKeyword && (
            <button
              type="button"
              onClick={() => {
                setKeyword("");
                setSubmittedKeyword("");
              }}
              className="mt-2 px-4 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-colors cursor-pointer"
            >
              Xem tất cả sản phẩm của Shop
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  );
}
