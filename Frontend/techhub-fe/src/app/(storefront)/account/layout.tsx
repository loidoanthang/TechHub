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
        {/* 2-Column Responsive Layout: Left Sidebar + Right Workspace */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
          <div className="lg:col-span-4 w-full flex flex-col">
            <Suspense
              fallback={
                <div className="w-full h-80 bg-white rounded-2xl animate-pulse"></div>
              }
            >
              <AccountSidebar />
            </Suspense>
          </div>
          <div className="lg:col-span-8 w-full flex flex-col">{children}</div>
        </div>
      </div>
    </div>
  );
}
