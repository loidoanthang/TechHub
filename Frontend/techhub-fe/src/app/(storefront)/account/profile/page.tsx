import { ProfileView } from "@/features/account/components/ProfileView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Thông tin tài khoản | TechHub Marketplace",
  description: "Quản lý thông tin hồ sơ cá nhân và thông tin liên hệ trên TechHub.",
};

export default function ProfilePage() {
  return <ProfileView />;
}
