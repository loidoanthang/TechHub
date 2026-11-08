import { Suspense } from "react";
import { Metadata } from "next";
import { SellerOrdersListView } from "@/features/seller/components/orders/SellerOrdersListView";
import { Loader2 } from "lucide-react";

export const instant = false;

export const metadata: Metadata = {
  title: "Quản Lý Đơn Hàng | TechHub Seller Centre",
  description: "Tra cứu, đóng gói linh kiện và cập nhật vận hành đơn hàng gian hàng TechHub.",
};

export default function SellerOrdersPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[400px] flex items-center justify-center">
          <Loader2 className="w-8 h-8 text-primary animate-spin motion-reduce:animate-none" />
          <span className="sr-only">Đang tải danh sách đơn hàng...</span>
        </div>
      }
    >
      <SellerOrdersListView />
    </Suspense>
  );
}
