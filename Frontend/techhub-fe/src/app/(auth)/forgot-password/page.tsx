import { ForgotPasswordForm } from "@/features/auth/components/ForgotPasswordForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Forgot Password | TechHub Marketplace",
  description: "Reset your TechHub account password via email verification code.",
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
