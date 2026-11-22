import { Suspense } from "react";
import { Metadata } from "next";
import { PaymentMethodsView } from "@/features/account/components/PaymentMethodsView";
import { Loader2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Phương thức thanh toán | TechHub Marketplace",
  description: "Quản lý thẻ ngân hàng liên kết VNPAY để thanh toán 1-Click nhanh chóng và bảo mật.",
};

export default function PaymentMethodsPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full bg-white rounded-2xl p-12 border border-[#c3c5d9]/25 flex items-center justify-center">
          <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
        </div>
      }
    >
      <PaymentMethodsView />
    </Suspense>
  );
}
