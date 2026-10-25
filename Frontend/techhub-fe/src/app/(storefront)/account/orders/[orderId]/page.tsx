import { Suspense } from "react";
import { notFound } from "next/navigation";
import { OrderDetailView } from "@/features/account/components/OrderDetailView";
import { MOCK_ORDERS } from "@/mocks";
import { Metadata } from "next";

interface OrderDetailPageProps {
  params: Promise<{ orderId: string }>;
}

export async function generateMetadata({
  params,
}: OrderDetailPageProps): Promise<Metadata> {
  const { orderId } = await params;
  return {
    title: `Order #${orderId} Details | TechHub Marketplace`,
    description: `Tracking timeline, delivery destination, and payment ledger for order #${orderId}.`,
  };
}

async function OrderDetailContent({ params }: OrderDetailPageProps) {
  const { orderId } = await params;

  const order =
    MOCK_ORDERS.find(
      (o) => o.id === orderId || o.orderNumber.toLowerCase() === orderId.toLowerCase()
    ) || MOCK_ORDERS[0];

  if (!order) {
    notFound();
  }

  return <OrderDetailView order={order} />;
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
