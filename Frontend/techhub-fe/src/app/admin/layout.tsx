import type { Metadata } from "next";
import { AdminLayout } from "@/features/admin/components/AdminLayout";

export const metadata: Metadata = {
  title: "Admin Portal | TechHub Marketplace",
  description:
    "Trung tâm điều hành, kiểm duyệt sản phẩm, đối tác và quản trị hệ thống TechHub Electronics Marketplace.",
};

export default function AppAdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AdminLayout>{children}</AdminLayout>;
}
