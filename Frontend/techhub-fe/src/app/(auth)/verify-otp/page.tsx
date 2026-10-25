import { Suspense } from "react";
import { OtpVerificationForm } from "@/features/auth/components/OtpVerificationForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Verify OTP | TechHub Marketplace",
  description: "Verify your email address to secure your TechHub account.",
};

export default function VerifyOtpPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[440px] h-96 bg-white rounded-2xl animate-pulse"></div>
      }
    >
      <OtpVerificationForm />
    </Suspense>
  );
}
