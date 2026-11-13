import { FollowedShopsView } from "@/features/account/components/FollowedShopsView";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Gian hàng đang theo dõi | TechHub Marketplace",
  description: "Danh sách các thương hiệu và gian hàng công nghệ bạn đang theo dõi trên TechHub.",
};

export default function FollowedShopsPage() {
  return <FollowedShopsView />;
}
