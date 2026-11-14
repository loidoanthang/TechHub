import { AccountMessagesPageView } from "@/features/chat";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Tin nhắn | TechHub",
  description: "Danh sách hội thoại với các gian hàng",
};

export default function AccountMessagesPage() {
  return <AccountMessagesPageView />;
}
