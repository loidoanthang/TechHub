"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  ArrowLeft,
  Store,
  Phone,
  MapPin,
  Clock,
  ShieldCheck,
  Check,
  Truck,
  Download,
  MessageSquare,
  Repeat,
  CreditCard,
  Tag,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Star,
  AlertCircle,
} from "lucide-react";
import { type OrderDTO, type OrderItemDTO } from "@/mocks";
import { CancelOrderModal } from "./CancelOrderModal";
import { ReturnRefundModal } from "./ReturnRefundModal";
import { ReviewProductModal } from "./ReviewProductModal";

interface OrderDetailViewProps {
  order: OrderDTO;
}

export function OrderDetailView({ order: initialOrder }: OrderDetailViewProps) {
  const [order, setOrder] = useState<OrderDTO>(initialOrder);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modals
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [reviewTargetItem, setReviewTargetItem] = useState<OrderItemDTO | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const handleConfirmReceipt = () => {
    setOrder((prev) => ({
      ...prev,
      status: "COMPLETED",
      statusLabel: "Completed",
      trackingTimeline: [
        ...prev.trackingTimeline,
        {
          status: "COMPLETED",
          title: "Order Completed",
          description: "Customer confirmed delivery receipt",
          timestamp: "Just now",
          completed: true,
          active: true,
        },
      ],
    }));
    showToast("Receipt confirmed! Escrow funds released to merchant.");
  };

  const handleCancelSuccess = (orderId: string, isDirect: boolean, reason: string) => {
    if (isDirect) {
      setOrder((prev) => ({
        ...prev,
        status: "CANCELLED",
        statusLabel: "Cancelled",
      }));
      showToast("Order cancelled immediately. Inventory restored.");
    } else {
      setOrder((prev) => ({
        ...prev,
        cancellationRequest: {
          id: "req-" + Date.now(),
          orderId,
          reason,
          status: "PENDING",
          createdAt: "Just now",
        },
      }));
      showToast("Cancellation request submitted for merchant review.");
    }
  };

  const handleReturnSuccess = (
    orderId: string,
    reason: string,
    description: string,
    mediaCount: number
  ) => {
    setOrder((prev) => ({
      ...prev,
      returnRefundRequest: {
        id: "rr-" + Date.now(),
        orderId,
        reason,
        description,
        status: "PENDING",
        mediaUrls: [],
        createdAt: "Just now",
      },
    }));
    showToast("Return & Refund request submitted with " + mediaCount + " attachments.");
  };

  const handleReviewSuccess = (
    itemId: string,
    rating: number,
    comment: string,
    mediaCount: number
  ) => {
    setOrder((prev) => ({
      ...prev,
      items: prev.items.map((it) =>
        it.id === itemId ? { ...it, hasReviewed: true } : it
      ),
    }));
    showToast(`Review submitted (${rating} stars)! Thank you for your feedback.`);
  };

  const isDelivered = order.status === "DELIVERED";
  const isCompleted = order.status === "COMPLETED";
  const isPendingPayment = order.status === "PENDING_PAYMENT";
  const isProcessingOrConfirmed =
    order.status === "PROCESSING" || order.status === "CONFIRMED";

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 border border-[#c3c5d9]/30 text-xs animate-in slide-in-from-bottom-5 duration-200">
          <CheckCircle2 className="w-4 h-4 text-[#4edea3] shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* 1. Order Header Bar */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#c3c5d9]/25 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <Link
            href="/account/orders"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#0045cc] hover:underline"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Purchases &amp; Orders</span>
          </Link>
          <div className="flex flex-wrap items-center gap-3 pt-1">
            <h1 className="text-xl font-bold tracking-tight text-[#0b1c30] font-heading">
              Order #{order.orderNumber}
            </h1>
            <span
              className={`text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider ${
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
          <p className="text-xs text-[#737688]">
            Placed on {order.createdAt} • {order.shippingCarrier}
          </p>
        </div>

        {/* Top Quick Actions */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="px-3.5 py-2 rounded-xl bg-[#eff4ff] text-[#0b1c30] hover:bg-[#dce9ff] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <MessageSquare className="w-3.5 h-3.5 text-[#0045cc]" />
            <span>Chat Seller</span>
          </button>
          <button
            type="button"
            className="px-3.5 py-2 rounded-xl bg-[#eff4ff] text-[#0b1c30] hover:bg-[#dce9ff] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-[#0045cc]" />
            <span>E-Invoice</span>
          </button>
        </div>
      </div>

      {/* Cancellation / Return Request Banner */}
      {order.cancellationRequest && (
        <div className="p-4 rounded-2xl bg-[#fef3c7] border border-[#fde68a] text-xs text-[#92400e] flex items-center justify-between">
          <span>
            ⚠️ Cancellation request is currently pending merchant approval. (Reason:{" "}
            <strong>{order.cancellationRequest.reason}</strong>)
          </span>
          <span className="font-bold uppercase text-[11px]">Under Review</span>
        </div>
      )}
      {order.returnRefundRequest && (
        <div className="p-4 rounded-2xl bg-[#eff4ff] border border-[#c3c5d9]/40 text-xs text-[#0045cc] flex items-center justify-between">
          <span>
            🔄 Return &amp; Refund dispute active. TechHub Escrow protection held until resolution.
          </span>
          <span className="font-bold uppercase text-[11px]">In Progress</span>
        </div>
      )}

      {/* 2. Live Tracking Stepper */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-6">
        <div className="flex items-center justify-between border-b border-[#eff4ff] pb-4">
          <div className="flex items-center gap-2.5">
            <Truck className="w-5 h-5 text-[#0045cc]" />
            <div>
              <h2 className="text-sm font-bold text-[#0b1c30]">
                Tracking Milestones
              </h2>
              <p className="text-xs text-[#737688]">
                Carrier: {order.shippingCarrier} • Tracking ID:{" "}
                <span className="font-mono text-[#0045cc] font-semibold">
                  {order.trackingCode}
                </span>
              </p>
            </div>
          </div>
          <a
            href={`tel:${order.carrierHotline}`}
            className="text-xs text-[#0045cc] hover:underline flex items-center gap-1 font-medium"
          >
            <Phone className="w-3.5 h-3.5" />
            <span>Hotline: {order.carrierHotline}</span>
          </a>
        </div>

        {/* Stepper Progress Bar */}
        <div className="relative px-2 pt-2 pb-4 overflow-x-auto">
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3 relative">
            {order.trackingTimeline.map((step, idx) => {
              const isDone = step.completed;
              const isActive = step.active;

              return (
                <div key={idx} className="flex flex-col items-center text-center relative min-w-0">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs shadow-sm z-10 ring-4 ring-white ${
                      isDone
                        ? "bg-[#0045cc] text-white"
                        : "bg-[#eff4ff] text-[#737688]"
                    }`}
                  >
                    {isDone ? (
                      <Check className="w-4 h-4" />
                    ) : (
                      <Clock className="w-4 h-4" />
                    )}
                  </div>

                  <p
                    className={`text-xs font-bold mt-2.5 truncate w-full px-0.5 ${
                      isActive ? "text-[#0045cc]" : "text-[#0b1c30]"
                    }`}
                  >
                    {step.title}
                  </p>
                  <p className="text-[10px] text-[#737688] mt-0.5 leading-tight">
                    {step.timestamp}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 3. Delivery Address Snapshot */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-4">
        <div className="flex items-center justify-between border-b border-[#eff4ff] pb-3">
          <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
            <MapPin className="w-4 h-4 text-[#0045cc]" />
            <span>Delivery Destination</span>
          </div>
          <span className="text-xs text-[#737688] font-mono">{order.trackingCode}</span>
        </div>

        <div className="flex flex-col gap-1.5 text-xs text-[#434656]">
          <div className="flex items-center gap-2 font-bold text-[#0b1c30] text-sm">
            <span>{order.recipientName}</span>
            <span className="font-normal text-xs text-[#737688]">
              {order.recipientPhone}
            </span>
          </div>
          <p className="leading-relaxed">{order.deliveryAddress}</p>
        </div>
      </div>

      {/* 4. Purchased Items Detail */}
      <div className="bg-white rounded-2xl shadow-sm border border-[#c3c5d9]/25 overflow-hidden flex flex-col">
        <div className="px-6 py-4 bg-[#eff4ff]/60 flex items-center justify-between border-b border-[#eff4ff]">
          <h3 className="text-xs font-bold uppercase tracking-wider text-[#0b1c30] flex items-center gap-2">
            <Store className="w-4 h-4 text-[#0045cc]" />
            <span>{order.shopName}</span>
          </h3>
          <span className="text-xs text-[#737688]">
            Items: {order.items.length}
          </span>
        </div>

        <div className="divide-y divide-[#eff4ff]">
          {order.items.map((item) => (
            <div
              key={item.id}
              className="p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
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
                  <h4 className="text-sm font-bold text-[#0b1c30] line-clamp-2 leading-snug">
                    {item.productName}
                  </h4>
                  <div className="flex items-center gap-2 mt-1.5 text-xs text-[#737688]">
                    <span className="bg-[#eff4ff] px-2 py-0.5 rounded text-[11px] font-medium text-[#434656]">
                      {item.variantName}
                    </span>
                    <span className="font-mono text-[11px]">SKU: {item.sku}</span>
                  </div>
                  <span className="text-xs text-[#434656] mt-1">
                    Quantity: <strong className="text-[#0b1c30]">x{item.quantity}</strong>
                  </span>

                  {/* Review Action */}
                  {isCompleted && (
                    <div className="mt-3">
                      {item.hasReviewed ? (
                        <span className="inline-flex items-center gap-1 text-xs text-[#006b5b] font-semibold">
                          <Check className="w-3.5 h-3.5" /> Reviewed
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setReviewTargetItem(item)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-semibold transition-colors cursor-pointer"
                        >
                          <Star className="w-3.5 h-3.5" />
                          <span>Review Product</span>
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div className="flex md:flex-col items-baseline md:items-end justify-between w-full md:w-auto shrink-0">
                <span className="font-heading text-lg font-bold text-[#0b1c30]">
                  {formatPrice(item.price)}
                </span>
                {item.originalPrice && (
                  <span className="text-xs text-[#737688] line-through">
                    {formatPrice(item.originalPrice)}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 5. Payment & Financial Ledger Breakdown */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#c3c5d9]/25 flex flex-col lg:flex-row justify-between gap-6">
        {/* Left: Payment Method info */}
        <div className="flex flex-col gap-3 flex-1">
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#0b1c30]">
            Payment &amp; Security
          </h4>

          <div className="p-4 rounded-xl bg-[#eff4ff]/60 border border-[#eff4ff] flex flex-col gap-2 text-xs">
            <div className="flex items-center gap-2 font-bold text-[#0b1c30]">
              <CreditCard className="w-4 h-4 text-[#0045cc]" />
              <span>{order.paymentMethod}</span>
            </div>
            <p className="text-[#434656]">{order.paymentDetails}</p>
            <div className="flex items-center gap-1.5 text-[11px] text-[#006b5b] font-medium pt-1">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>TechHub 100% Escrow Protection Guaranteed</span>
            </div>
          </div>
        </div>

        {/* Right: Ledger */}
        <div className="w-full lg:w-80 flex flex-col gap-2.5 pt-2 text-xs">
          <h4 className="font-bold uppercase tracking-wide text-[#0b1c30] mb-1">
            Financial Ledger
          </h4>

          <div className="flex items-center justify-between text-[#737688]">
            <span>Merchandise Subtotal:</span>
            <span className="font-mono font-bold text-[#0b1c30]">
              {formatPrice(order.totalProductAmount)}
            </span>
          </div>

          {order.shopDiscountAmount > 0 && (
            <div className="flex items-center justify-between text-[#006b5b]">
              <span className="flex items-center gap-1">
                <Tag className="w-3.5 h-3.5" />
                Store Discount:
              </span>
              <span className="font-mono font-bold">
                -{formatPrice(order.shopDiscountAmount)}
              </span>
            </div>
          )}

          {order.platformDiscountAmount > 0 && (
            <div className="flex items-center justify-between text-[#006b5b]">
              <span className="flex items-center gap-1">
                <Tag className="w-3.5 h-3.5" />
                TechHub Voucher:
              </span>
              <span className="font-mono font-bold">
                -{formatPrice(order.platformDiscountAmount)}
              </span>
            </div>
          )}

          <div className="flex items-center justify-between text-[#737688]">
            <span>Shipping Fee:</span>
            <span className="font-mono font-bold text-[#0b1c30]">
              +{formatPrice(order.shippingFee)}
            </span>
          </div>

          <div className="h-px bg-[#eff4ff] my-1"></div>

          <div className="flex items-baseline justify-between pt-1">
            <div>
              <span className="text-sm font-bold text-[#0b1c30]">Total Amount:</span>
              <p className="text-[10px] text-[#737688]">(VAT &amp; Escrow included)</p>
            </div>
            <span className="font-heading text-xl font-bold text-[#0045cc]">
              {formatPrice(order.totalAmount)}
            </span>
          </div>
        </div>
      </div>

      {/* 6. Footer Operations Bar (State-dependent Actions) */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#c3c5d9]/25 flex flex-wrap items-center justify-end gap-3">
        {/* Cancel button if PENDING_PAYMENT */}
        {isPendingPayment && (
          <>
            <button
              type="button"
              onClick={() => setIsCancelModalOpen(true)}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#ba1a1a] hover:bg-[#ba1a1a]/10 transition-colors cursor-pointer flex items-center gap-1.5"
            >
              <XCircle className="w-4 h-4" />
              <span>Cancel Order</span>
            </button>
            <Link
              href="/checkout"
              className="px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm transition-all active:scale-[0.99]"
            >
              <CreditCard className="w-4 h-4" />
              <span>Pay Now</span>
            </Link>
          </>
        )}

        {/* Request Cancellation if CONFIRMED or PROCESSING */}
        {isProcessingOrConfirmed && !order.cancellationRequest && (
          <button
            type="button"
            onClick={() => setIsCancelModalOpen(true)}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#ba1a1a] hover:bg-[#ba1a1a]/10 transition-colors cursor-pointer flex items-center gap-1.5"
          >
            <XCircle className="w-4 h-4" />
            <span>Request Order Cancellation</span>
          </button>
        )}

        {/* Confirm Receipt & Return/Refund if DELIVERED */}
        {isDelivered && (
          <>
            <button
              type="button"
              onClick={() => setIsReturnModalOpen(true)}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#434656] hover:bg-[#eff4ff] transition-colors cursor-pointer flex items-center gap-1.5"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Request Return &amp; Refund</span>
            </button>
            <button
              type="button"
              onClick={handleConfirmReceipt}
              className="px-5 py-2.5 rounded-xl bg-[#006b5b] hover:bg-[#005245] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm transition-all cursor-pointer active:scale-[0.99]"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>Confirm Receipt</span>
            </button>
          </>
        )}

        {/* Completed order actions: Return & Refund (if within 7 days) and Buy Again */}
        {isCompleted && (
          <>
            {!order.returnRefundRequest && (
              <button
                type="button"
                onClick={() => setIsReturnModalOpen(true)}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer flex items-center gap-1.5"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Return &amp; Refund (Within 7 Days)</span>
              </button>
            )}
            <Link
              href="/checkout"
              className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm transition-all active:scale-[0.99]"
            >
              <Repeat className="w-3.5 h-3.5" />
              <span>Buy Again</span>
            </Link>
          </>
        )}

        {/* For In-Transit orders */}
        {order.status === "SHIPPED" && (
          <Link
            href="/checkout"
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm transition-all active:scale-[0.99]"
          >
            <Repeat className="w-3.5 h-3.5" />
            <span>Buy Again</span>
          </Link>
        )}
      </div>

      {/* Modals */}
      <CancelOrderModal
        order={order}
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        onSuccess={handleCancelSuccess}
      />

      <ReturnRefundModal
        order={order}
        isOpen={isReturnModalOpen}
        onClose={() => setIsReturnModalOpen(false)}
        onSuccess={handleReturnSuccess}
      />

      <ReviewProductModal
        item={reviewTargetItem}
        isOpen={Boolean(reviewTargetItem)}
        onClose={() => setReviewTargetItem(null)}
        onSuccess={handleReviewSuccess}
      />
    </main>
  );
}
