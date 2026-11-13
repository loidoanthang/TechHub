import { WishlistView } from "@/features/account/components/WishlistView";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Danh sách Yêu thích | TechHub Marketplace",
  description: "Theo dõi và quản lý các sản phẩm công nghệ bạn yêu thích trên TechHub.",
};

export default function WishlistPage() {
  return <WishlistView />;
}
