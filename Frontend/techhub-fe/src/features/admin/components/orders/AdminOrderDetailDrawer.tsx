"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  ShoppingBag,
  Loader2,
  AlertCircle,
  Store,
  User,
  MapPin,
  CreditCard,
  XCircle,
  Package,
} from "lucide-react";
import {
  useAdminOrderDetailQuery,
  useCancelAdminOrderMutation,
} from "../../hooks/useAdminOperations";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  adminCancelOrderSchema,
  type AdminCancelOrderFormValues,
} from "../../schemas/moderationSchema";
import { formatPrice, getThumbnailUrl } from "@/lib/utils";
import { ADMIN_CANCELLATION_REASONS } from "@/lib/constants";
import { SelectDropdown } from "@/components/shared/SelectDropdown";

interface AdminOrderDetailDrawerProps {
  orderId: string | null;
  onClose: () => void;
  onActionSuccess?: (msg: string) => void;
}

export function AdminOrderDetailDrawer({
  orderId,
  onClose,
  onActionSuccess,
}: AdminOrderDetailDrawerProps) {
  const { data: order, isLoading, isError, refetch } =
    useAdminOrderDetailQuery(orderId);
  const cancelMutation = useCancelAdminOrderMutation();

  const [showCancelForm, setShowCancelForm] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleClose = useCallback(() => {
    setShowCancelForm(false);
    setActionError(null);
    onClose();
  }, [onClose]);

  useAdminDialogA11y(Boolean(orderId), handleClose);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<AdminCancelOrderFormValues>({
    resolver: zodResolver(adminCancelOrderSchema),
    defaultValues: {
      reasonCode: "SELLER_FORCE_MAJEURE",
      reasonNote: "",
    },
  });

  useEffect(() => {
    if (orderId) {
      reset({
        reasonCode: "SELLER_FORCE_MAJEURE",
        reasonNote: "",
      });
    }
  }, [orderId, reset]);

  if (!orderId) return null;

  const canCancel =
    order &&
    (order.availableActions?.length
      ? order.availableActions.includes("FORCE_CANCEL")
      : [
          "PENDING_PAYMENT",
          "PENDING_CONFIRMATION",
          "CONFIRMED",
          "PREPARING",
          "WAITING_FOR_PICKUP",
        ].includes(order.status));

  const handleCancelSubmit = async (values: AdminCancelOrderFormValues) => {
    if (!order) return;
    setActionError(null);
    try {
      await cancelMutation.mutateAsync({
        id: orderId,
        data: {
          reasonCode: values.reasonCode,
          reasonNote: values.reasonNote?.trim() || undefined,
        },
      });
      onActionSuccess?.(
        `Đã hủy đơn hàng #${order.orderNumber} theo quyết định quản trị.`
      );
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Hủy đơn hàng thất bại.";
      setActionError(msg);
    }
  };

  const itemsList = order?.items || [];

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="admin-order-drawer-title"
      className="fixed inset-0 z-50 flex justify-end overflow-hidden"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={handleClose}
      />

      <div className="relative z-10 w-full max-w-2xl bg-white h-screen max-h-screen shadow-2xl flex flex-col animate-in slide-in-from-right duration-200 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-xl bg-surface-container-low text-primary flex items-center justify-center shrink-0">
              <ShoppingBag className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <h2
                id="admin-order-drawer-title"
                className="font-heading text-base font-extrabold text-on-surface truncate"
              >
                Chi tiết Đơn hàng #{order?.orderNumber || "..."}
              </h2>
              <p className="text-[11px] text-slate-600">
                {order?.statusDescription || "Đang tải..."}
              </p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng chi tiết đơn hàng"
            onClick={handleClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body (scrollable container) */}
        <div
          tabIndex={0}
          className="flex-1 overflow-y-auto overscroll-contain p-5 sm:p-6 flex flex-col gap-5 min-h-0 focus:outline-none"
        >
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
              <span className="text-xs text-slate-500">
                Đang tải chi tiết đơn hàng...
              </span>
            </div>
          ) : isError || !order ? (
            <div className="p-8 text-center flex flex-col items-center gap-3">
              <AlertCircle className="w-8 h-8 text-rose-500" />
              <button
                type="button"
                onClick={() => refetch()}
                className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
              >
                Thử lại
              </button>
            </div>
          ) : (
            <>
              {actionError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{actionError}</span>
                </div>
              )}

              {/* Shop & Buyer */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 shrink-0">
                <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-1.5 text-xs">
                  <span className="font-bold text-[#0b1c30] flex items-center gap-1.5 pb-1 border-b border-slate-100">
                    <Store className="w-3.5 h-3.5 text-[#0045cc]" />
                    Gian hàng Bán
                  </span>
                  <span className="font-bold text-slate-900">
                    {order.shop?.shopName || "Không xác định"}
                  </span>
                  <span className="text-[11px] font-mono text-slate-500">
                    {order.shop?.sellerEmail || "Chưa có email"}
                  </span>
                </div>

                <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-1.5 text-xs">
                  <span className="font-bold text-[#0b1c30] flex items-center gap-1.5 pb-1 border-b border-slate-100">
                    <User className="w-3.5 h-3.5 text-[#0045cc]" />
                    Khách hàng Mua
                  </span>
                  <span className="font-bold text-slate-900">
                    {order.buyer?.buyerName || "Khách mua"}
                  </span>
                  <span className="text-[11px] font-mono text-slate-500">
                    {order.buyer?.buyerEmail || "Chưa có email"}
                  </span>
                </div>
              </div>

              {/* Delivery & Payment */}
              <div className="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/70 flex flex-col gap-2 text-xs shrink-0">
                <div className="flex items-start gap-2">
                  <MapPin className="w-4 h-4 text-[#0045cc] shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-slate-800">
                      {order.deliveryAddress?.recipientName ||
                        order.buyer?.recipientName ||
                        "Người nhận"}{" "}
                      •{" "}
                      {order.deliveryAddress?.phone ||
                        order.buyer?.recipientPhone ||
                        "Chưa có số ĐT"}
                    </span>
                    <p className="text-slate-600 mt-0.5">
                      {order.deliveryAddress?.fullAddress ||
                        order.buyer?.deliveryAddressSummary ||
                        "Chưa có địa chỉ chi tiết"}
                    </p>
                  </div>
                </div>
                {order.paymentInfo && (
                  <div className="flex items-center justify-between pt-2 border-t border-slate-200/60">
                    <span className="flex items-center gap-1.5 text-slate-600">
                      <CreditCard className="w-3.5 h-3.5" />
                      {order.paymentInfo.paymentMethodName.includes("VNPAY")
                        ? "Cổng thanh toán VNPAY"
                        : order.paymentInfo.paymentMethodName}
                    </span>
                    <span className="font-bold text-[#0045cc]">
                      {order.paymentInfo.paymentStatusDescription}
                    </span>
                  </div>
                )}
              </div>

              {/* Items in Order */}
              <div className="border border-slate-200 rounded-2xl bg-white shadow-2xs shrink-0 flex flex-col">
                <div className="bg-slate-50/90 px-4 py-2.5 border-b border-slate-200 flex items-center justify-between text-xs font-bold text-slate-700 rounded-t-2xl">
                  <span className="flex items-center gap-1.5">
                    <Package className="w-4 h-4 text-primary" />
                    Sản phẩm trong đơn ({order.totalItemsCount || itemsList.length})
                  </span>
                  {itemsList.length > 0 && (
                    <span className="text-[11px] font-normal text-slate-500">
                      {itemsList.length} mặt hàng
                    </span>
                  )}
                </div>

                {itemsList.length === 0 ? (
                  <div className="p-6 text-center text-xs text-slate-500">
                    Không có thông tin chi tiết sản phẩm.
                  </div>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {itemsList.map((item, idx) => (
                      <div
                        key={item.orderItemId || `item-${idx}`}
                        className="p-4 flex items-center justify-between gap-3 text-xs hover:bg-slate-50/50 transition-colors"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center text-slate-400">
                            {item.imageUrl ? (
                              // eslint-disable-next-line @next/next/no-img-element
                              <img
                                src={getThumbnailUrl(item.imageUrl, 120)}
                                alt={item.productName || "Sản phẩm"}
                                width={48}
                                height={48}
                                decoding="async"
                                className="w-full h-full object-cover"
                                onError={(e) => {
                                  (e.target as HTMLElement).style.display = "none";
                                }}
                              />
                            ) : (
                              <Package className="w-5 h-5 text-slate-300" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-slate-900 truncate" title={item.productName}>
                              {item.productName || "Sản phẩm không có tên"}
                            </p>
                            <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-slate-500 mt-1">
                              {item.variantName && (
                                <span className="text-slate-700 font-medium">
                                  {item.variantName}
                                </span>
                              )}
                              {item.sku && <span>SKU: {item.sku}</span>}
                              <span className="inline-flex items-center px-1.5 py-0.2 bg-slate-100 text-slate-800 rounded font-semibold text-[10px]">
                                SL: x{item.quantity}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-500 mt-1">
                              Đơn giá: {formatPrice(item.discountedUnitPrice || item.originalPrice || 0)}
                            </div>
                          </div>
                        </div>
                        <div className="text-right shrink-0">
                          <span className="font-mono font-bold text-slate-900 block">
                            {formatPrice(item.finalSubtotal || 0)}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Financial Breakdown */}
              <div className="p-4 rounded-2xl border border-slate-200 bg-white flex flex-col gap-2 text-xs shrink-0">
                <span className="font-bold text-slate-800 border-b border-slate-100 pb-2">
                  Chi tiết Tài chính &amp; Đối soát
                </span>
                <div className="flex justify-between text-slate-600">
                  <span>Tổng tiền hàng:</span>
                  <span className="font-mono">
                    {formatPrice(order.totalProductAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Phí vận chuyển:</span>
                  <span className="font-mono">
                    {formatPrice(order.shippingFee)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Giảm giá Shop Voucher:</span>
                  <span className="font-mono text-rose-600">
                    -{formatPrice(order.shopDiscountAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Giảm giá Sàn (Platform Voucher):</span>
                  <span className="font-mono text-rose-600">
                    -{formatPrice(order.platformDiscountAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-[#0045cc] font-bold pt-1 border-t border-slate-100">
                  <span>Khách thanh toán (Buyer Total):</span>
                  <span className="font-mono text-sm">
                    {formatPrice(order.buyerTotalAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-emerald-700 font-semibold">
                  <span>Hoa hồng Sàn thu (Commission):</span>
                  <span className="font-mono">
                    {formatPrice(order.totalCommission)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-800 font-bold">
                  <span>Thực nhận về Ví Seller:</span>
                  <span className="font-mono">
                    {formatPrice(order.totalSellerEarnings)}
                  </span>
                </div>
              </div>

              {/* Cancel Order Form */}
              {showCancelForm && (
                <form
                  onSubmit={handleSubmit(handleCancelSubmit)}
                  className="bg-rose-50/70 border border-rose-200 rounded-2xl p-4 flex flex-col gap-3 shrink-0"
                >
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-rose-900">
                      Lý do Quản trị viên hủy đơn (Bắt buộc) *
                    </label>
                    <SelectDropdown
                      value={watch("reasonCode")}
                      ariaLabel="Lý do Quản trị viên hủy đơn"
                      onChange={(val) =>
                        setValue(
                          "reasonCode",
                          val as AdminCancelOrderFormValues["reasonCode"],
                          { shouldValidate: true }
                        )
                      }
                      buttonClassName="h-10 px-3 rounded-xl border border-rose-300 bg-white text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500 font-medium"
                      options={ADMIN_CANCELLATION_REASONS.map((reason) => ({
                        value: reason.code,
                        label: reason.label,
                      }))}
                    />
                    {errors.reasonCode && (
                      <p className="text-[11px] text-rose-600 font-medium">
                        {errors.reasonCode.message}
                      </p>
                    )}
                  </div>

                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-rose-900">
                      Ghi chú / Căn cứ phân xử chi tiết
                    </label>
                    <textarea
                      rows={3}
                      {...register("reasonNote")}
                      placeholder="VD: Cửa hàng xác nhận quá hạn không giao hàng; Giải quyết khiếu nại hoàn tiền cho người mua..."
                      className="w-full p-3 rounded-xl border border-rose-300 bg-white text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
                    />
                    {errors.reasonNote && (
                      <p className="text-[11px] text-rose-600 font-medium">
                        {errors.reasonNote.message}
                      </p>
                    )}
                  </div>

                  <div className="flex justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setShowCancelForm(false)}
                      className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 cursor-pointer hover:bg-slate-200/50"
                    >
                      Đóng
                    </button>
                    <button
                      type="submit"
                      disabled={cancelMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer disabled:opacity-60"
                    >
                      {cancelMutation.isPending && (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      )}
                      <span>Xác nhận Hủy Đơn</span>
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {canCancel && !showCancelForm && (
          <div className="px-5 py-4 border-t border-slate-200 bg-slate-50 flex justify-end shrink-0">
            <button
              type="button"
              onClick={() => setShowCancelForm(true)}
              className="px-4 py-2 rounded-xl border border-rose-300 bg-white hover:bg-rose-50 text-rose-600 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
            >
              <XCircle className="w-4 h-4" />
              <span>Hủy đơn Tranh chấp (Admin)</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
