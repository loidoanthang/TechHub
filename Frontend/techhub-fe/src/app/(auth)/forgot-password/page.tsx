import { ForgotPasswordForm } from "@/features/auth/components/ForgotPasswordForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Quên mật khẩu | TechHub Marketplace",
  description: "Khôi phục mật khẩu tài khoản TechHub qua mã xác thực OTP.",
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
