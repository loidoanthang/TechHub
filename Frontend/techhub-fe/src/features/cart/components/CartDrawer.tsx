"use client";

import Link from "next/link";
import Image from "next/image";
import {
  X,
  Trash2,
  Plus,
  Minus,
  ArrowRight,
  ShoppingBag,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { useCart } from "../context/CartContext";

export function CartDrawer() {
  const {
    isOpen,
    closeCart,
    items,
    updateQuantity,
    removeItem,
    subtotal,
    totalCount,
    isLoading,
    error,
    clearError,
  } = useCart();

  const formattedSubtotal = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(subtotal);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Dimmed Backdrop */}
      <div
        className="fixed inset-0 bg-[#213145]/40 backdrop-blur-sm transition-opacity duration-300"
        onClick={closeCart}
        aria-hidden="true"
      />

      {/* Right Side Drawer */}
      <aside className="relative z-10 w-[420px] max-w-[90vw] h-full bg-white shadow-2xl flex flex-col justify-between transform transition-transform duration-300 ease-out border-l border-[#eff4ff]">
        {/* Drawer Header */}
        <div className="px-6 py-5 flex items-center justify-between border-b border-[#eff4ff]">
          <div className="flex items-center gap-2.5">
            <h2 className="font-bold text-xl tracking-tight text-[#0b1c30] font-[family-name:var(--font-heading)]">
              Shopping Cart
            </h2>
            <span className="inline-flex items-center justify-center px-2 py-0.5 text-xs font-semibold rounded-full bg-[#dce1ff] text-[#003bb0]">
              {totalCount}
            </span>
          </div>

          <button
            onClick={closeCart}
            aria-label="Close cart drawer"
            className="p-2 rounded-lg text-[#434656] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error notification banner */}
        {error && (
          <div className="mx-6 mt-4 p-3 rounded-xl bg-red-50 border border-red-200 text-red-600 text-xs flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={clearError}
              className="p-1 hover:bg-red-100 rounded text-red-700 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Drawer Content: Cart Items */}
        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4">
          {isLoading && items.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center text-[#434656] py-16 gap-3">
              <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
              <p className="text-xs text-[#737688]">Loading cart items...</p>
            </div>
          ) : items.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center text-[#434656] py-16">
              <ShoppingBag className="w-12 h-12 text-[#737688] mb-3 stroke-1" />
              <p className="font-semibold text-sm text-[#0b1c30]">Your cart is empty</p>
              <p className="text-xs text-[#737688] mt-1">
                Explore our marketplace to add items
              </p>
            </div>
          ) : (
            items.map((item) => (
              <div
                key={item.id}
                className="p-3.5 rounded-xl bg-[#eff4ff] flex gap-3.5 items-start group transition-colors hover:bg-[#dce9ff]/50"
              >
                {/* Product Thumbnail */}
                <div className="w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-white flex items-center justify-center p-1.5 border border-slate-100 relative">
                  <Image
                    src={item.imageUrl}
                    alt={item.title}
                    width={80}
                    height={80}
                    className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
                    unoptimized
                  />
                </div>

                {/* Details */}
                <div className="flex-1 min-w-0 flex flex-col justify-between h-full">
                  <div className="flex justify-between items-start gap-2">
                    <h3 className="font-semibold text-xs leading-snug text-[#0b1c30] line-clamp-2">
                      {item.title}
                    </h3>
                    <button
                      disabled={isLoading}
                      onClick={() => removeItem(item.id)}
                      className="text-[#737688] hover:text-[#a90900] transition-colors p-1 -mr-1 cursor-pointer disabled:opacity-40"
                      title="Remove item"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  <p className="text-[11px] text-[#434656] mt-1 line-clamp-1">{item.specs}</p>

                  <div className="flex items-center justify-between mt-3 pt-1">
                    <span className="font-bold text-sm text-[#0045cc] tracking-tight font-mono">
                      {new Intl.NumberFormat("vi-VN", {
                        style: "currency",
                        currency: "VND",
                      }).format(item.price)}
                    </span>

                    {/* Quantity Selector */}
                    <div className="flex items-center bg-white rounded-lg p-0.5 shadow-sm border border-slate-200">
                      <button
                        disabled={isLoading}
                        onClick={() => updateQuantity(item.id, -1)}
                        className="w-6 h-6 flex items-center justify-center rounded text-[#434656] hover:bg-[#eff4ff] transition-colors cursor-pointer disabled:opacity-40"
                        aria-label="Decrease quantity"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="w-7 text-center text-xs font-semibold text-[#0b1c30] font-mono">
                        {item.quantity}
                      </span>
                      <button
                        disabled={isLoading}
                        onClick={() => updateQuantity(item.id, 1)}
                        className="w-6 h-6 flex items-center justify-center rounded text-[#434656] hover:bg-[#eff4ff] transition-colors cursor-pointer disabled:opacity-40"
                        aria-label="Increase quantity"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Drawer Footer */}
        {items.length > 0 && (
          <div className="p-6 bg-[#eff4ff] space-y-4 border-t border-[#dce9ff]">
            <div className="flex items-center justify-between">
              <span className="text-sm text-[#434656]">
                Subtotal <span className="font-medium text-xs">({totalCount} items)</span>
              </span>
              <span className="font-bold text-lg text-[#0b1c30] tracking-tight font-mono">
                {formattedSubtotal}
              </span>
            </div>

            <div className="space-y-2 pt-1">
              <Link
                href="/checkout"
                onClick={closeCart}
                className="w-full flex items-center justify-center gap-2 py-3.5 px-4 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md transition-all active:scale-[0.99]"
              >
                <span>Proceed to Checkout</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        )}
      </aside>
    </div>
  );
}
