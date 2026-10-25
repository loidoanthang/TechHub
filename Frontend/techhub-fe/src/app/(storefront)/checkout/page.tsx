import { OnePageCartCheckoutView } from "@/features/cart/components/OnePageCartCheckoutView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Checkout & Cart | TechHub Marketplace",
  description: "Review your cart items, select shipping and payment method, and complete your purchase in one seamless checkout.",
};

export default function CheckoutPage() {
  return <OnePageCartCheckoutView />;
}
