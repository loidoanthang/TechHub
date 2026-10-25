"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Store,
  Trash2,
  Plus,
  Minus,
  Tag,
  MapPin,
  Truck,
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  Loader2,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";
import { MOCK_AVAILABLE_VOUCHERS, type Voucher } from "@/mocks";

export function OnePageCartCheckoutView() {
  const { items, updateQuantity, removeItem, subtotal, totalCount } = useCart();

  // Selection state
  const [selectedIds, setSelectedIds] = useState<string[]>(
    items.map((item) => item.id)
  );

  // Voucher state
  const [voucherCode, setVoucherCode] = useState("");
  const [appliedVouchers, setAppliedVouchers] = useState<Voucher[]>([]);
  const [voucherError, setVoucherError] = useState("");

  // Shipping & Payment state
  const [shippingMethod, setShippingMethod] = useState<"express" | "standard">("express");
  const [paymentMethod, setPaymentMethod] = useState<"vnpay" | "credit" | "cod">("vnpay");

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPlaced, setIsPlaced] = useState(false);

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

  const shippingFee = selectedItems.length > 0 ? (shippingMethod === "express" ? 50000 : 25000) : 0;
  const discountAmount = appliedVouchers.reduce((acc, v) => acc + v.discountAmount, 0);
  const grandTotal = Math.max(0, selectedSubtotal + shippingFee - discountAmount);

  const handleApplyVoucher = (v: Voucher) => {
    if (appliedVouchers.some((item) => item.id === v.id)) {
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
    if (!voucherCode.trim()) return;
    const found = MOCK_AVAILABLE_VOUCHERS.find(
      (v) => v.code.toUpperCase() === voucherCode.trim().toUpperCase()
    );
    if (!found) {
      setVoucherError("Invalid voucher code.");
      setTimeout(() => setVoucherError(""), 3000);
      return;
    }
    handleApplyVoucher(found);
    setVoucherCode("");
  };

  const handlePlaceOrder = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedItems.length === 0) return;
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setIsPlaced(true);
    }, 1500);
  };

  // Group items by storeName
  const groupedStores = items.reduce<Record<string, typeof items>>((acc, item) => {
    const store = item.storeName || "TechHub Verified Store";
    if (!acc[store]) acc[store] = [];
    acc[store].push(item);
    return acc;
  }, {});

  if (isPlaced) {
    return (
      <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-12 flex items-center justify-center overflow-hidden">
        <div className="max-w-md w-full mx-4 bg-white rounded-2xl shadow-lg border border-[#c3c5d9]/30 p-8 text-center flex flex-col items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <h1 className="font-heading text-2xl font-bold text-[#0b1c30]">
            Order Confirmed!
          </h1>
          <p className="text-sm text-[#434656] leading-relaxed">
            Thank you for your purchase. We have received your order{" "}
            <span className="font-mono font-bold text-[#0b1c30]">#TH-94812</span> and
            notified the certified merchants.
          </p>
          <div className="w-full bg-[#eff4ff] rounded-xl p-4 text-xs flex flex-col gap-2 my-2 text-left">
            <div className="flex justify-between">
              <span className="text-[#737688]">Total Paid:</span>
              <span className="font-bold text-[#0045cc] font-mono">
                {formatPrice(grandTotal)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Payment Method:</span>
              <span className="font-semibold text-emerald-600 uppercase">
                {paymentMethod === "vnpay" ? "VNPay QR" : paymentMethod === "credit" ? "Credit Card" : "Cash on Delivery"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Estimated Delivery:</span>
              <span className="font-semibold text-[#0b1c30]">
                {shippingMethod === "express" ? "Tomorrow, by 18:00 (Express)" : "2-3 business days"}
              </span>
            </div>
          </div>
          <div className="flex flex-col gap-2.5 w-full mt-2">
            <Link
              href="/"
              className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Back to Marketplace</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 flex flex-col gap-6">
        {/* Navigation Breadcrumb */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center text-xs text-[#737688] gap-1.5">
            <Link href="/" className="hover:text-[#0045cc] transition-colors">
              Home
            </Link>
            <span>/</span>
            <span className="text-[#0b1c30] font-medium">Cart &amp; Checkout</span>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              Shopping Cart &amp; Checkout
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
          <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column (8 cols): Cart Items + Shipping Address + Shipping Service + Payment */}
            <div className="lg:col-span-8 flex flex-col gap-5">
              {/* Select All Header */}
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

              {/* Cart Items by Store */}
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
                    {/* Store Header */}
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
                          <span className="text-sm font-bold text-[#0b1c30]">
                            {storeName}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Store Items List */}
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

              {/* Delivery Address Section */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center justify-between border-b border-[#c3c5d9]/20 pb-3">
                  <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                    <MapPin className="w-4 h-4 text-[#0045cc]" />
                    <span>Delivery Address</span>
                  </div>
                  <button
                    type="button"
                    className="text-xs font-semibold text-[#0045cc] hover:underline cursor-pointer"
                  >
                    Change Address
                  </button>
                </div>

                <div className="flex flex-col gap-1 text-sm">
                  <div className="flex items-center gap-2 font-bold text-[#0b1c30]">
                    <span>Alex Morgan</span>
                    <span className="text-xs font-normal text-[#737688]">
                      (+84) 912 345 678
                    </span>
                    <span className="bg-[#eff4ff] text-[#0045cc] text-[10px] font-bold px-2 py-0.5 rounded uppercase">
                      Default
                    </span>
                  </div>
                  <p className="text-xs text-[#434656] leading-relaxed">
                    Tòa nhà TechHub Tower, 123 Đường Điện Biên Phủ, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh
                  </p>
                </div>
              </div>

              {/* Shipping Method Section */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30] border-b border-[#c3c5d9]/20 pb-3">
                  <Truck className="w-4 h-4 text-[#0045cc]" />
                  <span>Shipping Method</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <label
                    onClick={() => setShippingMethod("express")}
                    className={`border rounded-xl p-4 flex items-start gap-3 cursor-pointer transition-all ${
                      shippingMethod === "express"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <input
                      type="radio"
                      name="shipping"
                      checked={shippingMethod === "express"}
                      onChange={() => setShippingMethod("express")}
                      className="mt-1 accent-[#0045cc]"
                    />
                    <div className="flex flex-col gap-0.5">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          TechHub Express
                        </span>
                        <span className="text-[10px] font-bold bg-[#ce2917] text-white px-1.5 py-0.2 rounded">
                          FAST
                        </span>
                      </div>
                      <span className="text-[11px] text-[#737688]">
                        Delivered within 24h • High-value hardware seal protection
                      </span>
                      <span className="text-xs font-mono font-bold text-[#0045cc] mt-1">
                        {formatPrice(50000)}
                      </span>
                    </div>
                  </label>

                  <label
                    onClick={() => setShippingMethod("standard")}
                    className={`border rounded-xl p-4 flex items-start gap-3 cursor-pointer transition-all ${
                      shippingMethod === "standard"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <input
                      type="radio"
                      name="shipping"
                      checked={shippingMethod === "standard"}
                      onChange={() => setShippingMethod("standard")}
                      className="mt-1 accent-[#0045cc]"
                    />
                    <div className="flex flex-col gap-0.5">
                      <span className="text-xs font-bold text-[#0b1c30]">
                        Standard Ground
                      </span>
                      <span className="text-[11px] text-[#737688]">
                        Delivered in 2-3 business days
                      </span>
                      <span className="text-xs font-mono font-bold text-[#0045cc] mt-1">
                        {formatPrice(25000)}
                      </span>
                    </div>
                  </label>
                </div>
              </div>

              {/* Payment Methods Section */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30] border-b border-[#c3c5d9]/20 pb-3">
                  <CreditCard className="w-4 h-4 text-[#0045cc]" />
                  <span>Payment Method</span>
                </div>

                <div className="flex flex-col gap-2.5">
                  {/* VNPay */}
                  <label
                    onClick={() => setPaymentMethod("vnpay")}
                    className={`border rounded-xl p-4 flex items-center justify-between cursor-pointer transition-all ${
                      paymentMethod === "vnpay"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === "vnpay"}
                        onChange={() => setPaymentMethod("vnpay")}
                        className="accent-[#0045cc]"
                      />
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          VNPay QR / Mobile Banking
                        </span>
                        <span className="text-[11px] text-[#737688]">
                          Instant authorization with 0% surcharge
                        </span>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold text-[#0045cc] bg-[#eff4ff] px-2 py-0.5 rounded">
                      Popular
                    </span>
                  </label>

                  {/* Credit Card */}
                  <label
                    onClick={() => setPaymentMethod("credit")}
                    className={`border rounded-xl p-4 flex items-center justify-between cursor-pointer transition-all ${
                      paymentMethod === "credit"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === "credit"}
                        onChange={() => setPaymentMethod("credit")}
                        className="accent-[#0045cc]"
                      />
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          International Card (Visa / Mastercard / JCB)
                        </span>
                        <span className="text-[11px] text-[#737688]">
                          3D Secure 2.0 encrypted transaction
                        </span>
                      </div>
                    </div>
                  </label>

                  {/* COD */}
                  <label
                    onClick={() => setPaymentMethod("cod")}
                    className={`border rounded-xl p-4 flex items-center justify-between cursor-pointer transition-all ${
                      paymentMethod === "cod"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === "cod"}
                        onChange={() => setPaymentMethod("cod")}
                        className="accent-[#0045cc]"
                      />
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          Cash on Delivery (COD)
                        </span>
                        <span className="text-[11px] text-[#737688]">
                          Inspect component seals before making cash payment
                        </span>
                      </div>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            {/* Right Column (4 cols Sticky): Vouchers & Final Checkout Order Summary */}
            <div className="lg:col-span-4 flex flex-col gap-4 lg:sticky lg:top-28">
              {/* Vouchers & Promos */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-5 flex flex-col gap-3.5">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                  <Tag className="w-4 h-4 text-[#0045cc]" />
                  <span>TechHub Vouchers &amp; Promos</span>
                </div>

                <div className="flex items-center gap-2">
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
                    type="button"
                    onClick={handleManualRedeem}
                    className="bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold px-4 py-2 rounded-lg transition-colors shadow-sm flex-shrink-0 cursor-pointer"
                  >
                    Redeem
                  </button>
                </div>

                {voucherError && (
                  <p className="text-[11px] text-[#ba1a1a] font-medium">{voucherError}</p>
                )}

                {/* Available Coupons list */}
                <div className="flex flex-col gap-2.5">
                  {MOCK_AVAILABLE_VOUCHERS.map((v) => {
                    const isApplied = appliedVouchers.some((item) => item.id === v.id);
                    return (
                      <div
                        key={v.id}
                        className={`border rounded-xl p-3 flex items-center justify-between gap-3 transition-colors ${
                          isApplied
                            ? "border-[#0045cc] bg-[#eff4ff]"
                            : "border-[#c3c5d9]/30 bg-white"
                        }`}
                      >
                        <div className="flex flex-col min-w-0">
                          <div className="font-mono font-bold text-[#0b1c30] text-xs">
                            {v.code}
                          </div>
                          <div className="text-xs text-[#434656] line-clamp-1 leading-tight mt-0.5">
                            {v.description}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleApplyVoucher(v)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition-colors flex-shrink-0 cursor-pointer ${
                            isApplied
                              ? "bg-[#0045cc] text-white border-[#0045cc]"
                              : "bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0b1c30] border-[#c3c5d9]/30"
                          }`}
                        >
                          {isApplied ? "Applied" : "Apply"}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Order Summary & Final Place Order CTA */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <h2 className="text-base font-bold text-[#0b1c30]">
                  Payment Summary
                </h2>

                <div className="flex flex-col gap-2.5 text-xs text-[#434656]">
                  <div className="flex items-center justify-between">
                    <span>Subtotal ({selectedItems.length} items):</span>
                    <span className="font-mono font-bold text-[#0b1c30]">
                      {formatPrice(selectedSubtotal)}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Shipping Fee:</span>
                    <span className="font-mono font-bold text-[#0b1c30]">
                      {shippingFee > 0 ? `+${formatPrice(shippingFee)}` : "Free"}
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
                </div>

                <div className="h-px bg-[#e5eeff] w-full"></div>

                <div className="flex flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-[#434656]">
                      Total Payment:
                    </span>
                    <span className="text-[11px] text-[#006b5b] font-semibold">
                      (VAT included)
                    </span>
                  </div>
                  <div className="font-heading text-2xl sm:text-3xl font-bold text-[#0045cc] tracking-tight">
                    {formatPrice(grandTotal)}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting || selectedItems.length === 0}
                  className="w-full py-3.5 px-4 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-bold transition-all shadow-md flex items-center justify-center gap-2 mt-2 active:scale-[0.99] disabled:opacity-60 cursor-pointer"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      <span>Processing Order...</span>
                    </>
                  ) : (
                    <>
                      <span>Place Order ({formatPrice(grandTotal)})</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>

                <div className="flex items-center justify-center gap-1.5 text-[11px] text-[#737688] pt-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>30-Day TechHub Buyer Protection Guarantee</span>
                </div>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
