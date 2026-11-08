"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  PackageCheck,
  Boxes,
  PlusCircle,
  TicketPercent,
  Settings,
  ExternalLink,
  Store,
  Menu,
  X,
  ChevronDown,
  LogOut,
  ArrowLeft,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  ShieldAlert,
  Loader2,
  FileX,
  RotateCcw,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useSellerShopQuery, useUpdateShopStatusMutation } from "../hooks/useSellerQueries";
import { ROUTES } from "@/lib/constants";
import type { ShopStatus } from "@/types/enums";

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: number;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

export function SellerLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, isLoading: isAuthLoading, isAuthenticated, logout, refreshProfile } = useAuth();
  const { data: shop, isLoading: isShopLoading } = useSellerShopQuery();
  const updateStatusMutation = useUpdateShopStatusMutation();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);

  // Auto-sync profile if authenticated buyer might have just been approved as SELLER
  const hasRefreshedRef = React.useRef(false);
  React.useEffect(() => {
    if (isAuthenticated && user && !user.roles?.includes("SELLER") && !hasRefreshedRef.current) {
      hasRefreshedRef.current = true;
      refreshProfile();
    }
  }, [isAuthenticated, user, refreshProfile]);

  // If on seller registration page, do not wrap in sidebar layout
  if (pathname === "/seller/register" || pathname.startsWith("/seller/register/")) {
    return <>{children}</>;
  }

  // Loading state
  if (isAuthLoading || (isAuthenticated && isShopLoading)) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
        <span className="text-xs font-semibold text-[#737688]">
          Đang tải Kênh Người Bán TechHub...
        </span>
      </div>
    );
  }

  // Unauthenticated Guard
  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] flex flex-col items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full border border-[#c3c5d9]/30 shadow-sm text-center flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
            <Store className="w-6 h-6" />
          </div>
          <h2 className="font-heading text-lg font-bold text-[#0b1c30]">
            Yêu cầu Đăng nhập Kênh Người Bán
          </h2>
          <p className="text-xs text-[#737688] leading-relaxed">
            Vui lòng đăng nhập với tài khoản đối tác bán hàng TechHub để truy cập trang quản trị.
          </p>
          <Link
            href={`${ROUTES.LOGIN}?returnUrl=/seller/dashboard`}
            className="w-full py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all text-center"
          >
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    );
  }

  // Not a Seller Guard
  const isSeller = user.roles?.includes("SELLER");
  if (!isSeller) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] flex flex-col items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full border border-[#c3c5d9]/30 shadow-sm text-center flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
            <Store className="w-6 h-6" />
          </div>
          <h2 className="font-heading text-lg font-bold text-[#0b1c30]">
            Chưa Đăng Ký Kênh Người Bán
          </h2>
          <p className="text-xs text-[#737688] leading-relaxed">
            Tài khoản hiện tại của bạn chưa mở gian hàng trên TechHub. Hãy hoàn tất hồ sơ đăng ký đối tác để bắt đầu kinh doanh.
          </p>
          <Link
            href={ROUTES.SELLER.REGISTER}
            className="w-full py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all text-center"
          >
            Đăng ký bán hàng ngay
          </Link>
          <Link
            href={ROUTES.HOME}
            className="text-xs font-semibold text-[#737688] hover:text-[#0045cc]"
          >
            Quay lại sàn mua hàng
          </Link>
        </div>
      </div>
    );
  }

  const currentStatus: ShopStatus = shop?.status || "ACTIVE";

  const handleToggleStatus = (newStatus: ShopStatus) => {
    setStatusDropdownOpen(false);
    if (newStatus !== currentStatus) {
      updateStatusMutation.mutate(newStatus);
    }
  };

  const navSections: NavSection[] = [
    {
      title: "TỔNG QUAN",
      items: [
        {
          label: "Tổng quan & Chỉ số",
          href: ROUTES.SELLER.DASHBOARD,
          icon: LayoutDashboard,
        },
      ],
    },
    {
      title: "QUẢN LÝ ĐƠN HÀNG",
      items: [
        {
          label: "Tất cả đơn hàng",
          href: "/seller/orders",
          icon: PackageCheck,
        },
        {
          label: "Yêu cầu hủy đơn",
          href: "/seller/orders?tab=cancellations",
          icon: FileX,
        },
        {
          label: "Yêu cầu trả hàng",
          href: "/seller/orders?tab=returns",
          icon: RotateCcw,
        },
      ],
    },
    {
      title: "QUẢN LÝ SẢN PHẨM",
      items: [
        {
          label: "Tất cả sản phẩm",
          href: "/seller/products",
          icon: Boxes,
        },
        {
          label: "Thêm sản phẩm mới",
          href: "/seller/products/new",
          icon: PlusCircle,
        },
      ],
    },
    {
      title: "MARKETING & CÀI ĐẶT",
      items: [
        {
          label: "Mã giảm giá Shop",
          href: "/seller/vouchers",
          icon: TicketPercent,
        },
        {
          label: "Kho hàng & Vận chuyển",
          href: "/seller/settings",
          icon: Settings,
        },
      ],
    },
  ];

  return (
    <div className="min-h-screen bg-[#f8f9fc] flex flex-col">
      {/* 1. TOP HEADER */}
      <header className="sticky top-0 z-40 w-full bg-white/95 backdrop-blur-md border-b border-[#e2e8f0] shadow-xs">
        <div className="px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Left: Mobile hamburger & Logo */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden p-2 rounded-xl text-[#434656] hover:bg-[#eff4ff] transition-colors"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>

            <Link href={ROUTES.SELLER.DASHBOARD} className="flex items-center gap-2 group">
              <span className="font-heading text-xl font-extrabold tracking-tight text-[#0b1c30]">
                TECH<span className="text-[#0045cc]">HUB</span>
              </span>
              <span className="h-4 w-px bg-[#c3c5d9]"></span>
              <span className="text-[11px] font-extrabold text-[#0045cc] uppercase tracking-wider bg-[#eff4ff] px-2 py-0.5 rounded-md border border-[#0045cc]/20">
                Seller Centre
              </span>
            </Link>
          </div>

          {/* Center/Right: Shop Status, Public Shop Link & Profile */}
          <div className="flex items-center gap-3 sm:gap-4">
            {/* Shop Status Selector */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setStatusDropdownOpen(!statusDropdownOpen)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
                  currentStatus === "ACTIVE"
                    ? "bg-[#006b5b]/10 text-[#006b5b] border-[#006b5b]/20 hover:bg-[#006b5b]/20"
                    : currentStatus === "PAUSED"
                    ? "bg-amber-50 text-amber-700 border-amber-300 hover:bg-amber-100"
                    : "bg-red-50 text-red-700 border-red-300"
                }`}
                disabled={currentStatus === "SUSPENDED" || updateStatusMutation.isPending}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    currentStatus === "ACTIVE"
                      ? "bg-[#006b5b] animate-pulse"
                      : currentStatus === "PAUSED"
                      ? "bg-amber-500"
                      : "bg-red-500"
                  }`}
                />
                <span className="hidden sm:inline">
                  {currentStatus === "ACTIVE"
                    ? "Đang mở cửa"
                    : currentStatus === "PAUSED"
                    ? "Tạm nghỉ"
                    : "Bị đình chỉ"}
                </span>
                {currentStatus !== "SUSPENDED" && <ChevronDown className="w-3.5 h-3.5" />}
              </button>

              {statusDropdownOpen && currentStatus !== "SUSPENDED" && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-2xl shadow-xl border border-[#e2e8f0] p-1.5 z-50 animate-in fade-in zoom-in-95">
                  <button
                    type="button"
                    onClick={() => handleToggleStatus("ACTIVE")}
                    className="w-full text-left px-3 py-2 text-xs font-semibold text-[#0b1c30] hover:bg-[#eff4ff] rounded-xl flex items-center gap-2 transition-colors"
                  >
                    <span className="w-2 h-2 rounded-full bg-[#006b5b]" />
                    <span>Mở cửa kinh doanh</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleToggleStatus("PAUSED")}
                    className="w-full text-left px-3 py-2 text-xs font-semibold text-amber-800 hover:bg-amber-50 rounded-xl flex items-center gap-2 transition-colors"
                  >
                    <span className="w-2 h-2 rounded-full bg-amber-500" />
                    <span>Tạm nghỉ bán hàng</span>
                  </button>
                </div>
              )}
            </div>

            {/* Link to Public Store */}
            {shop?.sellerName && (
              <Link
                href={ROUTES.SHOPS(shop.sellerName)}
                target="_blank"
                className="hidden md:flex items-center gap-1.5 text-xs font-semibold text-[#434656] hover:text-[#0045cc] bg-white border border-[#e2e8f0] px-3 py-1.5 rounded-xl hover:border-[#0045cc]/30 transition-all shadow-2xs"
              >
                <span>Xem gian hàng</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </Link>
            )}

            {/* Back to Marketplace */}
            <Link
              href={ROUTES.HOME}
              className="hidden lg:flex items-center gap-1.5 text-xs font-medium text-[#737688] hover:text-[#0b1c30] transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Về sàn mua hàng</span>
            </Link>

            <div className="h-4 w-px bg-[#c3c5d9]"></div>

            {/* User Profile */}
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] font-heading font-bold text-xs flex items-center justify-center ring-2 ring-[#eff4ff] overflow-hidden">
                {shop?.avatarUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={shop.avatarUrl} alt={shop.shopName} className="w-full h-full object-cover" />
                ) : (
                  (shop?.shopName || user.firstName || "S").charAt(0).toUpperCase()
                )}
              </div>
              <div className="hidden sm:flex flex-col text-left">
                <span className="text-xs font-bold text-[#0b1c30] leading-tight line-clamp-1 max-w-[120px]">
                  {shop?.shopName || "Gian hàng của tôi"}
                </span>
                <span className="text-[10px] text-[#737688]">Kênh Người Bán</span>
              </div>
              <button
                type="button"
                onClick={logout}
                title="Đăng xuất"
                className="p-1.5 rounded-xl text-[#737688] hover:text-[#ba1a1a] hover:bg-red-50 transition-colors"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* 2. BODY WITH SIDEBAR & CONTENT */}
      <div className="flex-1 flex max-w-[1600px] w-full mx-auto">
        {/* DESKTOP SIDEBAR */}
        <aside className="hidden lg:block w-56 bg-white border-r border-[#e2e8f0] shrink-0 p-3">
          <div className="sticky top-20 flex flex-col gap-5">
            {/* Shop Summary Card */}
            <div className="bg-[#eff4ff]/60 rounded-xl p-3 border border-[#0045cc]/10 flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-white text-[#0045cc] font-bold text-xs flex items-center justify-center shadow-2xs">
                  <Store className="w-3.5 h-3.5" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-xs font-bold text-[#0b1c30] truncate">
                    {shop?.shopName || "TechHub Shop"}
                  </h3>
                  <p className="text-[10px] text-[#737688]">
                    {shop?.rating ? `⭐ ${shop.rating.toFixed(1)}/5` : "Đối tác chính thức"}
                  </p>
                </div>
              </div>
            </div>

            {/* Navigation Groups */}
            <nav className="flex flex-col gap-4">
              {navSections.map((section) => (
                <div key={section.title} className="flex flex-col gap-0.5">
                  <span className="text-[10px] font-extrabold text-[#737688] tracking-wider px-2.5 mb-1">
                    {section.title}
                  </span>
                  {section.items.map((item) => {
                    const isActive = pathname === item.href;
                    const Icon = item.icon;
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={`flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-semibold transition-all ${
                          isActive
                            ? "bg-[#0045cc] text-white shadow-xs font-bold"
                            : "text-[#434656] hover:bg-[#eff4ff] hover:text-[#0045cc]"
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <Icon className={`w-4 h-4 shrink-0 ${isActive ? "text-white" : "text-[#737688]"}`} />
                          <span className="truncate">{item.label}</span>
                        </div>
                        {item.badge !== undefined && item.badge > 0 && (
                          <span
                            className={`text-[10px] px-1.5 py-0.5 rounded-full font-bold shrink-0 ${
                              isActive ? "bg-white text-[#0045cc]" : "bg-red-500 text-white"
                            }`}
                          >
                            {item.badge}
                          </span>
                        )}
                      </Link>
                    );
                  })}
                </div>
              ))}
            </nav>
          </div>
        </aside>

        {/* MOBILE SIDEBAR DRAWER */}
        {mobileMenuOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div
              className="fixed inset-0 bg-black/40 backdrop-blur-xs"
              onClick={() => setMobileMenuOpen(false)}
            />
            <div className="fixed inset-y-0 left-0 w-72 bg-white p-5 flex flex-col gap-5 z-50 overflow-y-auto">
              <div className="flex items-center justify-between border-b border-[#e2e8f0] pb-4">
                <span className="font-heading font-extrabold text-[#0b1c30]">
                  Kênh Người Bán
                </span>
                <button
                  type="button"
                  onClick={() => setMobileMenuOpen(false)}
                  className="p-1 rounded-lg text-[#737688] hover:bg-[#eff4ff]"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <nav className="flex flex-col gap-5">
                {navSections.map((section) => (
                  <div key={section.title} className="flex flex-col gap-1">
                    <span className="text-[10px] font-extrabold text-[#737688] tracking-wider px-3 mb-1">
                      {section.title}
                    </span>
                    {section.items.map((item) => {
                      const isActive = pathname === item.href;
                      const Icon = item.icon;
                      return (
                        <Link
                          key={item.href}
                          href={item.href}
                          onClick={() => setMobileMenuOpen(false)}
                          className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold ${
                            isActive
                              ? "bg-[#0045cc] text-white"
                              : "text-[#434656] hover:bg-[#eff4ff]"
                          }`}
                        >
                          <div className="flex items-center gap-2.5">
                            <Icon className="w-4 h-4" />
                            <span>{item.label}</span>
                          </div>
                        </Link>
                      );
                    })}
                  </div>
                ))}
              </nav>

              <div className="mt-auto border-t border-[#e2e8f0] pt-4 flex flex-col gap-2">
                <Link
                  href={ROUTES.HOME}
                  className="flex items-center gap-2 text-xs font-semibold text-[#737688] hover:text-[#0b1c30] px-3 py-2"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Về sàn mua sắm</span>
                </Link>
              </div>
            </div>
          </div>
        )}

        {/* 3. MAIN WORKSPACE */}
        <main className="flex-1 min-w-0 p-4 sm:p-5 lg:p-6">
          {/* PAUSED SHOP BANNER */}
          {currentStatus === "PAUSED" && (
            <div className="mb-6 p-4 rounded-2xl bg-amber-50 border border-amber-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-in fade-in">
              <div className="flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
                <p className="text-xs font-medium text-amber-900">
                  <strong>Gian hàng đang Tạm Nghỉ Bán:</strong> Người mua không thể đặt đơn hàng mới. Bạn có thể mở bán lại bất kỳ lúc nào.
                </p>
              </div>
              <button
                type="button"
                onClick={() => handleToggleStatus("ACTIVE")}
                className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition-colors shrink-0 shadow-2xs"
              >
                Mở bán lại ngay
              </button>
            </div>
          )}

          {/* SUSPENDED SHOP BANNER */}
          {currentStatus === "SUSPENDED" && (
            <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-center gap-3 animate-in fade-in">
              <ShieldAlert className="w-5 h-5 text-red-600 shrink-0" />
              <p className="text-xs font-medium text-red-900">
                <strong>Gian hàng bị Đình Chỉ:</strong> Gian hàng đã bị tạm ngưng bởi Quản trị viên TechHub do vi phạm chính sách hoặc chờ đối soát tài chính. Mọi tính năng tạo đơn và đăng bán đều bị khóa.
              </p>
            </div>
          )}

          {children}
        </main>
      </div>
    </div>
  );
}
