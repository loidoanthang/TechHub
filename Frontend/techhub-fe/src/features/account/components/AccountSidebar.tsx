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
import { mockUserProfile } from "@/mocks";

export function AccountSidebar() {
  const pathname = usePathname();
  const profile = mockUserProfile;

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
      badge: profile.ordersCount,
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
      label: "Security & Password",
      href: "#",
      icon: Lock,
      hasDot: true,
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
                src={profile.avatarUrl}
                alt={`${profile.firstName} ${profile.lastName}`}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
            {profile.emailVerified && (
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
              {profile.firstName} {profile.lastName}
            </h3>
            <div className="flex items-center gap-1.5 mt-1">
              <span className="text-[11px] font-bold text-[#006b5b] bg-[#eff4ff] px-2 py-0.5 rounded-full">
                {profile.tier}
              </span>
              <span className="text-xs text-[#737688] font-mono">
                {profile.status}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Rail Container */}
      <nav className="bg-white rounded-2xl p-2 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex items-center justify-between px-4 py-3 rounded-xl transition-all font-medium text-sm group ${
                isActive
                  ? "bg-[#0045cc] text-white font-semibold shadow-sm"
                  : "text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30]"
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <Icon
                  className={`w-5 h-5 shrink-0 transition-colors ${
                    isActive
                      ? "text-white"
                      : "text-[#737688] group-hover:text-[#0045cc]"
                  }`}
                />
                <span className="truncate">{item.label}</span>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                {item.hasDot && (
                  <span
                    className="w-2 h-2 rounded-full bg-emerald-500"
                    title="2FA Active"
                  ></span>
                )}
                {item.badge !== null && (
                  <span
                    className={`text-xs px-2 py-0.5 rounded-full font-bold ${
                      isActive
                        ? "bg-white/20 text-white"
                        : "bg-[#eff4ff] text-[#0045cc]"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
                <ChevronRight
                  className={`w-4 h-4 transition-transform ${
                    isActive
                      ? "text-white"
                      : "text-[#737688] opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5"
                  }`}
                />
              </div>
            </Link>
          );
        })}

        <div className="my-1.5 bg-[#eff4ff] h-[1px]"></div>

        {/* Sign Out Button */}
        <Link
          href="/login"
          className="flex items-center gap-3 px-4 py-3 rounded-xl text-[#ba1a1a] hover:bg-[#ffdad6]/40 transition-colors font-medium text-sm"
        >
          <LogOut className="w-5 h-5 shrink-0" />
          <span>Sign Out</span>
        </Link>
      </nav>
    </aside>
  );
}
