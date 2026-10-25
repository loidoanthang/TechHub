"use client";

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
  Award,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";

export function Header() {
  const { toggleCart, totalCount } = useCart();
  return (
    <header className="fixed top-0 w-full z-50 bg-white/95 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.04)]">
      {/* Top utility sub-header */}
      <div className="bg-[#eff4ff] text-[#434656]">
        <div className="max-w-6xl mx-auto px-6 lg:px-12 h-9 flex items-center justify-between text-[11px] font-semibold">
          <div className="flex items-center gap-2 truncate">
            <span>
              Free express shipping on tech orders over $99 • 30-day buyer protection guarantee •{" "}
              <Link href="/seller/register" className="text-[#0045cc] hover:underline font-semibold">
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
            <Link href="/account/orders" className="hover:text-[#0b1c30] transition-colors flex items-center gap-1">
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
        <div className="flex-1 max-w-2xl relative">
          <div className="flex items-center rounded-lg bg-[#eff4ff] p-1 shadow-[0_1px_3px_rgba(15,23,42,0.04)] focus-within:ring-2 focus-within:ring-[#0045cc]/20">
            <div className="relative flex-1 flex items-center px-2 pl-3">
              <input
                className="w-full bg-transparent text-sm text-[#0b1c30] placeholder:text-[#737688] focus:outline-none"
                placeholder="Search ultra-gear, RTX 5090, Hall Effect, OLED..."
                type="text"
              />
            </div>
            <Link
              href="/search"
              className="flex items-center justify-center bg-[#0045cc] hover:bg-[#0d5bff] text-white px-5 py-2 rounded-lg transition-colors shadow-sm"
              aria-label="Search"
            >
              <Search className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {/* Header Actions */}
        <div className="flex items-center gap-4 flex-shrink-0">
          {/* Wishlist & Cart */}
          <div className="flex items-center gap-2">
            <Link
              href="/account/orders"
              className="relative p-2 rounded-lg hover:bg-[#dce9ff] transition-colors text-[#434656] hover:text-[#0b1c30]"
              aria-label="Wishlist"
            >
              <Heart className="w-5 h-5" />
              <span className="absolute top-1 right-1 w-4 h-4 bg-[#a90900] text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                3
              </span>
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

          {/* User Profile Tile */}
          <Link href="/account" className="flex items-center gap-2.5 pl-2 border-l border-slate-200">
            <div className="w-8 h-8 rounded-full bg-slate-300 relative overflow-hidden flex-shrink-0">
              <Image
                src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop"
                alt="Alex M."
                fill
                className="object-cover"
                unoptimized
              />
            </div>
            <div className="hidden md:flex flex-col text-left">
              <span className="text-[13px] font-semibold text-[#0b1c30] leading-tight">Alex M.</span>
            </div>
          </Link>
        </div>
      </div>
    </header>
  );
}
