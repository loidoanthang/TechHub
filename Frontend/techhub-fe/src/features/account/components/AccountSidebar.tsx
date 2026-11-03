"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  BadgeCheck,
  Receipt,
  Truck,
  CreditCard,
  Lock,
  Bell,
  LogOut,
  ChevronRight,
} from "lucide-react";
import { useProfileQuery } from "../hooks/useAccountQueries";
import { useAuth } from "@/features/auth/hooks/useAuth";

export function AccountSidebar() {
  const pathname = usePathname();
  const { data: profile } = useProfileQuery();
  const { logout } = useAuth();

  const fullName = profile
    ? `${profile.firstName || ""} ${profile.lastName || ""}`.trim() || profile.email
    : "My Account";

  const avatarUrl =
    profile?.avatarUrl ||
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80";

  const navItems = [
    {
      label: "Account Information",
      href: "/account/profile",
      icon: BadgeCheck,
      badge: null,
    },
    {
      label: "My Purchases & Orders",
      href: "/account/orders",
      icon: Receipt,
      badge: null,
    },
    {
      label: "Shipping Addresses",
      href: "/account/addresses",
      icon: Truck,
      badge: null,
    },
    {
      label: "Banks & Cards",
      href: "#",
      icon: CreditCard,
      badge: null,
    },
    {
      label: "Bảo mật & Mật khẩu",
      href: "/account/security",
      icon: Lock,
      hasDot: false,
      badge: null,
    },
    {
      label: "Notifications & Alerts",
      href: "#",
      icon: Bell,
      badge: null,
    },
  ];

  return (
    <aside className="w-full flex flex-col gap-4 sticky top-[132px]">
      {/* Profile Anchor Bento Card */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#c3c5d9]/25 relative overflow-hidden">
        <div className="absolute -right-6 -top-6 w-32 h-32 bg-[#0045cc]/5 rounded-full pointer-events-none blur-2xl"></div>
        <div className="flex items-center gap-4 relative z-10">
          <div className="relative flex-shrink-0">
            <div className="w-16 h-16 rounded-full overflow-hidden ring-2 ring-[#0045cc]/20 relative shadow-sm">
              <Image
                src={avatarUrl}
                alt={fullName}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
            {profile?.emailVerified && (
              <div
                className="absolute -bottom-1 -right-1 bg-[#0045cc] text-white w-5 h-5 rounded-full flex items-center justify-center shadow-sm"
                title="Identity Verified"
              >
                <BadgeCheck className="w-3.5 h-3.5" />
              </div>
            )}
          </div>
          <div className="flex flex-col min-w-0">
            <h3 className="font-heading text-lg font-bold text-[#0b1c30] truncate">
              {fullName}
            </h3>
            <div className="flex items-center gap-1.5 mt-1">
              <span className="text-[11px] font-bold text-[#006b5b] bg-[#eff4ff] px-2 py-0.5 rounded-full">
                {profile?.roles?.[0] || "BUYER"}
              </span>
              <span className="text-xs text-[#737688] font-mono">
                {profile?.status || "ACTIVE"}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Stack Bento Card */}
      <nav className="bg-white rounded-2xl p-3 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;
          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex items-center justify-between px-3.5 py-3 rounded-xl transition-all duration-150 text-xs font-semibold ${
                isActive
                  ? "bg-[#0045cc] text-white shadow-sm shadow-[#0045cc]/20"
                  : "text-[#0b1c30] hover:bg-[#eff4ff] hover:text-[#0045cc]"
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon
                  className={`w-4 h-4 ${
                    isActive ? "text-white" : "text-[#737688]"
                  }`}
                />
                <span>{item.label}</span>
              </div>
              <div className="flex items-center gap-2">
                {item.hasDot && !isActive && (
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                )}
                {item.badge !== null && item.badge !== undefined && (
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      isActive
                        ? "bg-white text-[#0045cc]"
                        : "bg-[#eff4ff] text-[#0045cc]"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </div>
            </Link>
          );
        })}

        {/* Sign Out Action */}
        <div className="pt-2 mt-1 border-t border-[#eff4ff]">
          <button
            onClick={() => logout()}
            className="w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4 text-rose-500" />
            <span>Sign Out</span>
          </button>
        </div>
      </nav>
    </aside>
  );
}
