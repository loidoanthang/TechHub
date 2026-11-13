import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminSettingsView } from "@/features/admin/components/settings/AdminSettingsView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Chính sách & Vận hành | Admin Portal TechHub",
  description: "Cấu hình chính sách sàn, thông báo Broadcast và giả lập giao vận demo.",
};

export const instant = false;

export default function AdminSettingsPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải cài đặt hệ thống...</p>
        </div>
      }
    >
      <AdminSettingsView />
    </Suspense>
  );
}
