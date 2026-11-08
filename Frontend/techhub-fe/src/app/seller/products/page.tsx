import { Suspense } from "react";
import { Metadata } from "next";
import { SellerProductsView } from "@/features/seller/components/products/SellerProductsView";
import { Loader2 } from "lucide-react";

export const instant = false;

export const metadata: Metadata = {
  title: "Quản Lý Sản Phẩm | TechHub Seller Centre",
  description: "Quản lý danh mục kho linh kiện phần cứng, phân loại biến thể và trạng thái duyệt.",
};

export default function SellerProductsPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[400px] flex items-center justify-center">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
        </div>
      }
    >
      <SellerProductsView />
    </Suspense>
  );
}
