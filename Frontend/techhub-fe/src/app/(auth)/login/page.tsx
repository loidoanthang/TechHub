import { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Đăng nhập | TechHub Marketplace",
  description: "Đăng nhập tài khoản TechHub để mua sắm thiết bị công nghệ và theo dõi đơn hàng.",
};

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[440px] h-[480px] bg-white rounded-2xl animate-pulse" />
      }
    >
      <LoginForm />
    </Suspense>
  );
}
