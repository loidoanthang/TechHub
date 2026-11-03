import { SecurityView } from "@/features/account/components/SecurityView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Bảo mật & Mật khẩu | TechHub Marketplace",
  description: "Quản lý mật khẩu đăng nhập và bảo vệ an toàn cho tài khoản TechHub của bạn.",
};

export default function SecurityPage() {
  return <SecurityView />;
}
