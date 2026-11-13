"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  Store,
  Loader2,
  AlertCircle,
  ShieldAlert,
  ShieldCheck,
  Ban,
  BarChart3,
  MapPin,
  User,
} from "lucide-react";
import {
  useAdminShopDetailQuery,
  useUpdateAdminShopStatusMutation,
} from "../../hooks/useAdminModeration";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  statusReasonSchema,
  type StatusReasonFormValues,
} from "../../schemas/moderationSchema";
import { formatPrice, getThumbnailUrl } from "@/lib/utils";
import type { ShopStatus } from "@/types/enums";

interface ShopDetailDrawerProps {
  shopId: string | null;
  onClose: () => void;
  onActionSuccess?: (message: string) => void;
}

export function ShopDetailDrawer({
  shopId,
  onClose,
  onActionSuccess,
}: ShopDetailDrawerProps) {
  const {
    data: shopDetail,
    isLoading,
    isError,
    refetch,
  } = useAdminShopDetailQuery(shopId);
  const updateStatusMutation = useUpdateAdminShopStatusMutation();

  const [targetAction, setTargetAction] = useState<ShopStatus | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleClose = useCallback(() => {
    setTargetAction(null);
    setActionError(null);
    onClose();
  }, [onClose]);

  useAdminDialogA11y(Boolean(shopId), handleClose);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StatusReasonFormValues>({
    resolver: zodResolver(statusReasonSchema),
    defaultValues: { reason: "" },
  });

  useEffect(() => {
    if (shopId) {
      reset({ reason: "" });
    }
  }, [shopId, reset]);

  if (!shopId) return null;

  const handleStatusSubmit = async (values: StatusReasonFormValues) => {
    if (!targetAction || !shopDetail) return;
    setActionError(null);
    try {
      await updateStatusMutation.mutateAsync({
        id: shopId,
        data: {
          status: targetAction,
          reason: values.reason.trim(),
        },
      });
      onActionSuccess?.(
        `Đã chuyển trạng thái gian hàng "${shopDetail.shop.name}" sang ${targetAction}.`
      );
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật trạng thái gian hàng thất bại.";
      setActionError(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="shop-detail-drawer-title"
      className="fixed inset-0 z-50 flex justify-end"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={handleClose}
      />

      <div className="relative z-10 w-full max-w-xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-200 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70 shrink-0">
          <div className="min-w-0">
            <h2
              id="shop-detail-drawer-title"
              className="font-heading text-base font-extrabold text-on-surface truncate"
            >
              Chi tiết Gian hàng &amp; Chế tài
            </h2>
            <p className="text-[11px] text-slate-600 font-mono truncate">
              {shopDetail?.seller.sellerName
                ? `@${shopDetail.seller.sellerName}`
                : shopId}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng chi tiết gian hàng"
            onClick={handleClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 flex flex-col gap-5">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
              <span className="text-xs text-slate-500">
                Đang tải thông tin gian hàng...
              </span>
            </div>
          ) : isError || !shopDetail ? (
            <div className="p-8 text-center flex flex-col items-center gap-3">
              <AlertCircle className="w-8 h-8 text-rose-500" />
              <p className="text-xs font-bold text-slate-800">
                Không thể tải chi tiết gian hàng
              </p>
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

              {/* Shop Header Card */}
              <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50/70 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-12 h-12 rounded-xl bg-white border border-slate-200 flex items-center justify-center overflow-hidden shrink-0">
                    {shopDetail.shop.avatarUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={getThumbnailUrl(shopDetail.shop.avatarUrl, 120)}
                        alt={shopDetail.shop.name}
                        width={48}
                        height={48}
                        decoding="async"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <Store className="w-5 h-5 text-[#0045cc]" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <h3 className="font-bold text-sm text-slate-900 truncate">
                      {shopDetail.shop.name}
                    </h3>
                    <p className="text-[11px] font-mono text-slate-500">
                      Số dư ví: {formatPrice(shopDetail.seller.balance)}
                    </p>
                  </div>
                </div>
                <span
                  className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase shrink-0 ${
                    shopDetail.shop.status === "ACTIVE"
                      ? "bg-emerald-100 text-emerald-700"
                      : shopDetail.shop.status === "PAUSED"
                      ? "bg-amber-100 text-amber-800"
                      : shopDetail.shop.status === "SUSPENDED"
                      ? "bg-orange-100 text-orange-800"
                      : "bg-rose-600 text-white"
                  }`}
                >
                  {shopDetail.shop.status}
                </span>
              </div>

              {/* Metrics Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                <div className="p-3 rounded-xl border border-slate-200 bg-white text-center">
                  <p className="text-[10px] text-slate-500 font-semibold">
                    Tổng SP
                  </p>
                  <p className="font-heading text-base font-extrabold text-slate-900 font-mono">
                    {shopDetail.metrics.totalProducts}
                  </p>
                </div>
                <div className="p-3 rounded-xl border border-slate-200 bg-white text-center">
                  <p className="text-[10px] text-slate-500 font-semibold">
                    Đang mở bán
                  </p>
                  <p className="font-heading text-base font-extrabold text-emerald-700 font-mono">
                    {shopDetail.metrics.activeProducts}
                  </p>
                </div>
                <div className="p-3 rounded-xl border border-slate-200 bg-white text-center">
                  <p className="text-[10px] text-slate-500 font-semibold">
                    Đơn hoàn tất
                  </p>
                  <p className="font-heading text-base font-extrabold text-[#0045cc] font-mono">
                    {shopDetail.metrics.totalCompletedOrders}
                  </p>
                </div>
                <div className="p-3 rounded-xl border border-slate-200 bg-white text-center">
                  <p className="text-[10px] text-slate-500 font-semibold">
                    Doanh thu
                  </p>
                  <p className="font-heading text-xs font-extrabold text-slate-900 font-mono mt-1">
                    {formatPrice(shopDetail.metrics.cumulativeRevenue)}
                  </p>
                </div>
              </div>

              {/* Owner & Warehouse */}
              <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-2 text-xs">
                <span className="font-bold text-slate-800 flex items-center gap-1.5 border-b border-slate-100 pb-2">
                  <User className="w-4 h-4 text-[#0045cc]" />
                  Chủ sở hữu & Liên hệ
                </span>
                <div className="flex justify-between">
                  <span className="text-slate-500">Chủ shop:</span>
                  <span className="font-semibold text-slate-900">
                    {shopDetail.owner.fullName}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Email:</span>
                  <span className="font-mono text-slate-800">
                    {shopDetail.owner.email}
                  </span>
                </div>
                {shopDetail.warehouseAddress && (
                  <div className="pt-2 border-t border-slate-100 mt-1 flex items-start gap-2 text-[11px] text-slate-600">
                    <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                    <span>{shopDetail.warehouseAddress.fullAddress}</span>
                  </div>
                )}
              </div>

              {/* Reason form for status action */}
              {targetAction && (
                <form
                  onSubmit={handleSubmit(handleStatusSubmit)}
                  className="bg-amber-50/70 border border-amber-200 rounded-2xl p-4 flex flex-col gap-3 animate-in fade-in"
                >
                  <div className="flex items-center gap-2 text-xs font-bold text-amber-900">
                    <BarChart3 className="w-4 h-4" />
                    <span>
                      Xác nhận chuyển trạng thái sang{" "}
                      <strong>{targetAction}</strong>
                    </span>
                  </div>
                  <textarea
                    rows={3}
                    {...register("reason")}
                    placeholder="Nhập lý do áp dụng / gỡ chế tài gian hàng..."
                    className="w-full p-3 rounded-xl border border-amber-300 bg-white text-xs text-slate-800 focus:outline-none"
                  />
                  {errors.reason && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.reason.message}
                    </p>
                  )}

                  <div className="flex items-center justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setTargetAction(null)}
                      className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-white cursor-pointer"
                    >
                      Hủy
                    </button>
                    <button
                      type="submit"
                      disabled={updateStatusMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-[#0b1c30] hover:bg-slate-800 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                    >
                      {updateStatusMutation.isPending && (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      )}
                      <span>Xác nhận Thay đổi</span>
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {shopDetail && !targetAction && (
          <div className="px-5 py-4 border-t border-slate-200 bg-slate-50 flex flex-wrap items-center justify-end gap-2 shrink-0">
            {shopDetail.shop.status !== "ACTIVE" &&
              shopDetail.shop.status !== "BANNED" && (
                <button
                  type="button"
                  onClick={() => setTargetAction("ACTIVE")}
                  className="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>Khôi phục ACTIVE</span>
                </button>
              )}

            {shopDetail.shop.status !== "SUSPENDED" &&
              shopDetail.shop.status !== "BANNED" && (
                <button
                  type="button"
                  onClick={() => setTargetAction("SUSPENDED")}
                  className="px-3.5 py-2 rounded-xl border border-amber-400 bg-amber-50 hover:bg-amber-100 text-amber-800 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                >
                  <ShieldAlert className="w-4 h-4" />
                  <span>Tạm đình chỉ (SUSPEND)</span>
                </button>
              )}

            {shopDetail.shop.status !== "BANNED" && (
              <button
                type="button"
                onClick={() => setTargetAction("BANNED")}
                className="px-3.5 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
              >
                <Ban className="w-4 h-4" />
                <span>Cấm vĩnh viễn (BAN)</span>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
