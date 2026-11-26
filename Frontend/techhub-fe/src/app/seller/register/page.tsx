import { Metadata } from "next";
import { SellerRegistrationView } from "@/features/seller/components/SellerRegistrationView";

export const metadata: Metadata = {
  title: "Đăng ký Kênh Người Bán | TechHub Marketplace",
  description:
    "Mở gian hàng công nghệ chính hãng trên TechHub Marketplace với hệ thống bảo chứng thanh toán và vận chuyển tích hợp.",
};

export default function SellerRegisterPage() {
  return <SellerRegistrationView />;
}
