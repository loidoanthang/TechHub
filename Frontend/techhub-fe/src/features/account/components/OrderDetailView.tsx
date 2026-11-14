"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { ProductThumbnail } from "@/components/shared";
import {
  ArrowLeft,
  Store,
  Phone,
  MapPin,
  Clock,
  ShieldCheck,
  Truck,
  CreditCard,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Loader2,
  AlertCircle,
  Star,
} from "lucide-react";
import { useBuyerOrderDetailQuery, useCompleteOrderMutation } from "@/features/orders/hooks/useOrderQueries";
import type { OrderStatus } from "@/types/enums";
import type { BuyerOrderItemResponse } from "@/types/order";
import { CancelOrderModal } from "./CancelOrderModal";
import { ReturnRefundModal } from "./ReturnRefundModal";
import { ReviewProductModal } from "./ReviewProductModal";
import { TimelineStepNode } from "./TimelineStepNode";
import { TimelineStepInfo } from "./TimelineStepInfo";
import { buildNormalizedTimeline } from "@/features/orders/utils/orderTimelineMapper";

interface OrderDetailViewProps {
  orderId: string;
}

export function OrderDetailView({ orderId }: OrderDetailViewProps) {
  const { data: order, isLoading, error, refetch } = useBuyerOrderDetailQuery(orderId);
  const completeOrderMutation = useCompleteOrderMutation();

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [selectedReviewItem, setSelectedReviewItem] =
    useState<BuyerOrderItemResponse | null>(null);

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

  const handleConfirmReceipt = async () => {
    if (!order) return;
    try {
      await completeOrderMutation.mutateAsync(order.orderId);
      showToast("Receipt confirmed! Escrow funds released to merchant.");
      refetch();
    } catch {
      showToast("Failed to confirm receipt.");
    }
  };

  if (isLoading) {
    return (
      <div className="w-full h-96 flex items-center justify-center bg-white rounded-2xl border border-[#c3c5d9]/25">
        <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="w-full bg-white rounded-2xl p-8 border border-[#c3c5d9]/25 text-center flex flex-col items-center gap-4">
        <AlertCircle className="w-10 h-10 text-amber-500" />
        <h2 className="text-lg font-bold text-[#0b1c30]">Order Not Found</h2>
        <p className="text-xs text-[#737688]">
          We could not load details for order #{orderId}. It may not exist or belongs to another user.
        </p>
        <Link
          href="/account/orders"
          className="px-5 py-2.5 rounded-xl bg-[#0045cc] text-white text-xs font-semibold"
        >
          Back to My Orders
        </Link>
      </div>
    );
  }

  const isDelivered = order.status === "DELIVERED";
  const isPendingPayment = order.status === "PENDING_PAYMENT";
  const canCancelOrRequest =
    order.status === "PENDING_PAYMENT" ||
    order.status === "CONFIRMED" ||
    order.status === "PROCESSING";

  const normalizedTimeline = buildNormalizedTimeline(order.timeline, order.status);

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Back Button & Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-[#eff4ff]">
        <div className="flex items-center gap-3">
          <Link
            href="/account/orders"
            className="p-2 rounded-xl bg-white border border-[#c3c5d9]/30 text-[#434656] hover:bg-[#eff4ff] hover:text-[#0045cc] transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
                Order Details #{order.orderNumber}
              </h1>
              <span
                className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full uppercase ${
                  order.status === "COMPLETED"
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                    : order.status === "SHIPPED" || order.status === "DELIVERED"
                    ? "bg-blue-50 text-blue-700 border border-blue-200"
                    : order.status === "CANCELLED"
                    ? "bg-rose-50 text-rose-700 border border-rose-200"
                    : "bg-amber-50 text-amber-700 border border-amber-200"
                }`}
              >
                {order.statusDescription || order.status}
              </span>
            </div>
            <p className="text-xs text-[#737688] font-mono mt-0.5">
              Placed on {new Date(order.createdAt).toLocaleString("vi-VN")}
            </p>
          </div>
        </div>

        {/* Shop Info */}
        <Link
          href={`/shops/${order.shopSlug || order.shopName}`}
          className="flex items-center gap-2.5 px-3.5 py-2 bg-white hover:bg-[#eff4ff] rounded-xl border border-[#c3c5d9]/25 hover:border-[#0045cc]/30 transition-all group cursor-pointer shadow-2xs"
        >
          <Store className="w-4 h-4 text-[#0045cc]" />
          <span className="text-xs font-bold text-[#0b1c30] group-hover:text-[#0045cc] transition-colors">
            {order.shopName}
          </span>
          <span className="text-[11px] font-semibold text-[#0045cc] ml-1 opacity-0 group-hover:opacity-100 transition-opacity">
            Xem Shop &rarr;
          </span>
        </Link>
      </div>

      {/* Tracking Timeline Bento */}
      {normalizedTimeline.length > 0 && (
        <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/25 shadow-sm">
          <h2 className="text-xs font-bold text-[#0b1c30] uppercase tracking-wider mb-6 flex items-center gap-2">
            <Truck className="w-4 h-4 text-[#0045cc]" />
            Order Timeline &amp; Delivery Progress
          </h2>

          {/* DESKTOP & TABLET VIEW: Connected Continuous Stepper */}
          <div className="hidden sm:flex items-start justify-between relative w-full">
            {normalizedTimeline.map((step, idx) => (
              <div
                key={idx}
                className="flex-1 flex flex-col items-center text-center relative px-1"
              >
                {/* Connecting Line to next step */}
                {!step.isLast && (
                  <div
                    aria-hidden="true"
                    className={`absolute top-[17px] left-1/2 w-full h-1 -z-0 transition-all duration-300 ${
                      step.isNextDone ? "bg-primary" : "bg-slate-200"
                    }`}
                  />
                )}

                <TimelineStepNode
                  stepNum={step.stepNum}
                  isCompleted={step.isCompleted}
                  isCurrent={step.isCurrent}
                />

                <TimelineStepInfo
                  title={step.title}
                  description={step.description}
                  timestamp={step.timestamp}
                  isCompleted={step.isCompleted}
                  isCurrent={step.isCurrent}
                  layout="horizontal"
                />
              </div>
            ))}
          </div>

          {/* MOBILE VIEW: Vertical Stepper */}
          <div className="flex sm:hidden flex-col gap-6 relative pl-1">
            {normalizedTimeline.map((step, idx) => (
              <div key={idx} className="flex items-start gap-3.5 relative">
                {/* Connecting Vertical Line */}
                {!step.isLast && (
                  <div
                    aria-hidden="true"
                    className={`absolute left-[17px] top-9 bottom-[-24px] w-0.5 -z-0 ${
                      step.isNextDone ? "bg-primary" : "bg-slate-200"
                    }`}
                  />
                )}

                <TimelineStepNode
                  stepNum={step.stepNum}
                  isCompleted={step.isCompleted}
                  isCurrent={step.isCurrent}
                />

                <TimelineStepInfo
                  title={step.title}
                  description={step.description}
                  timestamp={step.timestamp}
                  isCompleted={step.isCompleted}
                  isCurrent={step.isCurrent}
                  layout="vertical"
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Grid: Delivery Destination & Payment Ledger */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Destination Card */}
        <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/25 shadow-sm flex flex-col gap-3">
          <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] uppercase tracking-wider">
            <MapPin className="w-4 h-4 text-[#0045cc]" />
            Delivery Destination
          </div>
          <div className="flex flex-col gap-1 text-xs">
            <span className="font-bold text-[#0b1c30] text-sm">
              {order.deliveryAddress?.recipientName}
            </span>
            <span className="text-[#737688] flex items-center gap-1.5">
              <Phone className="w-3.5 h-3.5 text-[#0045cc]" />
              {order.deliveryAddress?.phone}
            </span>
            <span className="text-[#0b1c30] mt-1 leading-relaxed">
              {order.deliveryAddress?.fullAddress ||
                `${order.deliveryAddress?.streetAddress}, ${order.deliveryAddress?.ward}, ${order.deliveryAddress?.district}, ${order.deliveryAddress?.province}`}
            </span>
          </div>
        </div>

        {/* Payment Summary */}
        <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/25 shadow-sm flex flex-col gap-3">
          <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] uppercase tracking-wider">
            <CreditCard className="w-4 h-4 text-[#0045cc]" />
            Payment Information
          </div>
          <div className="flex flex-col gap-2 text-xs">
            <div className="flex justify-between">
              <span className="text-[#737688]">Method:</span>
              <strong className="text-[#0b1c30]">{order.paymentMethod}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Status:</span>
              <strong
                className={
                  order.paymentStatus === "COMPLETED"
                    ? "text-emerald-600"
                    : order.paymentStatus === "FAILED"
                    ? "text-rose-600"
                    : "text-amber-600"
                }
              >
                {order.paymentStatus}
              </strong>
            </div>
            <div className="pt-2 border-t border-[#eff4ff] flex justify-between items-baseline">
              <span className="text-xs text-[#737688]">Total Paid / Due:</span>
              <span className="text-base font-bold text-[#0045cc]">
                {formatPrice(order.totalAmount)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Items List */}
      <div className="bg-white rounded-2xl border border-[#c3c5d9]/25 shadow-sm overflow-hidden flex flex-col">
        <div className="p-4 bg-[#eff4ff]/60 border-b border-[#eff4ff] text-xs font-bold text-[#0b1c30]">
          Ordered Components ({order.items.length})
        </div>

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
            const skuText = item.sku || item.variantSku;
            const hasDiscount =
              item.originalPrice !== undefined &&
              item.discountedUnitPrice !== undefined &&
              item.discountedUnitPrice < item.originalPrice;

            return (
              <div
                key={item.orderItemId}
                className="p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
              >
                <div className="flex items-start gap-4 flex-1 min-w-0">
                  <ProductThumbnail
                    src={item.imageUrl}
                    alt={item.productName}
                    sizeClassName="w-16 h-16"
                  />
                  <div className="flex flex-col min-w-0">
                    <h3 className="text-xs font-bold text-[#0b1c30] line-clamp-1">
                      {item.productName}
                    </h3>
                    <span className="text-[11px] text-[#737688] mt-0.5">
                      {skuText ? `SKU: ${skuText} • ` : ""}{item.variantName}
                    </span>
                    <div className="flex items-center gap-2 mt-1">
                      {hasDiscount ? (
                        <>
                          <span className="text-xs text-[#737688] line-through">
                            {formatPrice(item.originalPrice)}
                          </span>
                          <span className="text-xs font-semibold text-[#0b1c30]">
                            {formatPrice(unitPrice)}
                          </span>
                        </>
                      ) : (
                        <span className="text-xs text-[#0b1c30]">
                          {formatPrice(unitPrice)}
                        </span>
                      )}
                      <span className="text-xs text-[#737688]">× {item.quantity}</span>
                    </div>
                  </div>
                </div>

                <div className="flex flex-col sm:items-end gap-2 shrink-0">
                  <div className="text-sm font-bold text-[#0b1c30] sm:text-right">
                    {formatPrice(subtotal)}
                  </div>
                  {(order.status === "DELIVERED" ||
                    order.status === "COMPLETED") && (
                    <button
                      type="button"
                      onClick={() => setSelectedReviewItem(item)}
                      className="px-3 py-1.5 rounded-xl border border-[#0045cc]/30 text-[#0045cc] hover:bg-[#0045cc]/5 text-xs font-semibold flex items-center gap-1.5 cursor-pointer transition-colors shadow-2xs"
                    >
                      <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                      <span>{item.hasReviewed ? "Sửa đánh giá" : "Đánh giá sản phẩm"}</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Pricing Breakdown */}
        <div className="p-5 bg-[#eff4ff]/20 border-t border-[#eff4ff] flex flex-col gap-2 text-xs">
          <div className="flex justify-between text-[#737688]">
            <span>Products Subtotal:</span>
            <span>{formatPrice(order.totalProductAmount)}</span>
          </div>
          <div className="flex justify-between text-[#737688]">
            <span>Shipping Fee:</span>
            <span>{formatPrice(order.shippingFee)}</span>
          </div>
          {order.shopDiscountAmount > 0 && (
            <div className="flex justify-between text-emerald-600">
              <span>Shop Voucher Discount:</span>
              <span>-{formatPrice(order.shopDiscountAmount)}</span>
            </div>
          )}
          {order.platformDiscountAmount > 0 && (
            <div className="flex justify-between text-emerald-600">
              <span>Platform Voucher Discount:</span>
              <span>-{formatPrice(order.platformDiscountAmount)}</span>
            </div>
          )}
          <div className="pt-2 border-t border-[#eff4ff] flex justify-between items-baseline text-sm font-bold text-[#0b1c30]">
            <span>Total Payable:</span>
            <span className="text-base text-[#0045cc]">
              {formatPrice(order.totalAmount)}
            </span>
          </div>
        </div>

        {/* Order Actions */}
        <div className="p-4 bg-white border-t border-[#eff4ff] flex items-center justify-end gap-3 flex-wrap">
          {isPendingPayment && (
            <Link
              href={`/checkout?orderId=${order.orderId}`}
              className="px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold shadow-sm"
            >
              Pay Now
            </Link>
          )}

          {isDelivered && (
            <>
              <button
                type="button"
                onClick={handleConfirmReceipt}
                disabled={completeOrderMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-[#006b5b] hover:bg-[#008b77] text-white text-xs font-semibold shadow-sm flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Confirm Receipt</span>
              </button>
              <button
                type="button"
                onClick={() => setIsReturnModalOpen(true)}
                className="px-5 py-2.5 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Return / Refund</span>
              </button>
            </>
          )}

          {canCancelOrRequest && (
            <button
              type="button"
              onClick={() => setIsCancelModalOpen(true)}
              className="px-5 py-2.5 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-semibold cursor-pointer"
            >
              {isPendingPayment ? "Cancel Order" : "Request Cancellation"}
            </button>
          )}
        </div>
      </div>

      {/* Cancel Order Modal */}
      {isCancelModalOpen && (
        <CancelOrderModal
          orderId={order.orderId}
          orderNumber={order.orderNumber}
          shopName={order.shopName}
          status={order.status}
          isOpen={isCancelModalOpen}
          onClose={() => setIsCancelModalOpen(false)}
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
      {isReturnModalOpen && (
        <ReturnRefundModal
          orderId={order.orderId}
          orderNumber={order.orderNumber}
          shopName={order.shopName}
          totalAmount={order.totalAmount}
          isOpen={isReturnModalOpen}
          onClose={() => setIsReturnModalOpen(false)}
          onSuccess={() => {
            showToast("Return & Refund request submitted successfully.");
            refetch();
          }}
        />
      )}

      {/* Review Product Modal */}
      {selectedReviewItem && (
        <ReviewProductModal
          item={selectedReviewItem}
          isOpen={Boolean(selectedReviewItem)}
          onClose={() => setSelectedReviewItem(null)}
          onSuccess={(msg) => {
            showToast(msg);
            refetch();
          }}
        />
      )}

      {/* Toast Notification */}
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
