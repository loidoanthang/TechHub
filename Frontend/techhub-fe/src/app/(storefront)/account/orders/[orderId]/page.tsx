import { Suspense } from "react";
import { OrderDetailView } from "@/features/account/components/OrderDetailView";
import { Metadata } from "next";

interface OrderDetailPageProps {
  params: Promise<{ orderId: string }>;
}

export async function generateMetadata({
  params,
}: OrderDetailPageProps): Promise<Metadata> {
  const { orderId } = await params;
  return {
    title: `Chi tiết đơn hàng #${orderId} | TechHub Marketplace`,
    description: `Theo dõi tiến trình vận chuyển, địa chỉ nhận hàng và thanh toán cho đơn hàng #${orderId}.`,
  };
}

async function OrderDetailContent({ params }: OrderDetailPageProps) {
  const { orderId } = await params;
  return <OrderDetailView orderId={orderId} />;
}

export default function OrderDetailPage({ params }: OrderDetailPageProps) {
  return (
    <Suspense
      fallback={
        <div className="w-full h-96 bg-white rounded-2xl animate-pulse"></div>
      }
    >
      <OrderDetailContent params={params} />
    </Suspense>
  );
}
