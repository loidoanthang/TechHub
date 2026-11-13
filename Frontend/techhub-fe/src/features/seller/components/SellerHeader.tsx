"use client";

import Link from "next/link";
import { ChevronRight, ShieldCheck, HelpCircle } from "lucide-react";

export function SellerHeader() {
  return (
    <header className="sticky top-0 z-40 w-full bg-white border-b border-[#c3c5d9]/30 shadow-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Left: Brand & Portal Badge */}
        <div className="flex items-center gap-3">
          <Link href="/" className="flex items-center gap-2 group">
            <span className="font-heading text-xl font-extrabold tracking-tight text-[#0b1c30]">
              TECH<span className="text-[#0045cc]">HUB</span>
            </span>
          </Link>
          <span className="h-4 w-px bg-[#c3c5d9]"></span>
          <span className="text-xs font-bold text-[#434656] uppercase tracking-wider">
            Seller Centre
          </span>
        </div>

        {/* Right: User Profile & Support */}
        <div className="flex items-center gap-4">
          <a
            href="#support"
            className="hidden sm:flex items-center gap-1.5 text-xs font-semibold text-[#737688] hover:text-[#0045cc] transition-colors"
          >
            <HelpCircle className="w-4 h-4" />
            <span>Seller Support</span>
          </a>

          <div className="h-4 w-px bg-[#c3c5d9] hidden sm:block"></div>

          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-[#0045cc]/10 text-[#0045cc] font-heading font-bold text-xs flex items-center justify-center ring-2 ring-[#eff4ff]">
              AM
            </div>
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-bold text-[#0b1c30] leading-tight">
                Alex Morgan
              </span>
              <span className="text-[10px] text-[#737688]">Buyer Account</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
