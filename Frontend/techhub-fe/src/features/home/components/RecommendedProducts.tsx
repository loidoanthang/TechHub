import { ProductCard } from "@/components/shared";
import { MOCK_RECOMMENDED_PRODUCTS } from "@/mocks";

export function RecommendedProducts() {
  return (
    <section className="max-w-6xl mx-auto px-6 lg:px-12 w-full pb-20">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
        <div>
          <h2 className="text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
            Recommended for You
          </h2>
          <p className="text-xs text-[#434656] mt-1">
            Engineered hardware tailored to your build history and performance benchmarks
          </p>
        </div>
      </div>

      {/* 12-Card Catalog Grid (3 Rows x 4 Columns) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        {MOCK_RECOMMENDED_PRODUCTS.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {/* Load More Button */}
      <div className="mt-12 flex justify-center">
        <button className="px-8 py-3 rounded-lg bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] font-bold text-sm transition-colors shadow-sm">
          Load More Products
        </button>
      </div>
    </section>
  );
}
