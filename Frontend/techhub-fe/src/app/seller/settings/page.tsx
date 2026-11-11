import React, { Suspense } from "react";
import type { Metadata } from "next";
import { SellerSettingsView } from "@/features/seller/components/settings/SellerSettingsView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Kho Hàng & Cài Đặt Gian Hàng | Kênh Người Bán TechHub",
  description:
    "Cài đặt thông tin thương hiệu shop, địa chỉ kho lấy hàng, biểu phí giao hàng và tài khoản ngân hàng thụ hưởng.",
};

export const instant = false;

function SettingsLoading() {
  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full py-16 items-center justify-center min-h-[400px]">
      <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
      <p className="text-xs text-slate-500 font-medium">
        Đang tải thông tin cài đặt gian hàng...
      </p>
    </div>
  );
}

export default function SellerSettingsPage() {
  return (
    <Suspense fallback={<SettingsLoading />}>
      <SellerSettingsView />
    </Suspense>
  );
}
