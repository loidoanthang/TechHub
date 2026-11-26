import { OnePageCartCheckoutView } from "@/features/cart/components/OnePageCartCheckoutView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Giỏ hàng & Thanh toán | TechHub Marketplace",
  description: "Kiểm tra giỏ hàng, chọn địa chỉ giao hàng, phương thức thanh toán và hoàn tất đơn hàng trên TechHub.",
};

export default function CheckoutPage() {
  return <OnePageCartCheckoutView />;
}
