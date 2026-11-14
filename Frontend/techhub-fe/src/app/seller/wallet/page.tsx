import type { Metadata } from "next";
import { SellerWalletView } from "@/features/seller/components/wallet";

export const metadata: Metadata = {
  title: "Ví & Doanh thu Người Bán | TechHub Seller Center",
  description:
    "Theo dõi số dư khả dụng, quản lý rút tiền và đối soát biến động số dư bán hàng",
};

export default function SellerWalletPage() {
  return <SellerWalletView />;
}
