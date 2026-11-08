"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Package,
  Printer,
  ShieldCheck,
  Check,
  CheckCircle2,
  Clock,
  MapPin,
  Phone,
  CreditCard,
  Banknote,
  AlertTriangle,
  Loader2,
  Truck,
  ShoppingBag,
  Home,
  FileCheck,
} from "lucide-react";
import { formatPrice } from "@/lib/utils";
import {
  useSellerOrderDetailQuery,
  useUpdateSellerOrderStatusMutation,
  useSellerCancelOrderMutation,
} from "../../hooks/useSellerOrders";
import {
  StatusTransitionModal,
  CancelOrderModal,
  PrintShippingLabelModal,
} from "./SellerOrderActionModals";
import type { OrderStatus, CancellationReasonCode } from "@/types/enums";

interface SellerOrderDetailViewProps {
  orderId: string;
}

// Fallback metadata for the 6-step lifecycle
const STEP_DEFAULTS = [
  { name: "Đặt hàng", desc: "Người mua đã hoàn tất đặt đơn hàng", icon: ShoppingBag },
  { name: "Xác nhận", desc: "Đơn hàng đã được xác nhận, chờ đóng gói", icon: FileCheck },
  { name: "Đang đóng gói", desc: "Shop kiểm tra serial & niêm phong chống sốc", icon: Package },
  { name: "Đang giao hàng", desc: "Đơn vị vận chuyển đang phát hàng", icon: Truck },
  { name: "Đã giao hàng", desc: "Kiện hàng đã bàn giao thành công", icon: Home },
  { name: "Hoàn tất", desc: "Quyết toán doanh thu vào ví Escrow", icon: ShieldCheck },
];

const STATUS_PROGRESS_MAP: Partial<Record<OrderStatus, number>> = {
  PENDING_PAYMENT: 1,
  PENDING_CONFIRMATION: 1,
  CONFIRMED: 2,
  PREPARING: 3,
  PROCESSING: 3,
  WAITING_FOR_PICKUP: 3,
  SHIPPING: 4,
  SHIPPED: 4,
  DELIVERED: 5,
  COMPLETED: 6,
  CANCELLED: 1,
  RETURN_REQUESTED: 5,
  RETURNED: 5,
  REFUNDED: 6,
};

export function SellerOrderDetailView({ orderId }: SellerOrderDetailViewProps) {
  const { data: order, isLoading, error } = useSellerOrderDetailQuery(orderId);
  const updateStatusMutation = useUpdateSellerOrderStatusMutation();
  const cancelOrderMutation = useSellerCancelOrderMutation();

  // Modals
  const [transitionTargetStatus, setTransitionTargetStatus] = useState<OrderStatus | null>(null);
  const [transitionModalTitle, setTransitionModalTitle] = useState("");
  const [transitionModalDesc, setTransitionModalDesc] = useState("");
  const [isTransitionOpen, setIsTransitionOpen] = useState(false);

  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [isPrintLabelOpen, setIsPrintLabelOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="min-h-[400px] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-primary animate-spin motion-reduce:animate-none" />
        <span className="text-xs font-semibold text-slate-600">
          Đang tải chi tiết đơn hàng...
        </span>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="bg-white rounded-3xl p-8 max-w-md mx-auto text-center border border-slate-200 flex flex-col items-center gap-3">
        <AlertTriangle className="w-8 h-8 text-red-600" />
        <h3 className="font-heading text-base font-bold text-on-surface">
          Không tìm thấy đơn hàng
        </h3>
        <p className="text-xs text-slate-600">
          {(error as Error)?.message || "Đơn hàng không tồn tại hoặc bạn không có quyền truy cập."}
        </p>
        <Link
          href="/seller/orders"
          className="mt-2 px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all"
        >
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  const actions = order.availableActions || [];
  const hasConfirm = actions.includes("CONFIRM_ORDER");
  const hasStartPreparing = actions.includes("START_PREPARING");
  const hasReadyPickup = actions.includes("READY_FOR_PICKUP");
  const hasPrintLabel = actions.includes("PRINT_SHIPPING_LABEL");
  const hasCancel = actions.includes("CANCEL_ORDER");

  const isCOD = order.paymentMethod === "COD";

  const handleConfirmTransition = async (
    orderIdToUpdate: string,
    status: OrderStatus,
    note?: string
  ) => {
    await updateStatusMutation.mutateAsync({
      orderId: orderIdToUpdate,
      data: { status, note },
    });
  };

  const handleConfirmCancel = async (
    orderIdToCancel: string,
    reasonCode: CancellationReasonCode,
    detail?: string
  ) => {
    await cancelOrderMutation.mutateAsync({
      orderId: orderIdToCancel,
      data: { reasonCode, reasonDetail: detail },
    });
  };

  // Helper to format timestamps reliably
  const formatTime = (ts?: string | null) => {
    if (!ts || ts === "Pending") return null;
    try {
      const d = new Date(ts);
      if (isNaN(d.getTime())) return ts;
      return d.toLocaleDateString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
      });
    } catch {
      return ts;
    }
  };

  // Normalizing timeline steps to handle any backend property naming (completed, active, etc.)
  const normalizedTimeline = (order.timeline || []).map((step, idx) => {
    const stepNum = step.stepNumber || idx + 1;
    const fallbackMeta = STEP_DEFAULTS[idx] || {
      name: `Bước ${stepNum}`,
      desc: step.description,
      icon: Package,
    };

    const rawCompleted = (step as { completed?: boolean }).completed ?? step.isCompleted;
    const rawCurrent =
      (step as { current?: boolean; active?: boolean }).current ??
      (step as { current?: boolean; active?: boolean }).active ??
      step.isCurrent;

    const currentOrderStep = STATUS_PROGRESS_MAP[order.status] ?? 2;

    const isCompleted =
      typeof rawCompleted === "boolean"
        ? rawCompleted
        : stepNum < currentOrderStep || order.status === "COMPLETED";

    const isCurrent =
      typeof rawCurrent === "boolean"
        ? rawCurrent
        : stepNum === currentOrderStep && order.status !== "COMPLETED";

    return {
      stepNum,
      stepName: step.stepName || fallbackMeta.name,
      description: step.description || fallbackMeta.desc,
      timestamp: step.timestamp,
      isCompleted,
      isCurrent,
      Icon: fallbackMeta.icon,
    };
  });

  return (
    <div className="flex flex-col gap-6 max-w-6xl mx-auto">
      {/* 1. Top Bar: Back button, Order number, Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            href="/seller/orders"
            aria-label="Quay lại danh sách đơn hàng"
            className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-600 hover:text-primary hover:bg-blue-50 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-heading text-xl font-extrabold text-on-surface">
                Đơn Hàng #{order.orderNumber}
              </h1>
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
                {order.statusDescription || order.status}
              </span>
            </div>
            <p className="text-xs text-slate-600 mt-0.5">
              Tạo lúc: {new Date(order.createdAt).toLocaleString("vi-VN")}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
          {hasPrintLabel && (
            <button
              type="button"
              onClick={() => setIsPrintLabelOpen(true)}
              className="min-h-[40px] py-2 px-3.5 rounded-xl bg-white hover:bg-blue-50 text-on-surface-variant hover:text-primary border border-slate-200 text-xs font-bold transition-all shadow-2xs flex items-center gap-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <Printer className="w-4 h-4" />
              <span>In Phiếu Gửi</span>
            </button>
          )}

          {hasConfirm && (
            <button
              type="button"
              onClick={() => {
                setTransitionTargetStatus("CONFIRMED");
                setTransitionModalTitle("Xác Nhận Đơn Hàng");
                setTransitionModalDesc("Xác nhận tiếp nhận đơn hàng để cam kết đóng gói linh kiện.");
                setIsTransitionOpen(true);
              }}
              className="min-h-[40px] py-2 px-4 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Xác nhận đơn
            </button>
          )}

          {hasStartPreparing && (
            <button
              type="button"
              onClick={() => {
                setTransitionTargetStatus("PREPARING");
                setTransitionModalTitle("Bắt Đầu Đóng Gói");
                setTransitionModalDesc("Chuyển trạng thái sang Đang chuẩn bị linh kiện.");
                setIsTransitionOpen(true);
              }}
              className="min-h-[40px] py-2 px-4 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Bắt đầu đóng gói
            </button>
          )}

          {hasReadyPickup && (
            <button
              type="button"
              onClick={() => {
                setTransitionTargetStatus("WAITING_FOR_PICKUP");
                setTransitionModalTitle("Đóng Gói Xong - Chờ Bưu Tá Lấy");
                setTransitionModalDesc("Xác nhận kiện hàng đã dán tem niêm phong và sẵn sàng bàn giao.");
                setIsTransitionOpen(true);
              }}
              className="min-h-[40px] py-2 px-4 rounded-xl bg-secondary hover:bg-teal-700 text-white text-xs font-bold transition-all shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary"
            >
              Đóng gói xong (Chờ lấy)
            </button>
          )}

          {hasCancel && (
            <button
              type="button"
              onClick={() => setIsCancelOpen(true)}
              className="min-h-[40px] py-2 px-3.5 rounded-xl text-xs font-semibold text-red-700 hover:bg-red-50 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-600"
            >
              Hủy đơn
            </button>
          )}
        </div>
      </div>

      {/* 2. Professional Progress Bar Stepper */}
      {normalizedTimeline.length > 0 && (
        <section
          aria-label="Tiến trình xử lý đơn hàng"
          className="bg-white rounded-3xl p-5 sm:p-7 border border-slate-200 shadow-2xs"
        >
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-primary" />
              <h2 className="font-heading text-xs font-extrabold text-slate-700 tracking-wider uppercase">
                Tiến Trình Xử Lý Đơn Hàng
              </h2>
            </div>
            <span className="text-xs font-bold text-primary bg-primary/10 px-2.5 py-0.5 rounded-full">
              {order.statusDescription || order.status}
            </span>
          </div>

          {/* DESKTOP & TABLET VIEW: Continuous Connected Stepper */}
          <div className="hidden md:flex items-start justify-between relative w-full">
            {normalizedTimeline.map((step, idx) => {
              const formattedTime = formatTime(step.timestamp);
              const isLast = idx === normalizedTimeline.length - 1;

              // Check if the connecting line to the next step should be colored blue
              const nextStep = normalizedTimeline[idx + 1];
              const isNextDone = nextStep ? nextStep.isCompleted || nextStep.isCurrent : false;

              return (
                <div
                  key={step.stepNum}
                  className="flex-1 flex flex-col items-center text-center relative px-2"
                >
                  {/* Connecting line to NEXT step (starts at center of this node, reaches center of next) */}
                  {!isLast && (
                    <div
                      aria-hidden="true"
                      className={`absolute top-4 left-1/2 w-full h-0.5 -z-0 transition-all duration-300 ${
                        isNextDone ? "bg-primary" : "bg-slate-200"
                      }`}
                    />
                  )}

                  {/* Node Circle */}
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs relative z-10 ring-4 ring-white shadow-xs transition-all ${
                      step.isCompleted
                        ? "bg-primary text-white shadow-sm"
                        : step.isCurrent
                        ? "bg-primary text-white ring-4 ring-primary/20 shadow-md animate-pulse motion-reduce:animate-none"
                        : "bg-white border-2 border-slate-300 text-slate-500"
                    }`}
                  >
                    {step.isCompleted ? (
                      <Check className="w-4 h-4 stroke-[2.5]" />
                    ) : step.isCurrent ? (
                      <step.Icon className="w-4 h-4" />
                    ) : (
                      <span>{step.stepNum}</span>
                    )}
                  </div>

                  {/* Step Title */}
                  <h3
                    className={`mt-3 text-xs font-bold leading-tight ${
                      step.isCompleted || step.isCurrent ? "text-on-surface" : "text-slate-500"
                    }`}
                  >
                    {step.stepName}
                  </h3>

                  {/* Step Description */}
                  <p className="mt-1 text-[11px] text-slate-500 leading-snug line-clamp-2 max-w-[150px]">
                    {step.description}
                  </p>

                  {/* Timestamp Pill */}
                  {formattedTime && (
                    <time
                      dateTime={step.timestamp || undefined}
                      className="mt-1.5 text-[10px] font-mono font-medium text-slate-600 bg-slate-50 px-2 py-0.5 rounded border border-slate-200"
                    >
                      {formattedTime}
                    </time>
                  )}
                </div>
              );
            })}
          </div>

          {/* MOBILE VIEW: Vertical Stepper with continuous left track */}
          <div className="flex md:hidden flex-col relative pl-2">
            {normalizedTimeline.map((step, idx) => {
              const formattedTime = formatTime(step.timestamp);
              const isLast = idx === normalizedTimeline.length - 1;
              const nextStep = normalizedTimeline[idx + 1];
              const isNextDone = nextStep ? nextStep.isCompleted || nextStep.isCurrent : false;

              return (
                <div key={step.stepNum} className="flex items-start gap-3.5 relative pb-6 last:pb-0">
                  {/* Vertical connector line */}
                  {!isLast && (
                    <div
                      aria-hidden="true"
                      className={`absolute left-[15px] top-8 bottom-0 w-0.5 -z-0 transition-colors ${
                        isNextDone ? "bg-primary" : "bg-slate-200"
                      }`}
                    />
                  )}

                  {/* Step circle */}
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 relative z-10 ring-4 ring-white shadow-xs ${
                      step.isCompleted
                        ? "bg-primary text-white"
                        : step.isCurrent
                        ? "bg-primary text-white ring-4 ring-primary/20 shadow-md"
                        : "bg-white border-2 border-slate-300 text-slate-500"
                    }`}
                  >
                    {step.isCompleted ? (
                      <Check className="w-4 h-4 stroke-[2.5]" />
                    ) : step.isCurrent ? (
                      <step.Icon className="w-4 h-4" />
                    ) : (
                      <span>{step.stepNum}</span>
                    )}
                  </div>

                  {/* Content details */}
                  <div className="flex-1 min-w-0 pt-0.5">
                    <div className="flex flex-wrap items-center justify-between gap-1.5">
                      <h3
                        className={`text-xs font-bold ${
                          step.isCompleted || step.isCurrent ? "text-on-surface" : "text-slate-500"
                        }`}
                      >
                        {step.stepName}
                      </h3>
                      {formattedTime && (
                        <time
                          dateTime={step.timestamp || undefined}
                          className="text-[10px] font-mono text-slate-600 bg-slate-50 px-1.5 py-0.2 rounded border border-slate-200"
                        >
                          {formattedTime}
                        </time>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                      {step.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* 3. Main 2-Column Bento Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (8 cols): Recipient Address & Products List */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          {/* Recipient Address Card */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-2xs flex flex-col gap-3">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <MapPin className="w-4 h-4 text-primary" />
              <h3 className="font-heading text-sm font-bold text-on-surface">
                Địa Chỉ Giao Hàng (Người Nhận)
              </h3>
            </div>

            <div className="flex flex-col gap-1 text-xs">
              <span className="font-bold text-on-surface text-sm">
                {order.deliveryAddress?.contactName || order.buyerInfo?.recipientName}
              </span>
              <span className="font-mono text-on-surface-variant flex items-center gap-1.5 mt-0.5">
                <Phone className="w-3.5 h-3.5 text-primary" />
                {order.deliveryAddress?.phone || order.buyerInfo?.recipientPhone}
              </span>
              <p className="text-slate-600 leading-relaxed mt-1">
                {order.deliveryAddress?.streetAddress
                  ? `${order.deliveryAddress.streetAddress}, ${order.deliveryAddress.ward}, ${order.deliveryAddress.district}, ${order.deliveryAddress.province}`
                  : order.buyerInfo?.deliveryAddressSummary || order.buyerInfo?.provinceCity}
              </p>
            </div>
          </div>

          {/* Products Table Card */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-2xs flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Package className="w-4 h-4 text-primary" />
                <h3 className="font-heading text-sm font-bold text-on-surface">
                  Danh Mục Linh Kiện Đóng Gói ({order.totalItemsCount} món)
                </h3>
              </div>
            </div>

            <div className="flex flex-col divide-y divide-slate-100">
              {order.items?.map((item) => (
                <div
                  key={item.orderItemId}
                  className="py-3.5 first:pt-0 last:pb-0 flex items-center justify-between gap-4"
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    <div className="w-14 h-14 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                      {item.imageUrl ? (
                        <img
                          src={item.imageUrl}
                          alt={item.productName}
                          width={56}
                          height={56}
                          loading="lazy"
                          decoding="async"
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Package className="w-6 h-6 text-slate-400" />
                      )}
                    </div>
                    <div className="min-w-0">
                      <h4 className="text-xs font-bold text-on-surface truncate" title={item.productName}>
                        {item.productName}
                      </h4>
                      <p className="text-[11px] text-slate-600 mt-0.5">
                        Phân loại: <span className="font-semibold text-on-surface-variant">{item.variantName}</span>
                      </p>
                      {item.sku && (
                        <span className="text-[10px] font-mono text-slate-600 bg-slate-50 px-1.5 py-0.5 rounded border border-slate-200 mt-1 inline-block">
                          SKU: {item.sku}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <span className="text-xs font-bold text-on-surface">
                      {formatPrice(item.finalSubtotal)}
                    </span>
                    <p className="text-[11px] text-slate-600 mt-0.5">
                      {formatPrice(item.discountedUnitPrice)} x {item.quantity}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column (4 cols): Settlement & Financials */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          {/* Financial Breakdown Card */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-2xs flex flex-col gap-4">
            <h3 className="font-heading text-xs font-extrabold text-slate-700 tracking-wider uppercase border-b border-slate-100 pb-3">
              Chi Tiết Dòng Tiền Quyết Toán
            </h3>

            <div className="flex flex-col gap-2.5 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-600">Tiền hàng (Subtotal):</span>
                <span className="font-medium text-on-surface">
                  {formatPrice(order.totalProductAmount)}
                </span>
              </div>

              <div className="flex justify-between">
                <span className="text-slate-600">Phí vận chuyển:</span>
                <span className="font-medium text-on-surface">
                  +{formatPrice(order.shippingFee)}
                </span>
              </div>

              {order.shopDiscountAmount > 0 && (
                <div className="flex justify-between text-purple-700">
                  <span>Khuyến mãi từ Shop:</span>
                  <span>-{formatPrice(order.shopDiscountAmount)}</span>
                </div>
              )}

              {order.platformDiscountAmount > 0 && (
                <div className="flex justify-between text-blue-700">
                  <span>Trợ giá từ Sàn:</span>
                  <span>-{formatPrice(order.platformDiscountAmount)}</span>
                </div>
              )}

              <div className="flex justify-between text-amber-700 border-t border-slate-100 pt-2">
                <span>Phí hoa hồng sàn TechHub (5%):</span>
                <span>-{formatPrice(order.totalCommission)}</span>
              </div>

              {/* Net Payout to Seller */}
              <div className="p-3 bg-blue-50/60 rounded-2xl border border-primary/20 flex justify-between items-center mt-1">
                <div>
                  <span className="font-bold text-primary text-xs block">
                    Doanh thu thực nhận Shop:
                  </span>
                  <span className="text-[10px] text-slate-600">Giải ngân vào ví Escrow</span>
                </div>
                <span className="font-heading text-base font-extrabold text-primary">
                  {formatPrice(order.totalSellerEarnings)}
                </span>
              </div>

              {/* Total Buyer Paid */}
              <div className="flex justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-600">
                <span>Tổng khách thanh toán:</span>
                <span className="font-bold text-on-surface">
                  {formatPrice(order.buyerTotalAmount)}
                </span>
              </div>
            </div>

            {/* Payment Method pill */}
            <div className="p-3 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between text-xs">
              <span className="text-slate-600">Hình thức thanh toán:</span>
              <span className="font-bold text-on-surface">
                {order.paymentMethodName || (isCOD ? "COD Thu tiền mặt" : "VNPAY Online")}
              </span>
            </div>
          </div>

          {/* Tamper-Proof & Hardware Guarantee Card */}
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-secondary shrink-0 mt-0.5" />
            <div className="text-xs">
              <strong className="text-on-surface block">Chính sách bảo chứng TechHub:</strong>
              <p className="text-slate-600 mt-1 leading-relaxed">
                Linh kiện cần dán tem niêm phong trước khi bàn giao bưu tá để được đảm bảo quyền lợi khi phát sinh tranh chấp vận chuyển.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Action Modals */}
      <StatusTransitionModal
        order={order}
        targetStatus={transitionTargetStatus}
        title={transitionModalTitle}
        description={transitionModalDesc}
        isOpen={isTransitionOpen}
        onClose={() => setIsTransitionOpen(false)}
        onConfirm={handleConfirmTransition}
        isLoading={updateStatusMutation.isPending}
      />

      <CancelOrderModal
        order={order}
        isOpen={isCancelOpen}
        onClose={() => setIsCancelOpen(false)}
        onConfirm={handleConfirmCancel}
        isLoading={cancelOrderMutation.isPending}
      />

      <PrintShippingLabelModal
        order={order}
        isOpen={isPrintLabelOpen}
        onClose={() => setIsPrintLabelOpen(false)}
      />
    </div>
  );
}
