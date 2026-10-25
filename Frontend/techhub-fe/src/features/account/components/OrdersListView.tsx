"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Search,
  Store,
  MessageSquare,
  Eye,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Star,
  Repeat,
  CreditCard,
  Truck,
  Check,
} from "lucide-react";
import { MOCK_ORDERS, type OrderStatus, type OrderDTO, type OrderItemDTO } from "@/mocks";
import { CancelOrderModal } from "./CancelOrderModal";
import { ReturnRefundModal } from "./ReturnRefundModal";
import { ReviewProductModal } from "./ReviewProductModal";

const TABS: { label: string; status: OrderStatus | "ALL" }[] = [
  { label: "All Orders", status: "ALL" },
  { label: "To Pay", status: "PENDING_PAYMENT" },
  { label: "To Ship", status: "PROCESSING" },
  { label: "To Receive", status: "SHIPPED" },
  { label: "Delivered", status: "DELIVERED" },
  { label: "Completed", status: "COMPLETED" },
  { label: "Cancelled / Refund", status: "CANCELLED" },
];

export function OrdersListView() {
  const [orders, setOrders] = useState<OrderDTO[]>(MOCK_ORDERS);
  const [activeTab, setActiveTab] = useState<OrderStatus | "ALL">("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modal states
  const [cancelTargetOrder, setCancelTargetOrder] = useState<OrderDTO | null>(null);
  const [returnTargetOrder, setReturnTargetOrder] = useState<OrderDTO | null>(null);
  const [reviewTargetItem, setReviewTargetItem] = useState<{
    item: OrderItemDTO;
    orderId: string;
  } | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const handleConfirmReceipt = (orderId: string) => {
    setOrders((prev) =>
      prev.map((o) =>
        o.id === orderId
          ? {
              ...o,
              status: "COMPLETED",
              statusLabel: "Completed",
            }
          : o
      )
    );
    showToast("Receipt confirmed! Escrow funds released to merchant.");
  };

  const handleCancelSuccess = (orderId: string, isDirect: boolean, reason: string) => {
    if (isDirect) {
      setOrders((prev) =>
        prev.map((o) =>
          o.id === orderId
            ? {
                ...o,
                status: "CANCELLED",
                statusLabel: "Cancelled",
              }
            : o
        )
      );
      showToast("Order cancelled immediately. Inventory released.");
    } else {
      setOrders((prev) =>
        prev.map((o) =>
          o.id === orderId
            ? {
                ...o,
                cancellationRequest: {
                  id: "req-" + Date.now(),
                  orderId,
                  reason,
                  status: "PENDING",
                  createdAt: "Just now",
                },
              }
            : o
        )
      );
      showToast("Cancellation request submitted for merchant review.");
    }
  };

  const handleReturnSuccess = (
    orderId: string,
    reason: string,
    description: string,
    mediaCount: number
  ) => {
    setOrders((prev) =>
      prev.map((o) =>
        o.id === orderId
          ? {
              ...o,
              returnRefundRequest: {
                id: "rr-" + Date.now(),
                orderId,
                reason,
                description,
                status: "PENDING",
                mediaUrls: [],
                createdAt: "Just now",
              },
            }
          : o
        )
    );
    showToast("Return & Refund request submitted with " + mediaCount + " attachments.");
  };

  const handleReviewSuccess = (
    itemId: string,
    rating: number,
    comment: string,
    mediaCount: number
  ) => {
    setOrders((prev) =>
      prev.map((o) => ({
        ...o,
        items: o.items.map((item) =>
          item.id === itemId ? { ...item, hasReviewed: true } : item
        ),
      }))
    );
    showToast(`Review submitted (${rating} stars)! Thank you for your feedback.`);
  };

  const filteredOrders = orders.filter((order) => {
    const matchesTab =
      activeTab === "ALL" ||
      order.status === activeTab ||
      (activeTab === "CANCELLED" && (order.status === "CANCELLED" || order.status === "RETURNED"));

    const query = searchQuery.toLowerCase().trim();
    const matchesQuery =
      !query ||
      order.orderNumber.toLowerCase().includes(query) ||
      order.shopName.toLowerCase().includes(query) ||
      order.items.some((i) => i.productName.toLowerCase().includes(query));

    return matchesTab && matchesQuery;
  });

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 border border-[#c3c5d9]/30 text-xs animate-in slide-in-from-bottom-5 duration-200">
          <CheckCircle2 className="w-4 h-4 text-[#4edea3] shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Header: Title & Quick Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
            My Purchases &amp; Orders
          </h1>
          <p className="text-xs text-[#737688]">
            Track hardware shipments, order milestones, and certified receipts.
          </p>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-[#737688]" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by Order ID, Shop or item..."
            className="w-full pl-10 pr-4 py-2 rounded-xl text-xs bg-white border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] transition-all text-[#0b1c30] placeholder:text-[#737688] shadow-2xs"
          />
        </div>
      </div>

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

      {/* Orders List Container */}
      <div className="flex flex-col gap-5">
        {filteredOrders.length === 0 ? (
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
          filteredOrders.map((order) => {
            const isDelivered = order.status === "DELIVERED";
            const isCompleted = order.status === "COMPLETED";
            const isPendingPayment = order.status === "PENDING_PAYMENT";
            const isProcessingOrConfirmed =
              order.status === "PROCESSING" || order.status === "CONFIRMED";

            return (
              <div
                key={order.id}
                className="bg-white rounded-2xl border border-[#c3c5d9]/25 shadow-sm overflow-hidden flex flex-col transition-all hover:border-[#0045cc]/30"
              >
                {/* Order Header */}
                <div className="bg-[#eff4ff]/60 px-5 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 border-b border-[#eff4ff]">
                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1.5 text-xs font-bold text-[#0b1c30]">
                      <Store className="w-4 h-4 text-[#0045cc]" />
                      <span>{order.shopName}</span>
                    </span>
                    <span className="text-[#c3c5d9]">•</span>
                    <span className="text-xs text-[#737688] font-mono">
                      #{order.orderNumber}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-[11px] text-[#737688]">
                      {order.createdAt}
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
                      {order.statusLabel}
                    </span>
                  </div>
                </div>

                {/* Cancellation / Return Request Banner if active */}
                {order.cancellationRequest && (
                  <div className="px-5 py-2.5 bg-[#fef3c7] border-b border-[#fde68a] text-xs text-[#92400e] flex items-center justify-between">
                    <span>
                      ⚠️ Cancellation request pending merchant approval (Reason:{" "}
                      <em>{order.cancellationRequest.reason}</em>)
                    </span>
                    <span className="font-semibold text-[11px] uppercase">Pending</span>
                  </div>
                )}
                {order.returnRefundRequest && (
                  <div className="px-5 py-2.5 bg-[#eff4ff] border-b border-[#c3c5d9]/40 text-xs text-[#0045cc] flex items-center justify-between">
                    <span>
                      🔄 Return &amp; Refund request submitted (Reason:{" "}
                      <em>{order.returnRefundRequest.reason}</em>)
                    </span>
                    <span className="font-semibold text-[11px] uppercase">
                      Under Review
                    </span>
                  </div>
                )}

                {/* Items List */}
                <div className="divide-y divide-[#eff4ff]">
                  {order.items.map((item) => (
                    <div
                      key={item.id}
                      className="p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
                    >
                      <div className="flex items-start gap-4 flex-1 min-w-0">
                        <div className="w-20 h-20 rounded-xl overflow-hidden bg-[#eff4ff] shrink-0 border border-[#c3c5d9]/25 relative">
                          <Image
                            src={item.imageUrl}
                            alt={item.productName}
                            fill
                            className="object-cover"
                            unoptimized
                          />
                        </div>

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

                          {/* Review product per OrderItem if Completed */}
                          {isCompleted && (
                            <div className="mt-2.5">
                              {item.hasReviewed ? (
                                <span className="inline-flex items-center gap-1 text-[11px] text-[#006b5b] font-semibold">
                                  <Check className="w-3.5 h-3.5" /> Reviewed
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() =>
                                    setReviewTargetItem({
                                      item,
                                      orderId: order.id,
                                    })
                                  }
                                  className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-semibold transition-colors cursor-pointer"
                                >
                                  <Star className="w-3 h-3" />
                                  <span>Review Product</span>
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Item Price */}
                      <div className="flex md:flex-col items-baseline md:items-end justify-between w-full md:w-auto shrink-0">
                        <span className="font-heading text-base font-bold text-[#0b1c30]">
                          {formatPrice(item.price)}
                        </span>
                        {item.originalPrice && (
                          <span className="text-[11px] text-[#737688] line-through">
                            {formatPrice(item.originalPrice)}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Order Footer Total */}
                <div className="bg-[#eff4ff]/30 px-5 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs border-t border-[#eff4ff]">
                  <div className="flex items-center gap-2 text-[#737688]">
                    <span>
                      Shipping Carrier: <strong>{order.shippingCarrier}</strong>
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-[#434656]">Order Total:</span>
                    <span className="font-heading text-lg font-bold text-[#0045cc]">
                      {formatPrice(order.totalAmount)}
                    </span>
                  </div>
                </div>

                {/* Operational Action Buttons */}
                <div className="px-5 py-3.5 flex flex-wrap items-center justify-end gap-2.5 bg-white border-t border-[#eff4ff]">
                  {/* View Details always available */}
                  <Link
                    href={`/account/orders/${order.id}`}
                    className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#434656] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors flex items-center gap-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>Order Details</span>
                  </Link>

                  {/* 1. Direct Cancel if PENDING_PAYMENT */}
                  {isPendingPayment && (
                    <>
                      <button
                        type="button"
                        onClick={() => setCancelTargetOrder(order)}
                        className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#ba1a1a] hover:bg-[#ba1a1a]/10 transition-colors flex items-center gap-1.5 cursor-pointer"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        <span>Cancel Order</span>
                      </button>
                      <Link
                        href="/checkout"
                        className="px-4 py-1.5 rounded-xl text-xs font-bold bg-[#0045cc] hover:bg-[#0d5bff] text-white transition-all shadow-sm flex items-center gap-1.5 active:scale-[0.99]"
                      >
                        <CreditCard className="w-3.5 h-3.5" />
                        <span>Pay Now</span>
                      </Link>
                    </>
                  )}

                  {/* 2. Request Cancellation if CONFIRMED or PROCESSING */}
                  {isProcessingOrConfirmed && !order.cancellationRequest && (
                    <button
                      type="button"
                      onClick={() => setCancelTargetOrder(order)}
                      className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#ba1a1a] hover:bg-[#ba1a1a]/10 transition-colors flex items-center gap-1.5 cursor-pointer"
                    >
                      <XCircle className="w-3.5 h-3.5" />
                      <span>Request Cancellation</span>
                    </button>
                  )}

                  {/* 3. Confirm Receipt & Return/Refund if DELIVERED */}
                  {isDelivered && (
                    <>
                      <button
                        type="button"
                        onClick={() => setReturnTargetOrder(order)}
                        className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#434656] hover:bg-[#eff4ff] transition-colors flex items-center gap-1.5 cursor-pointer"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        <span>Return / Refund</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleConfirmReceipt(order.id)}
                        className="px-4 py-1.5 rounded-xl text-xs font-bold bg-[#006b5b] hover:bg-[#005245] text-white transition-all shadow-sm flex items-center gap-1.5 cursor-pointer active:scale-[0.99]"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Confirm Receipt</span>
                      </button>
                    </>
                  )}

                  {/* 4. Return/Refund within 7 days if COMPLETED & Buy Again */}
                  {isCompleted && (
                    <>
                      {!order.returnRefundRequest && (
                        <button
                          type="button"
                          onClick={() => setReturnTargetOrder(order)}
                          className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors flex items-center gap-1.5 cursor-pointer"
                        >
                          <RotateCcw className="w-3.5 h-3.5" />
                          <span>Return / Refund</span>
                        </button>
                      )}
                      <Link
                        href="/checkout"
                        className="px-4 py-1.5 rounded-xl text-xs font-semibold bg-[#0045cc] hover:bg-[#0d5bff] text-white transition-all shadow-sm flex items-center gap-1.5 active:scale-[0.99]"
                      >
                        <Repeat className="w-3.5 h-3.5" />
                        <span>Buy Again</span>
                      </Link>
                    </>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Modals */}
      <CancelOrderModal
        order={cancelTargetOrder}
        isOpen={Boolean(cancelTargetOrder)}
        onClose={() => setCancelTargetOrder(null)}
        onSuccess={handleCancelSuccess}
      />

      <ReturnRefundModal
        order={returnTargetOrder}
        isOpen={Boolean(returnTargetOrder)}
        onClose={() => setReturnTargetOrder(null)}
        onSuccess={handleReturnSuccess}
      />

      <ReviewProductModal
        item={reviewTargetItem?.item || null}
        isOpen={Boolean(reviewTargetItem)}
        onClose={() => setReviewTargetItem(null)}
        onSuccess={handleReviewSuccess}
      />
    </main>
  );
}
