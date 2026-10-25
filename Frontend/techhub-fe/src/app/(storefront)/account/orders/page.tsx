import { OrdersListView } from "@/features/account/components/OrdersListView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "My Purchases & Orders | TechHub Marketplace",
  description: "Track your orders, view order history, and monitor component delivery milestones on TechHub.",
};

export default function OrdersPage() {
  return <OrdersListView />;
}
