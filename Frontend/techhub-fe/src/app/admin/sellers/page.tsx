import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminSellersView } from "@/features/admin/components/sellers/AdminSellersView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Đối tác & Gian hàng | Admin Portal TechHub",
  description: "Thẩm định hồ sơ đăng ký mở Shop Seller và quản lý trạng thái gian hàng.",
};

export const instant = false;

export default function AdminSellersPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh sách đối tác...</p>
        </div>
      }
    >
      <AdminSellersView />
    </Suspense>
  );
}
