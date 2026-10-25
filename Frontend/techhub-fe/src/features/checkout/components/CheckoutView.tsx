"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  MapPin,
  CreditCard,
  Truck,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Loader2,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";

export function CheckoutView() {
  const { items, subtotal } = useCart();
  const [shippingMethod, setShippingMethod] = useState<"standard" | "express">("express");
  const [paymentMethod, setPaymentMethod] = useState<"vnpay" | "credit" | "cod">("vnpay");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPlaced, setIsPlaced] = useState(false);

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const shippingFee = shippingMethod === "express" ? 50000 : 25000;
  const discount = 500000; // Applied coupon default
  const grandTotal = Math.max(0, subtotal + shippingFee - discount);

  const handlePlaceOrder = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setIsPlaced(true);
    }, 1500);
  };

  if (isPlaced) {
    return (
      <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-16 flex items-center justify-center">
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
              <span className="text-[#737688]">Payment Status:</span>
              <span className="font-semibold text-emerald-600">Authorized</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Estimated Delivery:</span>
              <span className="font-semibold text-[#0b1c30]">Tomorrow, by 18:00</span>
            </div>
          </div>
          <div className="flex flex-col gap-2.5 w-full mt-2">
            <Link
              href="/"
              className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Back to Marketplace</span>
            </Link>
            <Link
              href="/cart"
              className="text-xs font-semibold text-[#434656] hover:text-[#0b1c30] py-1"
            >
              View Order Details
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
        <div className="flex items-center text-xs text-[#737688] gap-1.5">
          <Link href="/" className="hover:text-[#0045cc] transition-colors">
            Home
          </Link>
          <span>/</span>
          <Link href="/cart" className="hover:text-[#0045cc] transition-colors">
            Cart
          </Link>
          <span>/</span>
          <span className="text-[#0b1c30] font-medium">Checkout</span>
        </div>

        <div className="flex items-center justify-between">
          <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
            Checkout &amp; Payment
          </h1>
          <Link
            href="/cart"
            className="text-xs font-semibold text-[#0045cc] hover:underline inline-flex items-center gap-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Cart
          </Link>
        </div>

        <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left Form: Shipping, Method & Payment */}
          <div className="lg:col-span-8 flex flex-col gap-5">
            {/* 1. Shipping Address */}
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

            {/* 2. Shipping Service */}
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
                      Delivered within 24h • High-value insurance
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

            {/* 3. Payment Methods */}
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
                        VNPay QR / Banking App
                      </span>
                      <span className="text-[11px] text-[#737688]">
                        Instant authorization with 0% transaction surcharge
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

          {/* Right Summary Column */}
          <div className="lg:col-span-4 flex flex-col gap-4">
            <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
              <h2 className="text-base font-bold text-[#0b1c30]">
                Order Items ({items.length})
              </h2>

              <div className="divide-y divide-[#c3c5d9]/20 max-h-60 overflow-y-auto">
                {items.map((item) => (
                  <div key={item.id} className="py-3 flex items-center gap-3">
                    <div className="w-12 h-12 rounded-lg bg-[#eff4ff] relative overflow-hidden flex-shrink-0 border border-[#c3c5d9]/20">
                      <Image
                        src={item.imageUrl}
                        alt={item.title}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-bold text-[#0b1c30] truncate">
                        {item.title}
                      </p>
                      <p className="text-[11px] text-[#737688]">Qty: {item.quantity}</p>
                    </div>
                    <span className="text-xs font-mono font-bold text-[#0045cc] flex-shrink-0">
                      {formatPrice(item.price * item.quantity)}
                    </span>
                  </div>
                ))}
              </div>

              <div className="h-px bg-[#e5eeff] w-full"></div>

              {/* Price Details */}
              <div className="flex flex-col gap-2.5 text-xs text-[#434656]">
                <div className="flex items-center justify-between">
                  <span>Subtotal:</span>
                  <span className="font-mono font-bold text-[#0b1c30]">
                    {formatPrice(subtotal)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Shipping:</span>
                  <span className="font-mono font-bold text-[#0b1c30]">
                    +{formatPrice(shippingFee)}
                  </span>
                </div>
                <div className="flex items-center justify-between text-[#006b5b]">
                  <span>Voucher TECHMEGA500:</span>
                  <span className="font-mono font-bold">
                    -{formatPrice(discount)}
                  </span>
                </div>
              </div>

              <div className="h-px bg-[#e5eeff] w-full"></div>

              {/* Grand Total */}
              <div className="flex flex-col gap-1">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-[#434656]">
                    Total Payment:
                  </span>
                  <span className="text-[11px] text-[#006b5b] font-semibold">
                    (VAT Included)
                  </span>
                </div>
                <div className="font-heading text-2xl font-bold text-[#0045cc] tracking-tight">
                  {formatPrice(grandTotal)}
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting || items.length === 0}
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
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
