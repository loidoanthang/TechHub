import { Metadata } from "next";
import { SellerLayout } from "@/features/seller/components/SellerLayout";

export const instant = false;

export const metadata: Metadata = {
  title: "Seller Centre | TechHub Electronics",
  description: "Trang tổng quan và quản trị kênh bán hàng chuyên nghiệp TechHub Electronics.",
};

export default function AppSellerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <SellerLayout>{children}</SellerLayout>;
}
