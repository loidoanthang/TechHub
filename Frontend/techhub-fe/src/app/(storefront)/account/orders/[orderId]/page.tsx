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
    title: `Order #${orderId} Details | TechHub Marketplace`,
    description: `Tracking timeline, delivery destination, and payment ledger for order #${orderId}.`,
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
