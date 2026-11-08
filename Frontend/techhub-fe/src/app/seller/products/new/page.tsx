import React, { Suspense } from "react";
import type { Metadata } from "next";
import { ProductFormView } from "@/features/seller/components/products/form/ProductFormView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Đăng Bán Sản Phẩm Mới | Kênh Người Bán TechHub",
  description: "Tạo và gửi duyệt sản phẩm linh kiện thiết bị phần cứng mới trên sàn TechHub",
};

export const instant = false;

function ProductFormLoading() {
  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full py-12 items-center justify-center min-h-[400px]">
      <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
      <p className="text-xs text-slate-500 font-medium">Đang tải biểu mẫu đăng bán sản phẩm...</p>
    </div>
  );
}

export default function NewProductPage() {
  return (
    <Suspense fallback={<ProductFormLoading />}>
      <ProductFormView />
    </Suspense>
  );
}
