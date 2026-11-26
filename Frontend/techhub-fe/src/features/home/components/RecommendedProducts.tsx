"use client";

import Link from "next/link";
import { ProductCard } from "@/components/shared";
import { useProductsQuery } from "@/features/products/hooks/useProductQueries";
import { mapProductSummaryToProduct } from "@/features/products/utils/productMapper";
import { MOCK_RECOMMENDED_PRODUCTS } from "@/mocks";
import { Loader2, ArrowRight } from "lucide-react";

export function RecommendedProducts() {
  const { data: serverPage, isLoading } = useProductsQuery({
    size: 8,
    sortBy: "createdAt",
  });

  const serverProducts = serverPage?.content?.map(mapProductSummaryToProduct) || [];
  const hasServerProducts = serverProducts.length > 0;
  const productsToRender = hasServerProducts ? serverProducts : MOCK_RECOMMENDED_PRODUCTS;

  return (
    <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full pb-14 sm:pb-20">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between my-6 sm:mb-8 gap-3 sm:gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl lg:text-3xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
            Gợi ý dành cho bạn
          </h2>
          <p className="text-xs text-[#434656] mt-1">
            Thiết bị công nghệ tuyển chọn phù hợp với nhu cầu và hiệu năng của bạn
          </p>
        </div>
        <Link
          href="/search"
          className="text-xs font-bold text-[#0045cc] hover:underline inline-flex items-center gap-1 self-start sm:self-auto"
        >
          Xem tất cả <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {isLoading && !hasServerProducts ? (
        <div className="w-full py-16 flex flex-col items-center justify-center gap-3 text-slate-500">
          <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
          <span className="text-xs">Đang tải gợi ý sản phẩm...</span>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-6">
          {productsToRender.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* Load More Button */}
      <div className="mt-12 flex justify-center">
        <Link
          href="/search"
          className="px-8 py-3 rounded-lg bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] font-bold text-sm transition-colors shadow-sm inline-flex items-center gap-2"
        >
          <span>Khám phá thêm sản phẩm</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    </section>
  );
}
