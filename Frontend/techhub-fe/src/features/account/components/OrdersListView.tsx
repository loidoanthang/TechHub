"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { ProductThumbnail } from "@/components/shared";
import {
  Search,
  Store,
  Eye,
  CheckCircle2,
  XCircle,
  RotateCcw,
  CreditCard,
  Truck,
  Check,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { useBuyerOrdersQuery, useCompleteOrderMutation } from "@/features/orders/hooks/useOrderQueries";
import type { OrderStatus } from "@/types/enums";
import type { BuyerOrderResponse, BuyerOrderItemResponse } from "@/types/order";
import { CancelOrderModal } from "./CancelOrderModal";
import { ReturnRefundModal } from "./ReturnRefundModal";

const TABS: { label: string; status: OrderStatus | "ALL" }[] = [
  { label: "All Orders", status: "ALL" },
  { label: "To Pay", status: "PENDING_PAYMENT" },
  { label: "Processing", status: "PROCESSING" },
  { label: "Shipping", status: "SHIPPED" },
  { label: "Delivered", status: "DELIVERED" },
  { label: "Completed", status: "COMPLETED" },
  { label: "Cancelled", status: "CANCELLED" },
];

export function OrdersListView() {
  const [activeTab, setActiveTab] = useState<OrderStatus | "ALL">("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const { data: pageData, isLoading, error, refetch } = useBuyerOrdersQuery({
    status: activeTab,
    keyword: debouncedKeyword || undefined,
  });

  const completeOrderMutation = useCompleteOrderMutation();

  // Modals state
  const [cancelTargetOrder, setCancelTargetOrder] = useState<BuyerOrderResponse | null>(null);
  const [returnTargetOrder, setReturnTargetOrder] = useState<BuyerOrderResponse | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const formatPrice = (val?: number | string | null) => {
    if (val === undefined || val === null) return "0₫";
    const num = typeof val === "string" ? parseFloat(val) : val;
    if (isNaN(num)) return "0₫";
    return new Intl.NumberFormat("vi-VN").format(num) + "₫";
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setDebouncedKeyword(searchQuery.trim());
  };

  const handleConfirmReceipt = async (orderId: string) => {
    try {
      await completeOrderMutation.mutateAsync(orderId);
      showToast("Receipt confirmed! Escrow funds released to merchant.");
      refetch();
    } catch {
      showToast("Failed to confirm receipt. Please try again.");
    }
  };

  const orders = pageData?.content || [];

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Top Controls: Search Bar */}
      <form onSubmit={handleSearchSubmit} className="relative w-full">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search across all your orders by Order Number, Shop Name, or Item Name..."
          className="w-full h-12 pl-11 pr-24 rounded-2xl bg-white border border-[#c3c5d9]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all shadow-sm"
        />
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-[#737688]" />
        <button
          type="submit"
          className="absolute right-2 top-1/2 -translate-y-1/2 px-4 py-1.5 rounded-xl bg-[#0045cc] text-white text-xs font-semibold hover:bg-[#0d5bff] transition-colors"
        >
          Search
        </button>
      </form>

      {/* Order Status Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar border-b border-[#eff4ff]">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.status;
          return (
            <button
              key={tab.label}
              onClick={() => setActiveTab(tab.status)}
              className={`px-4 py-2.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                isActive
                  ? "bg-[#0045cc] text-white shadow-sm"
                  : "bg-white text-[#434656] hover:bg-[#eff4ff] hover:text-[#0b1c30] border border-[#c3c5d9]/20"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Failed to fetch orders from server.</span>
        </div>
      )}

      {/* Orders List Container */}
      <div className="flex flex-col gap-5">
        {isLoading ? (
          <div className="w-full h-64 flex items-center justify-center bg-white rounded-2xl border border-[#c3c5d9]/25">
            <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
          </div>
        ) : orders.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-[#c3c5d9]/25 flex flex-col items-center justify-center gap-3">
            <div className="w-16 h-16 rounded-full bg-[#eff4ff] flex items-center justify-center text-[#737688]">
              <Search className="w-7 h-7" />
            </div>
            <h3 className="font-heading font-bold text-base text-[#0b1c30]">
              No Orders Found
            </h3>
            <p className="text-xs text-[#737688] max-w-sm">
              We couldn&apos;t find any orders matching your criteria in this tab.
            </p>
            <Link
              href="/products"
              className="mt-2 px-5 py-2.5 rounded-xl bg-[#0045cc] text-white text-xs font-bold hover:bg-[#0d5bff] transition-colors"
            >
              Explore Components
            </Link>
          </div>
        ) : (
          orders.map((order: BuyerOrderResponse) => {
            const isDelivered = order.status === "DELIVERED";
            const isCompleted = order.status === "COMPLETED";
            const isPendingPayment = order.status === "PENDING_PAYMENT";
            const canCancelOrRequest =
              order.status === "PENDING_PAYMENT" ||
              order.status === "CONFIRMED" ||
              order.status === "PROCESSING";

            return (
              <div
                key={order.orderId}
                className="bg-white rounded-2xl border border-[#c3c5d9]/25 shadow-sm overflow-hidden flex flex-col transition-all hover:border-[#0045cc]/30"
              >
                {/* Order Header */}
                <div className="bg-[#eff4ff]/60 px-5 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 border-b border-[#eff4ff]">
                  <div className="flex items-center gap-3">
                    <Link
                      href={`/shops/${order.shopSlug || order.shopName}`}
                      className="flex items-center gap-1.5 text-xs font-bold text-[#0b1c30] hover:text-[#0045cc] transition-colors cursor-pointer group"
                    >
                      <Store className="w-4 h-4 text-[#0045cc]" />
                      <span className="group-hover:underline">{order.shopName}</span>
                    </Link>
                    <span className="text-[#c3c5d9]">•</span>
                    <span className="text-xs text-[#737688] font-mono">
                      #{order.orderNumber}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-[11px] text-[#737688]">
                      {new Date(order.createdAt).toLocaleDateString("vi-VN")}
                    </span>
                    <span
                      className={`text-[11px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider ${
                        order.status === "COMPLETED"
                          ? "bg-[#006b5b]/10 text-[#006b5b]"
                          : order.status === "SHIPPED" || order.status === "DELIVERED"
                          ? "bg-[#0045cc]/10 text-[#0045cc]"
                          : order.status === "CANCELLED"
                          ? "bg-[#ba1a1a]/10 text-[#ba1a1a]"
                          : "bg-[#f59e0b]/10 text-[#b45309]"
                      }`}
                    >
                      {order.statusDescription || order.status}
                    </span>
                  </div>
                </div>

                {/* Items List */}
                <div className="divide-y divide-[#eff4ff]">
                  {order.items.map((item: BuyerOrderItemResponse) => {
                    const unitPrice =
                      item.discountedUnitPrice ??
                      item.originalPrice ??
                      item.price ??
                      0;
                    const subtotal =
                      item.finalSubtotal ??
                      item.subtotal ??
                      unitPrice * item.quantity;
                    const hasDiscount =
                      item.originalPrice !== undefined &&
                      item.discountedUnitPrice !== undefined &&
                      item.discountedUnitPrice < item.originalPrice;

                    return (
                      <div
                        key={item.orderItemId}
                        className="p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
                      >
                        <div className="flex items-start gap-4 flex-1 min-w-0">
                          <ProductThumbnail
                            src={item.imageUrl}
                            alt={item.productName}
                            sizeClassName="w-20 h-20"
                          />

                          <div className="flex flex-col min-w-0">
                            <h3 className="text-sm font-bold text-[#0b1c30] hover:text-[#0045cc] transition-colors line-clamp-2 leading-snug">
                              {item.productName}
                            </h3>
                            <div className="flex items-center gap-2 mt-1 text-xs text-[#737688]">
                              <span className="bg-[#eff4ff] px-2 py-0.5 rounded text-[11px] font-medium text-[#434656]">
                                {item.variantName}
                              </span>
                              <span>
                                Qty: <strong className="text-[#0b1c30]">x{item.quantity}</strong>
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Item Pricing */}
                        <div className="flex flex-col items-end shrink-0">
                          {hasDiscount ? (
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs text-[#737688] line-through">
                                {formatPrice(item.originalPrice)}
                              </span>
                              <span className="text-sm font-bold text-[#0b1c30]">
                                {formatPrice(unitPrice)}
                              </span>
                            </div>
                          ) : (
                            <span className="text-sm font-bold text-[#0b1c30]">
                              {formatPrice(unitPrice)}
                            </span>
                          )}
                          <span className="text-[11px] text-[#737688]">
                            Subtotal: {formatPrice(subtotal)}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Order Footer & Actions */}
                <div className="bg-[#eff4ff]/20 p-5 border-t border-[#eff4ff] flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                  <div className="flex items-center gap-3 text-xs text-[#737688]">
                    <span>Payment: <strong className="text-[#0b1c30] font-semibold">{order.paymentMethod}</strong></span>
                    <span>•</span>
                    <span>Status: <strong className="text-[#0b1c30] font-semibold">{order.paymentStatus}</strong></span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2.5 w-full md:w-auto justify-end">
                    <div className="flex items-baseline gap-1.5 mr-2">
                      <span className="text-xs text-[#737688]">Order Total:</span>
                      <span className="text-base font-bold text-[#0045cc]">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>

                    <Link
                      href={`/account/orders/${order.orderId}`}
                      className="px-4 py-2 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-[#0b1c30] text-xs font-semibold transition-colors flex items-center gap-1.5"
                    >
                      <Eye className="w-3.5 h-3.5 text-[#737688]" />
                      <span>Details</span>
                    </Link>

                    {/* Pending payment: pay now */}
                    {isPendingPayment && (
                      <Link
                        href={`/checkout?orderId=${order.orderId}`}
                        className="px-4 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-all shadow-sm flex items-center gap-1.5"
                      >
                        <CreditCard className="w-3.5 h-3.5" />
                        <span>Pay Now</span>
                      </Link>
                    )}

                    {/* Delivered: Confirm Receipt */}
                    {isDelivered && (
                      <>
                        <button
                          type="button"
                          onClick={() => handleConfirmReceipt(order.orderId)}
                          disabled={completeOrderMutation.isPending}
                          className="px-4 py-2 rounded-xl bg-[#006b5b] hover:bg-[#008b77] text-white text-xs font-semibold transition-all shadow-sm flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Confirm Receipt</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setReturnTargetOrder(order)}
                          className="px-4 py-2 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold transition-colors flex items-center gap-1.5 cursor-pointer"
                        >
                          <RotateCcw className="w-3.5 h-3.5" />
                          <span>Return / Refund</span>
                        </button>
                      </>
                    )}

                    {/* Cancel or Request Cancellation */}
                    {canCancelOrRequest && (
                      <button
                        type="button"
                        onClick={() => setCancelTargetOrder(order)}
                        className="px-4 py-2 rounded-xl border border-[#ba1a1a]/30 text-[#ba1a1a] hover:bg-[#ba1a1a]/10 text-xs font-semibold transition-colors cursor-pointer"
                      >
                        <XCircle className="w-3.5 h-3.5 inline mr-1" />
                        {isPendingPayment ? "Cancel Order" : "Request Cancel"}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Cancel Order Modal */}
      {cancelTargetOrder && (
        <CancelOrderModal
          orderId={cancelTargetOrder.orderId}
          orderNumber={cancelTargetOrder.orderNumber}
          shopName={cancelTargetOrder.shopName}
          status={cancelTargetOrder.status}
          isOpen={Boolean(cancelTargetOrder)}
          onClose={() => setCancelTargetOrder(null)}
          onSuccess={(isDirect) => {
            showToast(
              isDirect
                ? "Order cancelled immediately."
                : "Cancellation request submitted for merchant review."
            );
            refetch();
          }}
        />
      )}

      {/* Return & Refund Modal */}
      {returnTargetOrder && (
        <ReturnRefundModal
          orderId={returnTargetOrder.orderId}
          orderNumber={returnTargetOrder.orderNumber}
          shopName={returnTargetOrder.shopName}
          totalAmount={returnTargetOrder.totalAmount}
          isOpen={Boolean(returnTargetOrder)}
          onClose={() => setReturnTargetOrder(null)}
          onSuccess={() => {
            showToast("Return & Refund request submitted successfully.");
            refetch();
          }}
        />
      )}

      {/* Floating Toast Notification */}
      <div
        className={`fixed bottom-8 right-8 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 transform transition-all duration-300 ${
          toastMessage
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-12 opacity-0 pointer-events-none"
        }`}
      >
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <span className="text-xs font-semibold">{toastMessage}</span>
      </div>
    </main>
  );
}
