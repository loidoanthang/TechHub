import { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Log in | TechHub Marketplace",
  description: "Log in to your TechHub account to explore tech gear and orders.",
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
