import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminCategoriesView } from "@/features/admin/components/categories/AdminCategoriesView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Danh mục & Thuộc tính | Admin Portal TechHub",
  description: "Quản lý danh mục sản phẩm và thuộc tính kỹ thuật động toàn sàn.",
};

export const instant = false;

export default function AdminCategoriesPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh mục...</p>
        </div>
      }
    >
      <AdminCategoriesView />
    </Suspense>
  );
}
