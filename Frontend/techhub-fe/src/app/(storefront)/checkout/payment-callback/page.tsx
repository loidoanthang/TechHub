import { PaymentCallbackView } from "@/features/checkout/components/PaymentCallbackView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Kết quả thanh toán | TechHub Marketplace",
  description: "Kết quả xác thực giao dịch qua cổng thanh toán VNPay.",
};

export default function PaymentCallbackPage() {
  return <PaymentCallbackView />;
}
