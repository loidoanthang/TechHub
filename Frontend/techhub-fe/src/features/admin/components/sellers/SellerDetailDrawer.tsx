"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  CheckCircle2,
  XCircle,
  Loader2,
  AlertCircle,
  Store,
  User,
  MapPin,
  Landmark,
  Truck,
} from "lucide-react";
import {
  useAdminSellerDetailQuery,
  useUpdateSellerApprovalMutation,
} from "../../hooks/useAdminModeration";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  rejectionReasonSchema,
  type RejectionReasonFormValues,
} from "../../schemas/moderationSchema";
import { formatPrice } from "@/lib/utils";

interface SellerDetailDrawerProps {
  sellerId: string | null;
  onClose: () => void;
  onActionSuccess?: (message: string) => void;
}

const SELLER_REJECT_PRESETS = [
  "Thông tin tài khoản ngân hàng không hợp lệ hoặc không trùng khớp chủ sở hữu",
  "Địa chỉ kho hàng không rõ ràng / thiếu thông tin liên hệ xác thực",
  "Tên gian hàng hoặc mô tả chứa nội dung vi phạm bản quyền thương hiệu",
];

export function SellerDetailDrawer({
  sellerId,
  onClose,
  onActionSuccess,
}: SellerDetailDrawerProps) {
  const {
    data: seller,
    isLoading,
    isError,
    refetch,
  } = useAdminSellerDetailQuery(sellerId);
  const approvalMutation = useUpdateSellerApprovalMutation();

  const [showRejectBox, setShowRejectBox] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleClose = useCallback(() => {
    setShowRejectBox(false);
    setActionError(null);
    onClose();
  }, [onClose]);

  useAdminDialogA11y(Boolean(sellerId), handleClose);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<RejectionReasonFormValues>({
    resolver: zodResolver(rejectionReasonSchema),
    defaultValues: { reason: "" },
  });

  useEffect(() => {
    if (sellerId) {
      reset({ reason: "" });
    }
  }, [sellerId, reset]);

  if (!sellerId) return null;

  const handleApprove = async () => {
    setActionError(null);
    try {
      await approvalMutation.mutateAsync({
        id: sellerId,
        data: { status: "APPROVED" },
      });
      onActionSuccess?.(
        `Đã phê duyệt đối tác "${seller?.shop?.name || seller?.sellerName}" và kích hoạt Shop!`
      );
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Phê duyệt hồ sơ thất bại.";
      setActionError(msg);
    }
  };

  const handleRejectSubmit = async (values: RejectionReasonFormValues) => {
    setActionError(null);
    try {
      await approvalMutation.mutateAsync({
        id: sellerId,
        data: {
          status: "REJECTED",
          rejectionReason: values.reason.trim(),
        },
      });
      onActionSuccess?.(`Đã từ chối hồ sơ đăng ký "${seller?.sellerName}".`);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Từ chối hồ sơ thất bại.";
      setActionError(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="seller-detail-drawer-title"
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
              id="seller-detail-drawer-title"
              className="font-heading text-base font-extrabold text-on-surface truncate"
            >
              Hồ sơ Thẩm định Đối tác Seller
            </h2>
            <p className="text-[11px] text-slate-600 font-mono truncate">
              @{seller?.sellerName || "..."}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng hồ sơ đối tác"
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
                Đang tải hồ sơ đối tác...
              </span>
            </div>
          ) : isError || !seller ? (
            <div className="p-8 text-center flex flex-col items-center gap-3">
              <AlertCircle className="w-8 h-8 text-rose-500" />
              <p className="text-xs font-bold text-slate-800">
                Không thể tải thông tin hồ sơ
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

              {/* Verification Status */}
              <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 border border-slate-200">
                <span className="text-xs font-bold text-slate-700">
                  Trạng thái Xét duyệt
                </span>
                <span
                  className={`px-2.5 py-1 rounded-full text-[11px] font-extrabold ${
                    seller.verificationStatus === "APPROVED"
                      ? "bg-emerald-100 text-emerald-700"
                      : seller.verificationStatus === "PENDING"
                      ? "bg-amber-100 text-amber-800"
                      : "bg-rose-100 text-rose-700"
                  }`}
                >
                  {seller.verificationStatus}
                </span>
              </div>

              {seller.rejectionReason && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800">
                  <p className="font-bold mb-1">Lý do từ chối trước đó:</p>
                  <p>{seller.rejectionReason}</p>
                </div>
              )}

              {/* 1. Shop Info */}
              <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-2.5">
                <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] border-b border-slate-100 pb-2">
                  <Store className="w-4 h-4 text-[#0045cc]" />
                  <span>Thông tin Gian hàng</span>
                </div>
                <div className="grid grid-cols-1 gap-1.5 text-xs">
                  <div className="flex justify-between gap-2">
                    <span className="text-slate-500">Tên Shop:</span>
                    <span className="font-bold text-slate-900 text-right">
                      {seller.shop?.name || "—"}
                    </span>
                  </div>
                  <div className="flex justify-between gap-2">
                    <span className="text-slate-500">Slug định danh:</span>
                    <span className="font-mono font-semibold text-[#0045cc]">
                      /shops/{seller.sellerName}
                    </span>
                  </div>
                  {seller.shop?.description && (
                    <p className="text-[11px] text-slate-600 bg-slate-50 p-2.5 rounded-xl mt-1">
                      {seller.shop.description}
                    </p>
                  )}
                </div>
              </div>

              {/* 2. Owner Info */}
              <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-2.5">
                <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] border-b border-slate-100 pb-2">
                  <User className="w-4 h-4 text-[#0045cc]" />
                  <span>Tài khoản Chủ sở hữu</span>
                </div>
                <div className="grid grid-cols-1 gap-1.5 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Họ tên:</span>
                    <span className="font-semibold text-slate-900">
                      {seller.user.fullName}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Email:</span>
                    <span className="font-mono text-slate-800">
                      {seller.user.email}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Điện thoại:</span>
                    <span className="font-mono text-slate-800">
                      {seller.user.phone || "Chưa có"}
                    </span>
                  </div>
                </div>
              </div>

              {/* 3. Warehouse & Shipping */}
              {seller.warehouseAddress && (
                <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-2.5">
                  <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] border-b border-slate-100 pb-2">
                    <MapPin className="w-4 h-4 text-[#0045cc]" />
                    <span>Kho hàng & Vận chuyển</span>
                  </div>
                  <div className="text-xs flex flex-col gap-1.5">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Người liên hệ:</span>
                      <span className="font-semibold text-slate-900">
                        {seller.warehouseAddress.contactName} (
                        {seller.warehouseAddress.phone})
                      </span>
                    </div>
                    <p className="text-slate-600 bg-slate-50 p-2.5 rounded-xl">
                      {seller.warehouseAddress.fullAddress}
                    </p>
                    {seller.shippingOption && (
                      <div className="flex items-center justify-between pt-1">
                        <span className="text-slate-500 flex items-center gap-1">
                          <Truck className="w-3.5 h-3.5" /> Phí vận chuyển:
                        </span>
                        <span className="font-mono font-bold text-slate-900">
                          {formatPrice(seller.shippingOption.shippingFee)}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* 4. Bank Account */}
              <div className="p-4 rounded-2xl border border-slate-200/80 flex flex-col gap-2.5">
                <div className="flex items-center gap-2 text-xs font-bold text-[#0b1c30] border-b border-slate-100 pb-2">
                  <Landmark className="w-4 h-4 text-[#0045cc]" />
                  <span>Tài khoản Ngân hàng Quyết toán</span>
                </div>
                <div className="grid grid-cols-1 gap-1.5 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Ngân hàng:</span>
                    <span className="font-bold text-slate-900">
                      {seller.bankName}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Số tài khoản:</span>
                    <span className="font-mono font-bold text-[#0045cc]">
                      {seller.bankAccountNumber}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Chủ tài khoản:</span>
                    <span className="font-bold uppercase text-slate-900">
                      {seller.bankAccountHolder}
                    </span>
                  </div>
                </div>
              </div>

              {/* Reject Box */}
              {showRejectBox && (
                <form
                  onSubmit={handleSubmit(handleRejectSubmit)}
                  className="bg-rose-50/70 border border-rose-200 rounded-2xl p-4 flex flex-col gap-3 animate-in fade-in"
                >
                  <label className="text-xs font-bold text-rose-900">
                    Lý do từ chối hồ sơ đăng ký Seller *
                  </label>
                  <textarea
                    rows={3}
                    {...register("reason")}
                    placeholder="Nhập rõ lý do để người đăng ký chỉnh sửa và nộp lại hồ sơ..."
                    className="w-full p-3 rounded-xl border border-rose-300 bg-white text-xs text-slate-800 focus:outline-none"
                  />
                  {errors.reason && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.reason.message}
                    </p>
                  )}

                  <div className="flex flex-wrap gap-1.5">
                    {SELLER_REJECT_PRESETS.map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() =>
                          setValue("reason", preset, { shouldValidate: true })
                        }
                        className="text-[10px] px-2.5 py-1 rounded-lg bg-white border border-rose-200 text-rose-700 hover:bg-rose-100 text-left cursor-pointer"
                      >
                        + {preset}
                      </button>
                    ))}
                  </div>

                  <div className="flex items-center justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setShowRejectBox(false)}
                      className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-white cursor-pointer"
                    >
                      Hủy
                    </button>
                    <button
                      type="submit"
                      disabled={approvalMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                    >
                      {approvalMutation.isPending && (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      )}
                      <span>Xác nhận Từ chối</span>
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {seller && seller.verificationStatus === "PENDING" && (
          <div className="px-5 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end gap-2 shrink-0">
            {!showRejectBox && (
              <button
                type="button"
                onClick={() => setShowRejectBox(true)}
                className="px-4 py-2 rounded-xl border border-rose-300 bg-white hover:bg-rose-50 text-rose-600 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
              >
                <XCircle className="w-4 h-4" />
                <span>Từ chối hồ sơ</span>
              </button>
            )}
            <button
              type="button"
              onClick={handleApprove}
              disabled={approvalMutation.isPending}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1.5 shadow-xs cursor-pointer"
            >
              {approvalMutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <CheckCircle2 className="w-4 h-4" />
              )}
              <span>Duyệt & Kích hoạt Shop</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
