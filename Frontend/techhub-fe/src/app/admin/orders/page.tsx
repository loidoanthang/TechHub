import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminOrdersView } from "@/features/admin/components/orders/AdminOrdersView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Đơn hàng Toàn sàn | Admin Portal TechHub",
  description: "Giám sát đơn hàng, doanh thu và xử lý khiếu nại toàn sàn TechHub.",
};

export const instant = false;

export default function AdminOrdersPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh sách đơn hàng...</p>
        </div>
      }
    >
      <AdminOrdersView />
    </Suspense>
  );
}
