import { Metadata } from "next";
import { SellerRegistrationView } from "@/features/seller/components/SellerRegistrationView";

export const metadata: Metadata = {
  title: "Register as a Seller | TechHub Electronics",
  description:
    "Open your official hardware merchant store on TechHub Marketplace with integrated escrow protection and logistics.",
};

export default function SellerRegisterPage() {
  return <SellerRegistrationView />;
}
