import { SellerChatView } from "@/features/chat";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Chat khách hàng | TechHub Seller",
  description: "Trò chuyện thời gian thực với khách hàng của bạn",
};

export default function SellerChatPage() {
  return <SellerChatView />;
}
