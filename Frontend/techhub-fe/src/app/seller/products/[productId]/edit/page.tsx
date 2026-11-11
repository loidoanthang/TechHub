import React, { Suspense } from "react";
import type { Metadata } from "next";
import { ProductFormView } from "@/features/seller/components/products/form/ProductFormView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Chỉnh Sửa Sản Phẩm | Kênh Người Bán TechHub",
  description: "Cập nhật thông tin chi tiết và thông số kỹ thuật sản phẩm linh kiện trên sàn TechHub",
};

export const instant = false;

function EditProductLoading() {
  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full py-12 items-center justify-center min-h-[400px]">
      <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
      <p className="text-xs text-slate-500 font-medium">Đang tải thông tin sản phẩm cần chỉnh sửa...</p>
    </div>
  );
}

export default async function EditProductPage({
  params,
}: {
  params: Promise<{ productId: string }>;
}) {
  const { productId } = await params;

  return (
    <Suspense fallback={<EditProductLoading />}>
      <ProductFormView productId={productId} />
    </Suspense>
  );
}
