"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { X, AlertTriangle, Loader2 } from "lucide-react";
import { useCancelOrderMutation, useRequestCancellationMutation } from "@/features/orders/hooks/useOrderQueries";
import type { CancellationReasonCode, OrderStatus } from "@/types/enums";
import { BUYER_CANCELLATION_REASONS } from "@/lib/constants";

const cancelOrderSchema = z
  .object({
    reasonCode: z.string().min(1, "Vui lòng chọn lý do hủy đơn"),
    customReason: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.reasonCode === "OTHER" && (!data.customReason || data.customReason.trim().length < 5)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Vui lòng nhập chi tiết lý do hủy đơn (tối thiểu 5 ký tự)",
        path: ["customReason"],
      });
    }
  });

type CancelOrderFormValues = z.infer<typeof cancelOrderSchema>;

interface CancelOrderModalProps {
  orderId: string;
  orderNumber: string;
  shopName: string;
  status: OrderStatus;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (isDirect: boolean) => void;
}

export function CancelOrderModal({
  orderId,
  orderNumber,
  shopName,
  status,
  isOpen,
  onClose,
  onSuccess,
}: CancelOrderModalProps) {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const cancelOrderMutation = useCancelOrderMutation();
  const requestCancellationMutation = useRequestCancellationMutation();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<CancelOrderFormValues>({
    resolver: zodResolver(cancelOrderSchema),
    defaultValues: {
      reasonCode: "BUYER_CHANGED_MIND",
      customReason: "",
    },
  });

  const selectedReasonCode = watch("reasonCode") as CancellationReasonCode;

  useEffect(() => {
    if (isOpen) {
      reset({ reasonCode: "BUYER_CHANGED_MIND", customReason: "" });
      setErrorMessage(null);
    }
  }, [isOpen, reset]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  // Direct cancel if PENDING_PAYMENT, otherwise Request Cancellation if PROCESSING/CONFIRMED
  const isDirectCancel = status === "PENDING_PAYMENT";
  const isPending = cancelOrderMutation.isPending || requestCancellationMutation.isPending;

  const onSubmit = async (values: CancelOrderFormValues) => {
    setErrorMessage(null);

    const reasonCode = values.reasonCode as CancellationReasonCode;
    const reasonNote = reasonCode === "OTHER" ? values.customReason?.trim() : undefined;

    try {
      if (isDirectCancel) {
        await cancelOrderMutation.mutateAsync({
          orderId,
          data: {
            reasonCode,
            reasonNote,
          },
        });
      } else {
        await requestCancellationMutation.mutateAsync({
          orderId,
          data: {
            reasonCode,
            reasonNote,
          },
        });
      }
      onSuccess(isDirectCancel);
      onClose();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errObj?.response?.data?.message ||
        errObj?.message ||
        "Không thể gửi yêu cầu hủy đơn. Vui lòng thử lại."
      );
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={isDirectCancel ? "Hủy đơn hàng" : "Yêu cầu hủy đơn hàng"}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#ba1a1a]/10 text-[#ba1a1a] flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                {isDirectCancel ? "Hủy đơn hàng" : "Yêu cầu hủy đơn hàng"}
              </h3>
              <p className="text-xs text-[#737688] font-mono">
                Đơn hàng #{orderNumber} • {shopName}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="w-8 h-8 rounded-full bg-[#eff4ff] hover:bg-[#c3c5d9]/30 text-[#434656] flex items-center justify-center transition-colors cursor-pointer shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Notice */}
        <div className="p-3.5 rounded-2xl bg-[#eff4ff] text-xs text-[#434656] leading-relaxed">
          {isDirectCancel ? (
            <p>
              Đơn hàng này chưa được thanh toán. Xác nhận hủy sẽ hoàn trả tồn kho ngay lập tức và không phát sinh phí.
            </p>
          ) : (
            <p>
              Đơn hàng của bạn đang được người bán xác nhận hoặc đóng gói. Yêu cầu hủy sẽ được gửi tới Shop để xem xét theo quy định của TechHub.
            </p>
          )}
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs">
            {errorMessage}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-[#0b1c30]">
              Lý do hủy đơn <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="flex flex-col gap-2">
              {BUYER_CANCELLATION_REASONS.map((opt) => (
                <label
                  key={opt.code}
                  className={`flex items-center gap-3 p-3 rounded-2xl border text-xs cursor-pointer transition-all ${
                    selectedReasonCode === opt.code
                      ? "border-[#0045cc] bg-[#eff4ff]/60 font-semibold text-[#0045cc]"
                      : "border-[#c3c5d9]/30 hover:bg-[#eff4ff]/30 text-[#434656]"
                  }`}
                >
                  <input
                    type="radio"
                    value={opt.code}
                    {...register("reasonCode")}
                    className="accent-[#0045cc] cursor-pointer"
                  />
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>
            {errors.reasonCode && (
              <p className="text-xs text-[#ba1a1a] font-medium">{errors.reasonCode.message}</p>
            )}
          </div>

          {selectedReasonCode === "OTHER" && (
            <div className="flex flex-col gap-1.5 animate-in fade-in duration-150">
              <label htmlFor="cancel-custom-reason" className="text-xs font-semibold text-[#0b1c30]">
                Mô tả chi tiết lý do hủy <span className="text-[#ba1a1a]">*</span>
              </label>
              <textarea
                id="cancel-custom-reason"
                rows={3}
                aria-invalid={Boolean(errors.customReason)}
                aria-describedby={errors.customReason ? "cancel-custom-reason-error" : undefined}
                {...register("customReason")}
                placeholder="Thông tin chi tiết giúp chúng tôi xử lý hủy đơn nhanh hơn..."
                className="w-full p-3 rounded-2xl border border-[#c3c5d9]/40 bg-[#eff4ff]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all resize-none"
              />
              {errors.customReason && (
                <p id="cancel-custom-reason-error" className="text-xs text-[#ba1a1a] font-medium">
                  {errors.customReason.message}
                </p>
              )}
            </div>
          )}

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="px-5 py-2.5 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-xs font-semibold text-[#434656] transition-colors cursor-pointer disabled:opacity-50"
            >
              Đóng
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="px-5 py-2.5 rounded-xl bg-[#ba1a1a] hover:bg-[#93000a] text-white text-xs font-semibold shadow-sm transition-all flex items-center justify-center gap-2 active:scale-[0.99] cursor-pointer disabled:opacity-60"
            >
              {isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : isDirectCancel ? (
                "Xác nhận hủy đơn"
              ) : (
                "Gửi yêu cầu hủy"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
