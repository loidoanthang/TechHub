import { ProfileView } from "@/features/account/components/ProfileView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Account Information | TechHub Marketplace",
  description: "Manage your personal profile details and contact verification credentials on TechHub.",
};

export default function ProfilePage() {
  return <ProfileView />;
}
