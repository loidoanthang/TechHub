"use client";

import React, { useState, memo } from "react";
import Link from "next/link";
import {
  Package,
  Printer,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  CreditCard,
  Banknote,
} from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { SellerOrderResponse } from "@/types/sellerOrders";
import type { OrderStatus } from "@/types/enums";

interface SellerOrderItemCardProps {
  order: SellerOrderResponse;
  onConfirmOrder?: (order: SellerOrderResponse) => void;
  onStartPreparing?: (order: SellerOrderResponse) => void;
  onReadyForPickup?: (order: SellerOrderResponse) => void;
  onPrintShippingLabel?: (order: SellerOrderResponse) => void;
  onCancelOrder?: (order: SellerOrderResponse) => void;
}

export const SellerOrderItemCard = memo(function SellerOrderItemCard({
  order,
  onConfirmOrder,
  onStartPreparing,
  onReadyForPickup,
  onPrintShippingLabel,
  onCancelOrder,
}: SellerOrderItemCardProps) {
  const [expanded, setExpanded] = useState(false);

  const items = order.items || [];
  const displayItems = expanded ? items : items.slice(0, 2);
  const hasMoreItems = items.length > 2;

  // Resolve distinct badge styles with WCAG AA compliance (4.5:1 contrast)
  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case "PENDING_PAYMENT":
        return "bg-amber-100 text-amber-900 border-amber-300";
      case "PENDING_CONFIRMATION":
        return "bg-purple-100 text-purple-900 border-purple-300";
      case "CONFIRMED":
        return "bg-blue-100 text-blue-900 border-blue-300";
      case "PREPARING":
        return "bg-indigo-100 text-indigo-900 border-indigo-300";
      case "WAITING_FOR_PICKUP":
        return "bg-teal-100 text-teal-900 border-teal-300";
      case "SHIPPING":
        return "bg-sky-100 text-sky-900 border-sky-300";
      case "DELIVERED":
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-900 border-emerald-300";
      case "CANCELLED":
        return "bg-slate-200 text-slate-800 border-slate-300";
      default:
        return "bg-slate-100 text-slate-800 border-slate-200";
    }
  };

  const isCOD = order.paymentMethod === "COD";

  const actions = order.availableActions || [];
  const hasConfirm = actions.includes("CONFIRM_ORDER");
  const hasStartPreparing = actions.includes("START_PREPARING");
  const hasReadyPickup = actions.includes("READY_FOR_PICKUP");
  const hasPrintLabel = actions.includes("PRINT_SHIPPING_LABEL");
  const hasCancel = actions.includes("CANCEL_ORDER");

  return (
    <article className="bg-white rounded-2xl border border-slate-200 shadow-2xs hover:border-primary/40 transition-all overflow-hidden flex flex-col">
      {/* 1. Header Bar: Order ID, Timestamp, Recipient info & Status */}
      <div className="px-4 sm:px-5 py-3 bg-slate-50 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 text-xs">
        {/* Mobile Top Row: Recipient + Time (Left) & Status Badge (Right) */}
        <div className="flex sm:hidden items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 text-xs font-bold text-slate-900 truncate">
            <span className="truncate">{order.buyerInfo?.recipientName || order.buyerInfo?.buyerName}</span>
            <span className="text-slate-400 font-normal">•</span>
            <time dateTime={order.createdAt} className="text-slate-500 font-normal text-[11px] shrink-0">
              {new Date(order.createdAt).toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </time>
          </div>
          <span
            className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getStatusBadge(
              order.status
            )}`}
          >
            {order.statusDescription || order.status}
          </span>
        </div>

        {/* Mobile Sub-row: Order Number */}
        <div className="flex sm:hidden items-center justify-between text-xs font-mono">
          <Link
            href={`/seller/orders/${order.orderId}`}
            className="font-bold text-primary hover:underline text-[11px]"
          >
            #{order.orderNumber}
          </Link>
          {items.length > 1 && (
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-200 text-slate-700">
              {order.totalItemsCount} sản phẩm
            </span>
          )}
        </div>

        {/* Desktop Header (> sm) */}
        <div className="hidden sm:flex flex-wrap items-center gap-2 sm:gap-3">
          <Link
            href={`/seller/orders/${order.orderId}`}
            className="font-mono font-bold text-primary hover:underline flex items-center gap-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded"
          >
            <span>#{order.orderNumber}</span>
          </Link>

          <span aria-hidden="true" className="text-slate-400">
            •
          </span>

          <time
            dateTime={order.createdAt}
            className="text-slate-600 font-medium text-[11px] sm:text-xs"
          >
            {new Date(order.createdAt).toLocaleDateString("vi-VN", {
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </time>

          <span aria-hidden="true" className="text-slate-400">
            •
          </span>

          {/* Recipient summary pill */}
          <div className="flex items-center gap-1.5 text-on-surface-variant bg-white px-2.5 py-1 rounded-lg border border-slate-200 text-xs">
            <span className="font-bold text-on-surface truncate">
              {order.buyerInfo?.recipientName || order.buyerInfo?.buyerName}
            </span>
            {order.buyerInfo?.recipientPhone && (
              <span className="text-slate-600 font-mono text-[11px]">
                ({order.buyerInfo.recipientPhone})
              </span>
            )}
            {order.buyerInfo?.provinceCity && (
              <span className="text-slate-600">
                - {order.buyerInfo.provinceCity}
              </span>
            )}
          </div>
        </div>

        {/* Desktop Status Badge & Items count (> sm) */}
        <div className="hidden sm:flex items-center justify-end gap-2">
          {items.length > 1 && (
            <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
              {order.totalItemsCount} sản phẩm
            </span>
          )}
          <span
            className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${getStatusBadge(
              order.status
            )}`}
          >
            {order.statusDescription || order.status}
          </span>
        </div>
      </div>

      {/* 2. Body Grid: Products List + Financials + Actions */}
      <div className="p-3.5 sm:p-5 flex flex-col lg:grid lg:grid-cols-12 gap-3 sm:gap-6 items-stretch lg:items-center">
        {/* Left Column (6 cols): Items List */}
        <div className="lg:col-span-6 flex flex-col gap-3 min-w-0">
          {displayItems.map((item, idx) => (
            <div key={item.orderItemId || idx} className="flex items-start gap-3">
              {/* Product Thumbnail with lazy loading & fixed aspect to prevent CLS */}
              <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                {item.imageUrl ? (
                  <img
                    src={item.imageUrl}
                    alt={item.productName}
                    width={64}
                    height={64}
                    loading="lazy"
                    decoding="async"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Package aria-hidden="true" className="w-5 h-5 text-slate-400" />
                )}
              </div>

              {/* Product Title & Spec details */}
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <h4 className="text-xs font-bold text-on-surface line-clamp-2" title={item.productName}>
                    {item.productName}
                  </h4>
                  {/* Desktop Price */}
                  <span className="text-xs font-bold text-on-surface shrink-0 hidden sm:inline">
                    {formatPrice(item.finalSubtotal)}
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-1.5 mt-1">
                  {item.variantName && (
                    <span className="text-[11px] text-on-surface-variant font-medium bg-slate-100 px-2 py-0.5 rounded">
                      {item.variantName}
                    </span>
                  )}
                  {item.sku && (
                    <span className="text-[10px] font-mono font-semibold text-slate-600 bg-slate-50 px-1.5 py-0.5 rounded border border-slate-200">
                      SKU: {item.sku}
                    </span>
                  )}
                </div>

                {/* Mobile Bottom Row for Item: Qty & Price */}
                <div className="flex sm:hidden items-center justify-between mt-2 pt-1">
                  <span className="text-[11px] font-extrabold text-primary bg-primary/10 px-2 py-0.5 rounded-md border border-primary/20">
                    x{item.quantity}
                  </span>
                  <span className="text-xs font-bold text-slate-900">
                    {formatPrice(item.finalSubtotal)}
                  </span>
                </div>

                {/* Desktop Qty */}
                <div className="hidden sm:inline-block mt-1.5">
                  <span className="text-[11px] font-extrabold text-primary bg-primary/10 px-2 py-0.5 rounded-md border border-primary/20">
                    x{item.quantity}
                  </span>
                </div>
              </div>
            </div>
          ))}

          {/* Expand / Collapse multi items */}
          {hasMoreItems && (
            <button
              type="button"
              onClick={() => setExpanded(!expanded)}
              className="text-xs font-bold text-primary hover:underline flex items-center gap-1 self-start mt-1 py-1 px-1 rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              {expanded ? (
                <>
                  <ChevronUp className="w-4 h-4" />
                  <span>Thu gọn danh sách</span>
                </>
              ) : (
                <>
                  <ChevronDown className="w-4 h-4" />
                  <span>Xem thêm {items.length - 2} sản phẩm khác</span>
                </>
              )}
            </button>
          )}

          {/* Internal Note / Buyer Note */}
          {order.note && (
            <div className="text-[11px] text-slate-700 bg-amber-50 p-2.5 rounded-xl border border-amber-200 flex items-start gap-1.5 mt-1">
              <span className="font-bold text-amber-900 shrink-0">Ghi chú:</span>
              <span className="line-clamp-2">{order.note}</span>
            </div>
          )}
        </div>

        {/* Middle Column (3 cols): Financials & Net Earnings */}
        <div className="lg:col-span-3 flex flex-row lg:flex-col items-center lg:items-start justify-between border-t lg:border-t-0 lg:border-l border-slate-100 pt-2.5 lg:pt-0 lg:pl-5 min-w-0">
          <div>
            <div className="flex items-center gap-1.5">
              {isCOD ? (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 flex items-center gap-1 whitespace-nowrap">
                  <Banknote className="w-3 h-3 text-slate-500 shrink-0" />
                  <span>COD</span>
                </span>
              ) : (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-1 whitespace-nowrap">
                  <CreditCard className="w-3 h-3 text-emerald-600 shrink-0" />
                  <span>VNPAY</span>
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-500 mt-1 hidden sm:block">
              Khách trả: {formatPrice(order.buyerTotalAmount)}
            </p>
          </div>

          <div className="text-right lg:text-left">
            <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider block">
              Thực nhận Shop
            </span>
            <span className="font-heading text-base sm:text-lg font-extrabold text-primary tracking-tight whitespace-nowrap block">
              {formatPrice(order.totalSellerEarnings)}
            </span>
            <span className="text-[10px] text-slate-500 block sm:hidden">
              Khách trả: {formatPrice(order.buyerTotalAmount)}
            </span>
          </div>
        </div>

        {/* Right Column (3 cols): Contextual Actions */}
        <div className="lg:col-span-3 flex flex-col sm:flex-row lg:flex-col items-stretch lg:items-end justify-center gap-2 border-t lg:border-t-0 lg:border-l border-slate-100 pt-2.5 lg:pt-0 lg:pl-5">
          {/* Primary Action Button */}
          {hasConfirm && onConfirmOrder && (
            <button
              type="button"
              onClick={() => onConfirmOrder(order)}
              className="w-full lg:w-48 min-h-[38px] py-2 px-3 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-2xs text-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-1 whitespace-nowrap"
            >
              Xác nhận đơn
            </button>
          )}

          {hasStartPreparing && onStartPreparing && (
            <button
              type="button"
              onClick={() => onStartPreparing(order)}
              className="w-full lg:w-48 min-h-[38px] py-2 px-3 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-2xs text-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-1 whitespace-nowrap"
            >
              Bắt đầu đóng gói
            </button>
          )}

          {hasReadyPickup && onReadyForPickup && (
            <button
              type="button"
              onClick={() => onReadyForPickup(order)}
              className="w-full lg:w-48 min-h-[38px] py-2 px-3 rounded-xl bg-secondary hover:bg-teal-700 text-white text-xs font-bold transition-all shadow-2xs text-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-1 whitespace-nowrap"
            >
              Đóng gói xong (Chờ lấy)
            </button>
          )}

          {/* Secondary Actions */}
          <div className="flex items-center gap-2 w-full lg:w-48">
            {hasPrintLabel && onPrintShippingLabel && (
              <button
                type="button"
                onClick={() => onPrintShippingLabel(order)}
                title="In phiếu gửi & Tem niêm phong"
                className="flex-1 min-h-[36px] py-1.5 px-2 rounded-xl bg-slate-50 hover:bg-blue-50 text-on-surface-variant hover:text-primary border border-slate-200 text-xs font-semibold flex items-center justify-center gap-1 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary whitespace-nowrap"
              >
                <Printer className="w-3.5 h-3.5 shrink-0" />
                <span>In phiếu</span>
              </button>
            )}

            <Link
              href={`/seller/orders/${order.orderId}`}
              className="flex-1 min-h-[36px] py-1.5 px-2 rounded-xl bg-slate-50 hover:bg-blue-50 text-on-surface-variant hover:text-primary border border-slate-200 text-xs font-semibold flex items-center justify-center gap-1 transition-colors text-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary whitespace-nowrap"
            >
              <span>Chi tiết</span>
              <ExternalLink className="w-3.5 h-3.5 shrink-0" />
            </Link>
          </div>

          {/* Cancel button if permitted */}
          {hasCancel && onCancelOrder && (
            <button
              type="button"
              onClick={() => onCancelOrder(order)}
              className="text-xs font-semibold text-red-700 hover:text-red-800 hover:underline self-center lg:self-end mt-1 py-1 px-1 rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-600 whitespace-nowrap"
            >
              Hủy đơn hàng
            </button>
          )}
        </div>
      </div>
    </article>
  );
});
