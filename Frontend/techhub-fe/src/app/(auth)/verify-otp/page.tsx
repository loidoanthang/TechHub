import { Suspense } from "react";
import { OtpVerificationForm } from "@/features/auth/components/OtpVerificationForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Xác thực OTP | TechHub Marketplace",
  description: "Xác thực email để kích hoạt và bảo mật tài khoản TechHub.",
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
