import type { Metadata } from "next";
import { NotificationsPageView } from "@/features/notifications/components";

export const metadata: Metadata = {
  title: "Thông báo gian hàng | TechHub Seller Center",
  description:
    "Cập nhật đơn hàng mới, đánh giá khách hàng, phê duyệt sản phẩm và thông báo vận hành gian hàng.",
};

export default function SellerNotificationsPage() {
  return (
    <NotificationsPageView
      portalContext="seller"
      title="Thông báo gian hàng"
      description="Quản lý và theo dõi đơn bán mới, đánh giá khách hàng và thông báo vận hành Shop"
    />
  );
}
