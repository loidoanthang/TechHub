import { Suspense } from "react";
import { SearchContent } from "@/features/search/components";

export default function ProductsPage() {
  return (
    <Suspense
      fallback={
        <div className="max-w-6xl mx-auto px-6 py-20 text-center text-sm text-slate-500">
          Đang tải danh sách sản phẩm...
        </div>
      }
    >
      <SearchContent />
    </Suspense>
  );
}
