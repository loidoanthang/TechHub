import { Suspense } from "react";
import { AccountSidebar } from "@/features/account/components/AccountSidebar";

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12">
        {/* 2-Column Responsive Layout: Left Sidebar (w-56 matching Seller & Admin) + Right Workspace */}
        <div className="flex flex-col lg:flex-row gap-6 items-start">
          <div className="w-full lg:w-56 shrink-0">
            <Suspense
              fallback={
                <div className="w-full h-80 bg-white rounded-2xl animate-pulse"></div>
              }
            >
              <AccountSidebar />
            </Suspense>
          </div>
          <div className="flex-1 min-w-0 w-full">{children}</div>
        </div>
      </div>
    </div>
  );
}
