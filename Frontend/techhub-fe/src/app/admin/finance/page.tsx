import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminFinanceView } from "@/features/admin/components/finance/AdminFinanceView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Tài chính & Voucher Sàn | Admin Portal TechHub",
  description: "Quản lý Voucher Toàn sàn và phê duyệt yêu cầu giải ngân Payout Seller.",
};

export const instant = false;

export default function AdminFinancePage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải dữ liệu tài chính...</p>
        </div>
      }
    >
      <AdminFinanceView />
    </Suspense>
  );
}
