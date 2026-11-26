import { RegisterForm } from "@/features/auth/components/RegisterForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Đăng ký tài khoản | TechHub Marketplace",
  description: "Tạo tài khoản TechHub để khám phá thiết bị công nghệ và nhận ưu đãi chính hãng.",
};

export default function RegisterPage() {
  return <RegisterForm />;
}
