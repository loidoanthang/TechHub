import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminReviewsView } from "@/features/admin/components/reviews/AdminReviewsView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Kiểm duyệt Đánh giá | Admin Portal TechHub",
  description: "Quản lý và kiểm duyệt đánh giá của khách hàng trên toàn sàn TechHub.",
};

export const instant = false;

export default function AdminReviewsPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh sách đánh giá...</p>
        </div>
      }
    >
      <AdminReviewsView />
    </Suspense>
  );
}
