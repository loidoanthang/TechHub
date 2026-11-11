import React, { Suspense } from "react";
import type { Metadata } from "next";
import { SellerVouchersView } from "@/features/seller/components/vouchers/SellerVouchersView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Quản Lý Mã Giảm Giá | Kênh Người Bán TechHub",
  description:
    "Quản lý, tạo mới và theo dõi các chương trình voucher khuyến mãi của gian hàng TechHub Electronics.",
};

export const instant = false;

function VouchersLoading() {
  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full py-16 items-center justify-center min-h-[400px]">
      <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
      <p className="text-xs text-slate-500 font-medium">Đang tải danh sách mã giảm giá...</p>
    </div>
  );
}

export default function SellerVouchersPage() {
  return (
    <Suspense fallback={<VouchersLoading />}>
      <SellerVouchersView />
    </Suspense>
  );
}
