import React, { Suspense } from "react";
import type { Metadata } from "next";
import { AdminUsersView } from "@/features/admin/components/users/AdminUsersView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Quản lý Người dùng | Admin Portal TechHub",
  description: "Tra cứu, khóa tạm hoặc cấm tài khoản người dùng trên sàn TechHub.",
};

export const instant = false;

export default function AdminUsersPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <p className="text-xs text-slate-500">Đang tải danh sách người dùng...</p>
        </div>
      }
    >
      <AdminUsersView />
    </Suspense>
  );
}
