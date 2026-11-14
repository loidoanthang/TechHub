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
  Heart,
  Store,
} from "lucide-react";
import { useProfileQuery } from "../hooks/useAccountQueries";
import { useWishlistIdsQuery } from "../api/wishlistApi";
import { useFollowedShopSlugsQuery } from "@/features/shops/api/shopFollowingApi";
import { useUnreadNotificationCount } from "@/features/notifications/api/notificationQueries";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { UserAvatar } from "@/components/shared";
import { ROUTES } from "@/lib/constants";

export function AccountSidebar() {
  const pathname = usePathname();
  const { data: profile } = useProfileQuery();
  const { data: wishlistData } = useWishlistIdsQuery();
  const { data: followedData } = useFollowedShopSlugsQuery();
  const { logout, isAuthenticated } = useAuth();
  const { badgeText: unreadBadge, hasUnread: hasUnreadNotif } =
    useUnreadNotificationCount({
      enabled: isAuthenticated,
    });

  const fullName = profile
    ? `${profile.firstName || ""} ${profile.lastName || ""}`.trim() || profile.email
    : "Tài khoản của tôi";

  const avatarUrl = profile?.avatarUrl || "/images/default-avatar.svg";

  const navItems = [
    {
      label: "Thông tin tài khoản",
      href: "/account/profile",
      icon: BadgeCheck,
      badge: null,
    },
    {
      label: "Đơn mua của tôi",
      href: "/account/orders",
      icon: Receipt,
      badge: null,
    },
    {
      label: "Thông báo của tôi",
      href: ROUTES.ACCOUNT.NOTIFICATIONS,
      icon: Bell,
      badge: hasUnreadNotif ? unreadBadge : null,
      hasAlertBadge: hasUnreadNotif,
    },
    {
      label: "Danh sách Yêu thích",
      href: "/account/wishlist",
      icon: Heart,
      badge: wishlistData?.totalCount ? String(wishlistData.totalCount) : null,
    },
    {
      label: "Shop đang theo dõi",
      href: "/account/followed-shops",
      icon: Store,
      badge: followedData?.totalCount ? String(followedData.totalCount) : null,
    },
    {
      label: "Sổ địa chỉ nhận hàng",
      href: "/account/addresses",
      icon: Truck,
      badge: null,
    },
    {
      label: "Bảo mật & Mật khẩu",
      href: "/account/security",
      icon: Lock,
      hasDot: false,
      badge: null,
    },
  ];

  return (
    <aside className="w-full flex flex-col gap-3.5 sticky top-[132px]">
      {/* Profile Anchor Bento Card */}
      <div className="bg-white rounded-2xl p-3.5 shadow-sm border border-[#c3c5d9]/25 relative overflow-hidden">
        <div className="absolute -right-6 -top-6 w-32 h-32 bg-[#0045cc]/5 rounded-full pointer-events-none blur-2xl"></div>
        <div className="flex items-center gap-3 relative z-10">
          <div className="relative flex-shrink-0">
            <UserAvatar
              src={avatarUrl}
              alt={fullName}
              size="md"
              className="ring-2 ring-[#0045cc]/20 shadow-sm"
            />
            {profile?.emailVerified && (
              <div
                className="absolute -bottom-1 -right-1 bg-[#0045cc] text-white w-4 h-4 rounded-full flex items-center justify-center shadow-sm"
                title="Identity Verified"
              >
                <BadgeCheck className="w-2.5 h-2.5" />
              </div>
            )}
          </div>
          <div className="flex flex-col min-w-0">
            <h3 className="font-heading text-xs sm:text-sm font-bold text-[#0b1c30] truncate">
              {fullName}
            </h3>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span className="text-[10px] font-bold text-[#006b5b] bg-[#eff4ff] px-1.5 py-0.5 rounded-full">
                {profile?.roles?.[0] || "BUYER"}
              </span>
              <span className="text-[10px] text-[#737688] font-mono">
                {profile?.status || "ACTIVE"}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Stack Bento Card */}
      <nav className="bg-white rounded-2xl p-2.5 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;
          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex items-center justify-between px-3 py-2.5 rounded-xl transition-all duration-150 text-xs font-semibold ${
                isActive
                  ? "bg-[#0045cc] text-white shadow-sm shadow-[#0045cc]/20"
                  : "text-[#0b1c30] hover:bg-[#eff4ff] hover:text-[#0045cc]"
              }`}
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <Icon
                  className={`w-4 h-4 shrink-0 ${
                    isActive ? "text-white" : "text-[#737688]"
                  }`}
                />
                <span className="truncate">{item.label}</span>
              </div>
              <div className="flex items-center gap-1.5 shrink-0 ml-1">
                {item.hasDot && !isActive && (
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                )}
                {item.badge !== null && item.badge !== undefined && (
                  <span
                    className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                      isActive
                        ? "bg-white text-[#0045cc]"
                        : item.hasAlertBadge
                        ? "bg-red-500 text-white"
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
            className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-semibold text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4 text-rose-500 shrink-0" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </nav>
    </aside>
  );
}
