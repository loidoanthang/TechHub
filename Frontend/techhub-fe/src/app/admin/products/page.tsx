import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminProductsView } from "@/features/admin/components/products/AdminProductsView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Kiểm duyệt Sản phẩm | Admin Portal TechHub",
  description: "Thẩm định, phê duyệt hoặc từ chối sản phẩm mới từ các Shop.",
};

export const instant = false;

export default function AdminProductsPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh sách kiểm duyệt...</p>
        </div>
      }
    >
      <AdminProductsView />
    </Suspense>
  );
}
