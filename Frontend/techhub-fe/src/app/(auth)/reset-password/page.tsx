import { Suspense } from "react";
import { ResetPasswordForm } from "@/features/auth/components/ResetPasswordForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Đặt lại mật khẩu | TechHub Marketplace",
  description: "Thiết lập mật khẩu mới an toàn cho tài khoản TechHub của bạn.",
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
