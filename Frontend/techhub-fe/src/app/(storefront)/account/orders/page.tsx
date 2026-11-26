import { OrdersListView } from "@/features/account/components/OrdersListView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Đơn mua của tôi | TechHub Marketplace",
  description: "Theo dõi trạng thái đơn hàng, lịch sử mua sắm và chi tiết giao nhận trên TechHub.",
};

export default function OrdersPage() {
  return <OrdersListView />;
}
