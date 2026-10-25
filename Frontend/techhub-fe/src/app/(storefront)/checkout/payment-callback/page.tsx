import { PaymentCallbackView } from "@/features/checkout/components/PaymentCallbackView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Payment Result | TechHub Marketplace",
  description: "VNPay payment gateway transaction authorization result.",
};

export default function PaymentCallbackPage() {
  return <PaymentCallbackView />;
}
