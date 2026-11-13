import type { Metadata } from "next";
import { SellerReviewsView } from "@/features/seller/components/reviews/SellerReviewsView";

export const metadata: Metadata = {
  title: "Quản lý Đánh giá & Phản hồi | TechHub Seller",
  description: "Theo dõi nhận xét của người mua và gửi phản hồi chăm sóc khách hàng",
};

export default function SellerReviewsPage() {
  return <SellerReviewsView />;
}
