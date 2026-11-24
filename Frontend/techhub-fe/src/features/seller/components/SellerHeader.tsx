"use client";

import Link from "next/link";
import Image from "next/image";
import { HelpCircle, User } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";

export function SellerHeader() {
  const { user, isAuthenticated } = useAuth();

  const fullName = user
    ? `${user.lastName || ""} ${user.firstName || ""}`.trim() || user.email
    : "Khách";

  const initials = user
    ? `${user.firstName?.[0] || ""}${user.lastName?.[0] || ""}`.toUpperCase() || "U"
    : "K";

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
            Kênh Người Bán
          </span>
        </div>

        {/* Right: User Profile & Support */}
        <div className="flex items-center gap-4">
          <Link
            href="/help"
            className="hidden sm:flex items-center gap-1.5 text-xs font-semibold text-[#737688] hover:text-[#0045cc] transition-colors"
          >
            <HelpCircle className="w-4 h-4" />
            <span>Hỗ trợ người bán</span>
          </Link>

          <div className="h-4 w-px bg-[#c3c5d9] hidden sm:block"></div>

          {isAuthenticated && user ? (
            <div className="flex items-center gap-2.5">
              {user.avatarUrl ? (
                <div className="relative w-8 h-8 rounded-full overflow-hidden ring-2 ring-[#eff4ff]">
                  <Image
                    src={user.avatarUrl}
                    alt={fullName}
                    fill
                    className="object-cover"
                  />
                </div>
              ) : (
                <div className="w-8 h-8 rounded-full bg-[#0045cc]/10 text-[#0045cc] font-heading font-bold text-xs flex items-center justify-center ring-2 ring-[#eff4ff]">
                  {initials}
                </div>
              )}
              <div className="hidden md:flex flex-col text-left">
                <span className="text-xs font-bold text-[#0b1c30] leading-tight line-clamp-1 max-w-[140px]">
                  {fullName}
                </span>
                <span className="text-[10px] text-[#737688]">Tài khoản người mua</span>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs font-semibold text-[#434656]">
              <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-500">
                <User className="w-4 h-4" />
              </div>
              <span className="hidden sm:inline">Chưa đăng nhập</span>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
