"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import {
  Search,
  Heart,
  ShoppingBag,
  Globe,
  ChevronDown,
  Truck,
  HelpCircle,
  User as UserIcon,
  LogOut,
  Package,
  MapPin,
  Store,
  ShieldCheck,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useWishlistIdsQuery } from "@/features/account/api/wishlistApi";
import { ROUTES } from "@/lib/constants";
import { UserAvatar } from "./UserAvatar";
import { NotificationBell } from "@/features/notifications/components";

export function Header() {
  const { toggleCart, totalCount } = useCart();
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const { data: wishlistData } = useWishlistIdsQuery();
  const wishlistCount = isAuthenticated ? (wishlistData?.totalCount || 0) : 0;
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState("");
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const q = params.get("q") || params.get("keyword") || "";
      if (q) setSearchTerm(q);
    }
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const query = searchTerm.trim();
    if (query) {
      router.push(`/search?q=${encodeURIComponent(query)}`);
    } else {
      router.push("/search");
    }
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const fullName = user
    ? `${user.firstName || ""} ${user.lastName || ""}`.trim() || user.email
    : "";

  const isSeller = user?.roles?.includes("SELLER");
  const isAdmin = user?.roles?.includes("ADMIN");

  return (
    <header className="fixed top-0 w-full z-50 bg-white border-b border-slate-200/70 shadow-[0_1px_8px_rgba(0,0,0,0.04)]">
      {/* Top utility sub-header */}
      <div className="bg-[#eff4ff] text-[#434656]">
        <div className="max-w-6xl mx-auto px-6 lg:px-12 h-9 flex items-center justify-between text-[11px] font-semibold">
          <div className="flex items-center gap-2 truncate">
            <span>
              Free express shipping on tech orders over $99 • 30-day buyer protection guarantee •{" "}
              <Link href={ROUTES.SELLER.REGISTER} className="text-[#0045cc] hover:underline font-semibold">
                Become a Verified Seller
              </Link>
            </span>
          </div>

          <div className="hidden md:flex items-center gap-6">
            <div className="flex items-center gap-1 cursor-pointer hover:text-[#0b1c30] transition-colors">
              <Globe className="w-3.5 h-3.5" />
              <span>EN / USD</span>
              <ChevronDown className="w-3 h-3" />
            </div>
            <Link href={ROUTES.ACCOUNT.ORDERS} className="hover:text-[#0b1c30] transition-colors flex items-center gap-1">
              <Truck className="w-3.5 h-3.5" />
              <span>Track Order</span>
            </Link>
            <Link href="#" className="hover:text-[#0b1c30] transition-colors flex items-center gap-1">
              <HelpCircle className="w-3.5 h-3.5" />
              <span>Help Center</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Main Omnisearch & Navigation header */}
      <div className="max-w-6xl mx-auto px-6 lg:px-12 h-20 flex items-center justify-between gap-6">
        {/* TechHub Logo */}
        <Link href="/" className="flex items-center gap-2.5 flex-shrink-0">
          <div className="relative w-8 h-8 flex items-center justify-center">
            <Image
              src="/logo.svg"
              alt="TechHub Logo"
              width={32}
              height={32}
              className="object-contain"
              priority
            />
          </div>
          <span className="text-2xl font-bold tracking-tight text-[#0b1c30] font-[family-name:var(--font-heading)]">
            Tech<span className="text-[#0045cc]">Hub</span>
          </span>
        </Link>

        {/* Omnisearch field */}
        <form onSubmit={handleSearchSubmit} className="flex-1 max-w-2xl relative">
          <div className="flex items-center rounded-lg bg-[#eff4ff] p-1 shadow-[0_1px_3px_rgba(15,23,42,0.04)] focus-within:ring-2 focus-within:ring-[#0045cc]/20">
            <div className="relative flex-1 flex items-center px-2 pl-3">
              <input
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-transparent text-sm text-[#0b1c30] placeholder:text-[#737688] focus:outline-none"
                placeholder="Tìm kiếm iPhone, MacBook, Sony, tai nghe, màn hình..."
                type="text"
              />
            </div>
            <button
              type="submit"
              className="flex items-center justify-center bg-[#0045cc] hover:bg-[#0d5bff] text-white px-5 py-2 rounded-lg transition-colors shadow-sm cursor-pointer"
              aria-label="Search"
            >
              <Search className="w-4 h-4" />
            </button>
          </div>
        </form>

        {/* Header Actions */}
        <div className="flex items-center gap-4 flex-shrink-0">
          {/* Notifications, Wishlist & Cart */}
          <div className="flex items-center gap-1.5 sm:gap-2">
            <NotificationBell portalContext="buyer" />

            <Link
              href={isAuthenticated ? ROUTES.ACCOUNT.WISHLIST : ROUTES.LOGIN}
              className="relative p-2 rounded-lg hover:bg-[#dce9ff] transition-colors text-[#434656] hover:text-[#0b1c30]"
              aria-label="Wishlist"
              title="Danh sách yêu thích"
            >
              <Heart className="w-5 h-5" />
              {wishlistCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-rose-500 text-white text-[10px] font-bold rounded-full h-4 min-w-[16px] px-1 flex items-center justify-center shadow-sm">
                  {wishlistCount > 99 ? "99+" : wishlistCount}
                </span>
              )}
            </Link>

            <button
              type="button"
              onClick={toggleCart}
              className="relative p-2 rounded-lg hover:bg-[#dce9ff] transition-colors text-[#434656] hover:text-[#0b1c30]"
              aria-label="Cart"
            >
              <ShoppingBag className="w-5 h-5" />
              {totalCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 bg-[#0045cc] text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                  {totalCount}
                </span>
              )}
            </button>
          </div>

          {/* User Profile Tile or Login/Register buttons */}
          <div className="pl-2 border-l border-slate-200">
            {isLoading ? (
              <div className="w-8 h-8 rounded-full bg-slate-200 animate-pulse" />
            ) : isAuthenticated && user ? (
              <div className="relative" ref={dropdownRef}>
                <button
                  type="button"
                  onClick={() => setDropdownOpen((prev) => !prev)}
                  className="flex items-center gap-2 cursor-pointer p-1 rounded-xl hover:bg-[#eff4ff] transition-colors"
                >
                  <UserAvatar
                    src={user.avatarUrl}
                    alt={fullName}
                    size="sm"
                  />
                  <div className="hidden md:flex flex-col text-left">
                    <span className="text-[13px] font-semibold text-[#0b1c30] leading-tight truncate max-w-[120px]">
                      {fullName}
                    </span>
                  </div>
                  <ChevronDown className="w-3.5 h-3.5 text-[#737688]" />
                </button>

                {/* Dropdown Menu */}
                {dropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.12)] border border-slate-100 py-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                    <div className="px-4 py-2 border-b border-slate-100">
                      <p className="text-xs font-semibold text-[#0b1c30] truncate">
                        {fullName}
                      </p>
                      <p className="text-[11px] text-[#737688] truncate">{user.email}</p>
                    </div>

                    <Link
                      href={ROUTES.ACCOUNT.PROFILE}
                      onClick={() => setDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2.5 text-xs text-[#434656] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors"
                    >
                      <UserIcon className="w-4 h-4 text-[#737688]" />
                      <span>Profile & Settings</span>
                    </Link>

                    <Link
                      href={ROUTES.ACCOUNT.ORDERS}
                      onClick={() => setDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2.5 text-xs text-[#434656] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors"
                    >
                      <Package className="w-4 h-4 text-[#737688]" />
                      <span>My Orders</span>
                    </Link>

                    <Link
                      href={ROUTES.ACCOUNT.ADDRESSES}
                      onClick={() => setDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2.5 text-xs text-[#434656] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors"
                    >
                      <MapPin className="w-4 h-4 text-[#737688]" />
                      <span>Saved Addresses</span>
                    </Link>

                    <Link
                      href={isSeller ? ROUTES.SELLER.DASHBOARD : ROUTES.SELLER.REGISTER}
                      onClick={() => setDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2.5 text-xs text-[#0045cc] font-medium hover:bg-[#eff4ff] transition-colors border-t border-slate-50"
                    >
                      <Store className="w-4 h-4" />
                      <span>{isSeller ? "Seller Portal" : "Become a Seller"}</span>
                    </Link>

                    {isAdmin && (
                      <Link
                        href={ROUTES.ADMIN.DASHBOARD}
                        onClick={() => setDropdownOpen(false)}
                        className="flex items-center gap-2.5 px-4 py-2.5 text-xs text-[#0b1c30] font-bold hover:bg-[#eff4ff] transition-colors border-t border-slate-50"
                      >
                        <ShieldCheck className="w-4 h-4 text-[#0045cc]" />
                        <span>Cổng Quản trị (Admin Portal)</span>
                      </Link>
                    )}

                    <button
                      type="button"
                      onClick={() => {
                        setDropdownOpen(false);
                        logout();
                      }}
                      className="w-full flex items-center gap-2.5 px-4 py-2.5 text-xs text-red-600 hover:bg-red-50 transition-colors border-t border-slate-100 text-left cursor-pointer"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>Sign Out</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  href={ROUTES.LOGIN}
                  className="px-3.5 py-2 text-xs font-semibold text-[#0b1c30] hover:text-[#0045cc] rounded-lg hover:bg-[#eff4ff] transition-colors"
                >
                  Log In
                </Link>
                <Link
                  href={ROUTES.REGISTER}
                  className="px-4 py-2 text-xs font-semibold text-white bg-[#0045cc] hover:bg-[#0d5bff] rounded-lg shadow-sm transition-colors"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
