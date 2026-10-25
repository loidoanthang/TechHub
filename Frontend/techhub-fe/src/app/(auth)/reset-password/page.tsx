import { ResetPasswordForm } from "@/features/auth/components/ResetPasswordForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Reset Password | TechHub Marketplace",
  description: "Set a new secure password for your TechHub marketplace account.",
};

export default function ResetPasswordPage() {
  return <ResetPasswordForm />;
}
