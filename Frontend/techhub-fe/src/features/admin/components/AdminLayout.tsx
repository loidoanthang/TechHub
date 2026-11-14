"use client";

import React, { useState } from "react";
import Link, { useLinkStatus } from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  ShieldCheck,
  PackageCheck,
  Store,
  FolderTree,
  ShoppingBag,
  Users,
  Wallet,
  Settings,
  Menu,
  X,
  LogOut,
  ArrowLeft,
  Loader2,
  ShieldAlert,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";
import { useAdminDialogA11y } from "../hooks/useAdminDialogA11y";
import { NotificationBell } from "@/features/notifications/components";

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: "TỔNG QUAN SÀN",
    items: [
      {
        label: "Dashboard Hệ thống",
        href: ROUTES.ADMIN.DASHBOARD,
        icon: LayoutDashboard,
      },
    ],
  },
  {
    title: "KIỂM DUYỆT & ĐỐI TÁC",
    items: [
      {
        label: "Kiểm duyệt Sản phẩm",
        href: ROUTES.ADMIN.PRODUCTS,
        icon: PackageCheck,
      },
      {
        label: "Đối tác & Gian hàng",
        href: ROUTES.ADMIN.SELLERS,
        icon: Store,
      },
    ],
  },
  {
    title: "DANH MỤC, ĐƠN & USER",
    items: [
      {
        label: "Danh mục & Thuộc tính",
        href: ROUTES.ADMIN.CATEGORIES,
        icon: FolderTree,
      },
      {
        label: "Đơn hàng Toàn sàn",
        href: ROUTES.ADMIN.ORDERS,
        icon: ShoppingBag,
      },
      {
        label: "Quản lý Người dùng",
        href: ROUTES.ADMIN.USERS,
        icon: Users,
      },
    ],
  },
  {
    title: "TÀI CHÍNH & CẤU HÌNH",
    items: [
      {
        label: "Tài chính & Voucher sàn",
        href: ROUTES.ADMIN.FINANCE,
        icon: Wallet,
      },
      {
        label: "Chính sách & Vận hành",
        href: ROUTES.ADMIN.SETTINGS,
        icon: Settings,
      },
    ],
  },
];

function NavLinkHint() {
  const { pending } = useLinkStatus();
  if (!pending) return null;
  return (
    <Loader2 className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none shrink-0 opacity-90" />
  );
}

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, isLoading: isAuthLoading, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [pendingNav, setPendingNav] = useState<{
    href: string;
    from: string;
  } | null>(null);

  const pendingHref =
    pendingNav && pendingNav.from === pathname ? pendingNav.href : null;

  useAdminDialogA11y(mobileMenuOpen, () => setMobileMenuOpen(false));

  if (isAuthLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-primary animate-spin motion-reduce:animate-none" />
        <span className="text-xs font-semibold text-slate-600">
          Đang xác thực quyền Quản trị viên TechHub...
        </span>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full border border-slate-200 shadow-sm text-center flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-on-surface text-white flex items-center justify-center">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <h2 className="font-heading text-lg font-bold text-on-surface">
            Đăng nhập Cổng Quản Trị
          </h2>
          <p className="text-xs text-slate-600 leading-relaxed">
            Vui lòng đăng nhập bằng tài khoản Quản trị viên (ADMIN) để truy cập trung tâm điều hành TechHub.
          </p>
          <Link
            href={`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(pathname || ROUTES.ADMIN.DASHBOARD)}`}
            className="w-full py-2.5 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-colors duration-150 text-center"
          >
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    );
  }

  const isAdmin = user.roles?.includes("ADMIN");
  if (!isAdmin) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] flex flex-col items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full border border-rose-200 shadow-sm text-center flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h2 className="font-heading text-lg font-bold text-[#0b1c30]">
            Không có quyền truy cập (403)
          </h2>
          <p className="text-xs text-slate-500 leading-relaxed">
            Tài khoản <strong className="text-slate-700">{user.email}</strong> không thuộc nhóm Quản trị viên Hệ thống.
          </p>
          <Link
            href={ROUTES.HOME}
            className="w-full py-2.5 rounded-xl bg-[#0b1c30] hover:bg-slate-800 text-white text-xs font-bold transition-colors duration-150 text-center"
          >
            Quay về Trang chủ Sàn
          </Link>
        </div>
      </div>
    );
  }

  const adminName =
    `${user.firstName || ""} ${user.lastName || ""}`.trim() || user.email;

  const renderNavLinks = (onItemClick?: () => void) => (
    <nav className="flex flex-col gap-4">
      {NAV_SECTIONS.map((section) => (
        <div key={section.title} className="flex flex-col gap-0.5">
          <span className="text-[10px] font-extrabold text-slate-600 tracking-wider px-2.5 mb-1 uppercase">
            {section.title}
          </span>
          {section.items.map((item) => {
            const isRouteMatch =
              pathname === item.href || pathname.startsWith(`${item.href}/`);
            const isActive = pendingHref
              ? pendingHref === item.href
              : isRouteMatch;
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                prefetch={true}
                onClick={() => {
                  if (!isRouteMatch) {
                    setPendingNav({ href: item.href, from: pathname });
                  }
                  onItemClick?.();
                }}
                className={`flex items-center justify-between px-2.5 py-2.5 rounded-xl text-xs font-semibold transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                  isActive
                    ? "bg-primary text-white shadow-xs font-bold"
                    : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                }`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <Icon
                    className={`w-4 h-4 shrink-0 ${
                      isActive ? "text-white" : "text-slate-500"
                    }`}
                  />
                  <span className="truncate">{item.label}</span>
                </div>
                <NavLinkHint />
              </Link>
            );
          })}
        </div>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {pendingHref && (
        <div
          aria-hidden
          className="fixed top-0 left-0 right-0 h-0.5 bg-primary-container z-50 animate-pulse motion-reduce:animate-none"
        />
      )}

      {/* 1. TOP HEADER (Sticky on native document scroll with isolated GPU layer) */}
      <header className="sticky top-0 z-30 w-full bg-white border-b border-slate-200 shadow-xs [transform:translateZ(0)]">
        <div className="px-4 sm:px-6 h-[60px] flex items-center justify-between gap-4">
          {/* Left: Hamburger + Brand */}
          <div className="flex items-center gap-3 min-w-0">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-on-surface-variant hover:bg-surface-container-low transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              aria-label="Mở menu quản trị"
              aria-expanded={mobileMenuOpen}
            >
              {mobileMenuOpen ? (
                <X className="w-5 h-5" />
              ) : (
                <Menu className="w-5 h-5" />
              )}
            </button>

            <Link
              href={ROUTES.ADMIN.DASHBOARD}
              className="flex items-center gap-2 min-w-0 group"
            >
              <span className="font-heading text-xl font-extrabold tracking-tight text-on-surface">
                TECH<span className="text-primary">HUB</span>
              </span>
              <span className="h-4 w-px bg-outline-variant hidden sm:inline-block" />
              <span className="text-[11px] font-extrabold text-primary uppercase tracking-wider bg-surface-container-low px-2 py-0.5 rounded-md border border-primary/20 flex items-center gap-1 shrink-0">
                <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
                <span>
                  Admin<span className="hidden sm:inline"> Portal</span>
                </span>
              </span>
            </Link>
          </div>

          {/* Right: Storefront link & Admin Profile */}
          <div className="flex items-center gap-3 sm:gap-4 shrink-0">
            <Link
              href={ROUTES.HOME}
              className="hidden lg:flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-on-surface transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Về sàn mua hàng</span>
            </Link>

            <div className="h-4 w-px bg-outline-variant hidden lg:block" />

            {/* Notification Bell */}
            <NotificationBell portalContext="admin" />

            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-primary/10 text-primary font-heading font-bold text-xs flex items-center justify-center ring-2 ring-surface-container-low shrink-0">
                {adminName.charAt(0).toUpperCase()}
              </div>
              <div className="hidden sm:flex flex-col text-left">
                <span className="text-xs font-bold text-on-surface leading-tight truncate max-w-[140px]">
                  {adminName}
                </span>
                <span className="text-[10px] text-slate-600">
                  Quản trị viên Hệ thống
                </span>
              </div>
              <button
                type="button"
                onClick={logout}
                title="Đăng xuất"
                aria-label="Đăng xuất"
                className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-600 hover:text-red-700 hover:bg-red-50 transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* 2. BODY WITH SIDEBAR & CONTENT (Native window document scroll for GPU compositor smoothness) */}
      <div className="flex-1 flex max-w-[1600px] w-full mx-auto">
        {/* DESKTOP SIDEBAR */}
        <aside className="hidden lg:block w-56 bg-white border-r border-slate-200 shrink-0">
          <div className="sticky top-[60px] max-h-[calc(100vh-60px)] overflow-y-auto no-scrollbar p-3 flex flex-col gap-5">
            {/* Admin Summary Card */}
            <div className="bg-surface-container-low/60 rounded-xl p-3 border border-primary/10 flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-white text-primary font-bold text-xs flex items-center justify-center shadow-2xs shrink-0">
                  <ShieldCheck className="w-3.5 h-3.5" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-xs font-bold text-on-surface truncate">
                    Trung tâm Điều hành
                  </h3>
                  <p className="text-[10px] text-slate-600 truncate">
                    TechHub Admin Portal
                  </p>
                </div>
              </div>
            </div>

            {renderNavLinks()}
          </div>
        </aside>

        {/* MOBILE SIDEBAR DRAWER */}
        {mobileMenuOpen && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Menu điều hướng Quản trị"
            className="fixed inset-0 z-50 lg:hidden"
          >
            <div
              className="fixed inset-0 bg-slate-950/45"
              onClick={() => setMobileMenuOpen(false)}
            />
            <div className="fixed inset-y-0 left-0 w-72 bg-white p-5 flex flex-col gap-5 z-50 overflow-y-auto shadow-xl">
              <div className="flex items-center justify-between border-b border-slate-200 pb-4">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-primary" />
                  <span className="font-heading font-extrabold text-on-surface">
                    TechHub Admin Portal
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setMobileMenuOpen(false)}
                  aria-label="Đóng menu quản trị"
                  className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-600 hover:bg-surface-container-low cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {renderNavLinks(() => setMobileMenuOpen(false))}

              <div className="mt-auto border-t border-slate-200 pt-4 flex flex-col gap-2">
                <Link
                  href={ROUTES.HOME}
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-on-surface px-3 py-2"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Về sàn mua sắm</span>
                </Link>
              </div>
            </div>
          </div>
        )}

        {/* 3. MAIN WORKSPACE */}
        <main className="flex-1 min-w-0 flex flex-col">
          {children}
        </main>
      </div>
    </div>
  );
}
