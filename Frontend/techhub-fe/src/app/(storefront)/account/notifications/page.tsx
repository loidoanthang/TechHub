import type { Metadata } from "next";
import { NotificationsPageView } from "@/features/notifications/components";

export const metadata: Metadata = {
  title: "Thông báo của tôi | TechHub Marketplace",
  description:
    "Cập nhật trạng thái đơn hàng, ưu đãi khuyến mãi, phản hồi đánh giá và tin tức hệ thống TechHub.",
};

export default function AccountNotificationsPage() {
  return (
    <NotificationsPageView
      portalContext="buyer"
      title="Thông báo của tôi"
      description="Theo dõi cập nhật đơn hàng, khuyến mãi và thông báo tài khoản TechHub"
    />
  );
}
