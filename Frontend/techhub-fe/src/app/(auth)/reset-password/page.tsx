import { Suspense } from "react";
import { ResetPasswordForm } from "@/features/auth/components/ResetPasswordForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Reset Password | TechHub Marketplace",
  description: "Set a new secure password for your TechHub marketplace account.",
};

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[460px] h-[520px] bg-white rounded-2xl animate-pulse" />
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}
