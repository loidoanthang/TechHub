"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Store,
  Trash2,
  Plus,
  Minus,
  Tag,
  ArrowRight,
  ShieldCheck,
  Check,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";
import { resolveSellerSlug } from "@/features/shops";
interface Voucher {
  id: string;
  code: string;
  title: string;
  description: string;
  discountAmount: number;
  minOrderValue: number;
}

export function CartView() {
  const { items, updateQuantity, removeItem, subtotal, totalCount } = useCart();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [hasInitializedSelection, setHasInitializedSelection] = useState(false);

  useEffect(() => {
    if (!hasInitializedSelection && items.length > 0) {
      setSelectedIds(items.map((item) => item.id));
      setHasInitializedSelection(true);
    }
  }, [items, hasInitializedSelection]);
  const [voucherCode, setVoucherCode] = useState("");
  const [appliedVouchers, setAppliedVouchers] = useState<Voucher[]>([]);
  const [voucherError, setVoucherError] = useState("");

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const isAllSelected = items.length > 0 && selectedIds.length === items.length;

  const toggleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(items.map((i) => i.id));
    }
  };

  const toggleSelectItem = (id: string) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const selectedItems = items.filter((item) => selectedIds.includes(item.id));
  const selectedSubtotal = selectedItems.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  const discountAmount = appliedVouchers.reduce(
    (acc, v) => acc + v.discountAmount,
    0
  );
  const finalTotal = Math.max(0, selectedSubtotal - discountAmount);

  const handleApplyVoucher = (v: Voucher) => {
    if (appliedVouchers.some((item) => item.id === v.id)) {
      // Remove
      setAppliedVouchers(appliedVouchers.filter((item) => item.id !== v.id));
      return;
    }
    if (selectedSubtotal < v.minOrderValue) {
      setVoucherError(`Minimum order value ${formatPrice(v.minOrderValue)} required.`);
      setTimeout(() => setVoucherError(""), 3000);
      return;
    }
    setAppliedVouchers([...appliedVouchers, v]);
    setVoucherError("");
  };

  const handleManualRedeem = (e: React.FormEvent) => {
    e.preventDefault();
    const code = voucherCode.trim().toUpperCase();
    if (!code) return;
    if (code === "TECH50K" || code === "TECHHUB50") {
      const v: Voucher = {
        id: "voucher-tech50k",
        code,
        title: "Ưu đãi công nghệ TechHub",
        description: "Giảm 50.000₫ cho đơn hàng",
        discountAmount: 50000,
        minOrderValue: 200000,
      };
      handleApplyVoucher(v);
      setVoucherCode("");
    } else {
      setVoucherError("Mã voucher không hợp lệ hoặc đã hết hạn.");
      setTimeout(() => setVoucherError(""), 3000);
    }
  };

  // Group items by storeName
  const groupedStores = items.reduce<Record<string, typeof items>>((acc, item) => {
    const store = item.storeName || "TechHub Verified Store";
    if (!acc[store]) acc[store] = [];
    acc[store].push(item);
    return acc;
  }, {});

  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 flex flex-col gap-6">
        {/* Breadcrumb & Title */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center text-xs text-[#737688] gap-1.5">
            <Link href="/" className="hover:text-[#0045cc] transition-colors">
              Home
            </Link>
            <span>/</span>
            <span className="text-[#0b1c30] font-medium">Your Shopping Cart</span>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              Your Cart
            </h1>
            <Link
              href="/search"
              className="text-xs sm:text-sm font-bold text-[#0045cc] hover:underline inline-flex items-center gap-1"
            >
              Continue Shopping <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {items.length === 0 ? (
          <div className="bg-white rounded-2xl border border-[#c3c5d9]/30 shadow-sm p-12 flex flex-col items-center justify-center text-center">
            <div className="w-16 h-16 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center mb-4">
              <Store className="w-8 h-8" />
            </div>
            <h2 className="font-heading text-xl font-bold text-[#0b1c30] mb-2">
              Your cart is empty
            </h2>
            <p className="text-sm text-[#434656] max-w-sm mb-6">
              Looks like you haven&apos;t added any tech items yet. Explore top-rated gear and deals!
            </p>
            <Link
              href="/search"
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors shadow-sm"
            >
              Explore Products
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column: Cart Items List */}
            <div className="lg:col-span-8 flex flex-col gap-4">
              {/* Select All Bar */}
              <div className="bg-white rounded-xl p-4 px-6 border border-[#c3c5d9]/30 shadow-sm flex items-center justify-between">
                <label className="flex items-center gap-3 cursor-pointer select-none text-xs sm:text-sm font-semibold text-[#0b1c30]">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    onChange={toggleSelectAll}
                    className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer"
                  />
                  <span>Select All ({totalCount} items)</span>
                </label>
                <span className="text-xs text-[#737688]">
                  Selected:{" "}
                  <strong className="text-[#0045cc] font-bold">
                    {selectedItems.length}
                  </strong>
                </span>
              </div>

              {/* Stores and Products */}
              {Object.entries(groupedStores).map(([storeName, storeItems]) => {
                const storeAllSelected = storeItems.every((i) =>
                  selectedIds.includes(i.id)
                );

                const toggleStore = () => {
                  if (storeAllSelected) {
                    setSelectedIds(
                      selectedIds.filter(
                        (id) => !storeItems.some((si) => si.id === id)
                      )
                    );
                  } else {
                    const newIds = [
                      ...selectedIds,
                      ...storeItems
                        .map((si) => si.id)
                        .filter((id) => !selectedIds.includes(id)),
                    ];
                    setSelectedIds(newIds);
                  }
                };

                return (
                  <div
                    key={storeName}
                    className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm overflow-hidden flex flex-col"
                  >
                    {/* Store Title */}
                    <div className="px-6 py-3.5 border-b border-[#c3c5d9]/20 flex items-center justify-between bg-white">
                      <div className="flex items-center gap-3">
                        <input
                          type="checkbox"
                          checked={storeAllSelected}
                          onChange={toggleStore}
                          className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer"
                        />
                        <div className="flex items-center gap-2">
                          <Store className="w-4 h-4 text-[#0045cc]" />
                          <Link
                            href={`/shops/${resolveSellerSlug(storeName)}`}
                            className="text-sm font-bold text-[#0b1c30] hover:text-[#0045cc] transition-colors hover:underline"
                          >
                            {storeName}
                          </Link>
                        </div>
                      </div>
                    </div>

                    {/* Store Item Rows */}
                    <div className="divide-y divide-[#c3c5d9]/20">
                      {storeItems.map((item) => (
                        <div
                          key={item.id}
                          className="p-6 flex flex-col sm:flex-row items-start sm:items-center gap-4"
                        >
                          <div className="flex items-center gap-3">
                            <input
                              type="checkbox"
                              checked={selectedIds.includes(item.id)}
                              onChange={() => toggleSelectItem(item.id)}
                              className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer flex-shrink-0"
                            />
                            <div className="w-20 h-20 rounded-xl overflow-hidden bg-[#eff4ff] flex-shrink-0 border border-[#c3c5d9]/30 relative">
                              <Image
                                src={item.imageUrl}
                                alt={item.title}
                                fill
                                className="object-cover"
                                unoptimized
                              />
                            </div>
                          </div>

                          <div className="flex-1 min-w-0 flex flex-col gap-1">
                            <h3 className="text-sm font-bold text-[#0b1c30] line-clamp-2 leading-snug hover:text-[#0045cc] transition-colors cursor-pointer">
                              {item.title}
                            </h3>
                            <div className="text-xs text-[#737688]">
                              Variant:{" "}
                              <span className="text-[#434656] font-medium">
                                {item.specs}
                              </span>
                            </div>
                            <div className="mt-1.5 text-[#0045cc] font-bold text-base">
                              {formatPrice(item.price)}
                            </div>
                          </div>

                          <div className="flex items-center justify-between sm:justify-end gap-4 w-full sm:w-auto mt-2 sm:mt-0 flex-shrink-0">
                            {/* Quantity Controls */}
                            <div className="flex items-center border border-[#c3c5d9]/40 rounded-lg overflow-hidden bg-white">
                              <button
                                type="button"
                                onClick={() => updateQuantity(item.id, -1)}
                                className="w-8 h-8 flex items-center justify-center text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer"
                              >
                                <Minus className="w-3.5 h-3.5" />
                              </button>
                              <span className="w-8 text-center text-xs font-bold text-[#0b1c30]">
                                {item.quantity}
                              </span>
                              <button
                                type="button"
                                onClick={() => updateQuantity(item.id, 1)}
                                className="w-8 h-8 flex items-center justify-center text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer"
                              >
                                <Plus className="w-3.5 h-3.5" />
                              </button>
                            </div>

                            {/* Delete Item */}
                            <button
                              type="button"
                              onClick={() => removeItem(item.id)}
                              className="text-[#737688] hover:text-[#ba1a1a] transition-colors p-1.5 rounded-lg hover:bg-[#eff4ff] cursor-pointer"
                              title="Delete item"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Right Column: Vouchers & Order Summary */}
            <div className="lg:col-span-4 flex flex-col gap-4">
              {/* Vouchers & Promos */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-5 flex flex-col gap-3.5">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                  <Tag className="w-4 h-4 text-[#0045cc]" />
                  <span>TechHub Vouchers &amp; Promos</span>
                </div>

                <form onSubmit={handleManualRedeem} className="flex items-center gap-2">
                  <div className="relative flex-1 flex items-center rounded-lg border border-[#c3c5d9]/40 bg-[#eff4ff] px-3 py-1.5 focus-within:ring-2 focus-within:ring-[#0045cc]/20 focus-within:border-[#0045cc] transition-all">
                    <input
                      type="text"
                      value={voucherCode}
                      onChange={(e) => setVoucherCode(e.target.value)}
                      placeholder="Enter voucher code"
                      className="w-full bg-transparent font-mono text-xs uppercase tracking-wider text-[#0b1c30] placeholder:text-[#737688] placeholder:normal-case placeholder:tracking-normal focus:outline-none"
                    />
                  </div>
                  <button
                    type="submit"
                    className="bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold px-4 py-2 rounded-lg transition-colors shadow-sm flex-shrink-0 cursor-pointer"
                  >
                    Redeem
                  </button>
                </form>

                {voucherError && (
                  <p className="text-[11px] text-[#ba1a1a] font-medium">{voucherError}</p>
                )}

                {/* Applied Coupons list */}
                {appliedVouchers.length > 0 && (
                  <div className="flex flex-col gap-2">
                    {appliedVouchers.map((v) => (
                      <div
                        key={v.id}
                        className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between"
                      >
                        <div>
                          <span className="font-mono font-bold">{v.code}</span>: Giảm{" "}
                          {formatPrice(v.discountAmount)}
                        </div>
                        <button
                          type="button"
                          onClick={() => handleApplyVoucher(v)}
                          className="text-xs text-red-600 hover:underline cursor-pointer"
                        >
                          Bỏ chọn
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Order Summary */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <h2 className="text-base font-bold text-[#0b1c30]">
                  Order Summary
                </h2>

                <div className="flex flex-col gap-2.5 text-xs text-[#434656]">
                  <div className="flex items-center justify-between">
                    <span>Subtotal ({selectedItems.length} items):</span>
                    <span className="font-mono font-bold text-[#0b1c30]">
                      {formatPrice(selectedSubtotal)}
                    </span>
                  </div>
                  {discountAmount > 0 && (
                    <div className="flex items-center justify-between text-[#006b5b]">
                      <span>Voucher Discount:</span>
                      <span className="font-mono font-bold">
                        -{formatPrice(discountAmount)}
                      </span>
                    </div>
                  )}
                  <div className="flex items-center justify-between">
                    <span>Estimated Shipping:</span>
                    <span className="text-[#737688]">Calculated at checkout</span>
                  </div>
                </div>

                <div className="h-px bg-[#e5eeff] w-full"></div>

                <div className="flex flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-[#434656]">
                      Total Amount:
                    </span>
                    <span className="text-[11px] text-[#006b5b] font-semibold">
                      (VAT included)
                    </span>
                  </div>
                  <div className="font-heading text-2xl font-bold text-[#0045cc] tracking-tight">
                    {formatPrice(finalTotal)}
                  </div>
                </div>

                <Link
                  href="/checkout"
                  className={`w-full py-3.5 px-4 rounded-xl text-white text-sm font-bold transition-all shadow-md flex items-center justify-center gap-2 mt-2 ${
                    selectedItems.length === 0
                      ? "bg-neutral-300 pointer-events-none opacity-60"
                      : "bg-[#0045cc] hover:bg-[#0d5bff] active:scale-[0.99]"
                  }`}
                >
                  <span>Proceed to Checkout ({selectedItems.length})</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
