import { Metadata } from "next";
import { SellerDashboardView } from "@/features/seller/components/SellerDashboardView";

export const instant = false;

export const metadata: Metadata = {
  title: "Tổng Quan Kênh Người Bán | TechHub Electronics",
  description: "Trang tổng quan vận hành, doanh thu và quản lý kho hàng TechHub Seller Centre.",
};

export default function SellerDashboardPage() {
  return <SellerDashboardView />;
}
