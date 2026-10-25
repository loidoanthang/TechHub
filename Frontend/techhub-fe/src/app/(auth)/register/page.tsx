import { RegisterForm } from "@/features/auth/components/RegisterForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Create Account | TechHub Marketplace",
  description: "Join TechHub to explore tech gear and exclusive enthusiast deals.",
};

export default function RegisterPage() {
  return <RegisterForm />;
}
