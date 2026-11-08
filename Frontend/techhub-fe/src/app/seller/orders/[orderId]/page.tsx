import { Suspense } from "react";
import { Metadata } from "next";
import { SellerOrderDetailView } from "@/features/seller/components/orders/SellerOrderDetailView";
import { Loader2 } from "lucide-react";

export const instant = false;

export async function generateMetadata({
  params,
}: {
  params: Promise<{ orderId: string }>;
}): Promise<Metadata> {
  const resolvedParams = await params;
  return {
    title: `Chi Tiết Đơn Hàng #${resolvedParams.orderId.slice(-8).toUpperCase()} | TechHub Seller Centre`,
    description: "Chi tiết đóng gói linh kiện, lịch sử tiến trình vận đơn và đối soát doanh thu.",
  };
}

export default async function SellerOrderDetailPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const resolvedParams = await params;

  return (
    <Suspense
      fallback={
        <div className="min-h-[400px] flex items-center justify-center">
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
        </div>
      }
    >
      <SellerOrderDetailView orderId={resolvedParams.orderId} />
    </Suspense>
  );
}
