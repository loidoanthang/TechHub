import { Suspense } from "react";
import { SuspendedView } from "@/features/auth/components/SuspendedView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Account Suspended | TechHub Marketplace",
  description: "Account security notification and support appeal.",
};

export default function SuspendedPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[480px] h-96 bg-white rounded-2xl animate-pulse"></div>
      }
    >
      <SuspendedView />
    </Suspense>
  );
}
