import { LoginForm } from "@/features/auth/components/LoginForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Log in | TechHub Marketplace",
  description: "Log in to your TechHub account to explore tech gear and orders.",
};

export default function LoginPage() {
  return <LoginForm />;
}
