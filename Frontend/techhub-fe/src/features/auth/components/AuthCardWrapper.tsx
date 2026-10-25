"use client";

import Link from "next/link";
import Image from "next/image";

interface AuthLayoutProps {
  children: React.ReactNode;
}

export function AuthCardWrapper({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-[#f8f9ff] text-[#0b1c30] flex flex-col justify-between selection:bg-[#dce1ff] selection:text-[#00164f] relative overflow-hidden">
      {/* Brand Header */}
      <header className="w-full pt-8 flex items-center justify-center shrink-0 z-10">
        <Link href="/" className="flex items-center gap-2 group">
          <div className="w-10 h-10 rounded-xl flex items-center justify-center p-2">
            <Image
              src="/logo.svg"
              alt="TechHub Logo"
              width={24}
              height={24}
              className="w-6 h-6"
            />
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-heading text-2xl font-bold tracking-tight text-[#0b1c30]">
              TechHub
            </span>
          </div>
        </Link>
      </header>

      {/* Main Content Area */}
      <main className="w-full flex-1 flex flex-col items-center justify-center px-4 py-8 z-10">
        {children}
      </main>

      {/* Minimal Footer */}
      <footer className="w-full py-6 text-center text-xs text-[#737688] z-10">
        <p>© 2026 TechHub Marketplace. All rights reserved.</p>
      </footer>
    </div>
  );
}
