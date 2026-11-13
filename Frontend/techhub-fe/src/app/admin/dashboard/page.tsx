import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminDashboardView } from "@/features/admin/components/dashboard/AdminDashboardView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Admin Dashboard | TechHub Admin Portal",
  description: "Tổng quan doanh thu, GMV, hoa hồng và chỉ số kiểm duyệt toàn sàn TechHub.",
};

export default function AdminDashboardPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải Dashboard...</p>
        </div>
      }
    >
      <AdminDashboardView />
    </Suspense>
  );
}
