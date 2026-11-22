import { RecentlyViewedView } from "@/features/account/components/RecentlyViewedView";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sản phẩm đã xem gần đây | TechHub Marketplace",
  description: "Lịch sử các sản phẩm công nghệ bạn đã xem gần đây trên TechHub Marketplace.",
};

export default function RecentlyViewedPage() {
  return <RecentlyViewedView />;
}
