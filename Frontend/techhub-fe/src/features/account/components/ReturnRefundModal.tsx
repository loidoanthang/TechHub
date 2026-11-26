"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { X, RotateCcw, AlertCircle, Loader2 } from "lucide-react";
import { useCreateReturnRequestMutation } from "@/features/orders/hooks/useOrderQueries";
import { GalleryImageUploader } from "@/components/shared/upload/GalleryImageUploader";
import { uploadApi } from "@/lib/api/upload";
import { UPLOAD_LIMITS } from "@/features/uploads/types";

interface ReturnRefundModalProps {
  orderId: string;
  orderNumber: string;
  shopName: string;
  totalAmount: number;
  paymentMethod?: string;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const RETURN_REASONS = [
  { code: "DEFECTIVE", label: "Sản phẩm lỗi kỹ thuật / Không lên nguồn hoặc không hoạt động" },
  { code: "DAMAGED", label: "Sản phẩm bị hư hỏng, móp méo hoặc vỡ khi vận chuyển" },
  { code: "WRONG_ITEM", label: "Giao sai sản phẩm / Sai phân loại đã đặt" },
  { code: "MISSING_PARTS", label: "Thiếu phụ kiện hoặc linh kiện đi kèm" },
  { code: "NOT_AS_DESCRIBED", label: "Sản phẩm khác biệt rõ rệt so với mô tả / thông số" },
];

const createReturnSchema = (isCod: boolean) =>
  z
    .object({
      reasonCode: z.string().min(1, "Vui lòng chọn lý do trả hàng"),
      description: z
        .string()
        .trim()
        .min(10, "Vui lòng mô tả chi tiết vấn đề (tối thiểu 10 ký tự)"),
      bankName: z.string().optional(),
      bankAccountNumber: z.string().optional(),
      bankAccountHolder: z.string().optional(),
    })
    .superRefine((data, ctx) => {
      if (isCod) {
        if (!data.bankName?.trim()) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Vui lòng nhập tên ngân hàng",
            path: ["bankName"],
          });
        }
        if (!data.bankAccountNumber?.trim()) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Vui lòng nhập số tài khoản",
            path: ["bankAccountNumber"],
          });
        }
        if (!data.bankAccountHolder?.trim()) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Vui lòng nhập tên chủ tài khoản",
            path: ["bankAccountHolder"],
          });
        }
      }
    });

type ReturnRefundFormValues = {
  reasonCode: string;
  description: string;
  bankName?: string;
  bankAccountNumber?: string;
  bankAccountHolder?: string;
};

export function ReturnRefundModal({
  orderId,
  orderNumber,
  shopName,
  totalAmount,
  paymentMethod,
  isOpen,
  onClose,
  onSuccess,
}: ReturnRefundModalProps) {
  const [evidenceImages, setEvidenceImages] = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const isCod = paymentMethod === "COD";
  const createReturnRequestMutation = useCreateReturnRequestMutation();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<ReturnRefundFormValues>({
    resolver: zodResolver(createReturnSchema(isCod)),
    defaultValues: {
      reasonCode: RETURN_REASONS[0].code,
      description: "",
      bankName: "",
      bankAccountNumber: "",
      bankAccountHolder: "",
    },
  });

  const selectedReasonCode = watch("reasonCode");

  useEffect(() => {
    if (isOpen) {
      reset({
        reasonCode: RETURN_REASONS[0].code,
        description: "",
        bankName: "",
        bankAccountNumber: "",
        bankAccountHolder: "",
      });
      setEvidenceImages([]);
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

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const onSubmit = async (values: ReturnRefundFormValues) => {
    setErrorMessage(null);

    const reasonItem = RETURN_REASONS.find((r) => r.code === values.reasonCode);
    const reasonText = `${reasonItem?.label || values.reasonCode}: ${values.description.trim()}`;

    try {
      await createReturnRequestMutation.mutateAsync({
        orderId,
        data: {
          reason: reasonText,
          proofImages: evidenceImages.length > 0 ? evidenceImages.join(",") : undefined,
          bankName: isCod ? values.bankName?.trim() : undefined,
          bankAccountNumber: isCod ? values.bankAccountNumber?.replace(/\s+/g, "").trim() : undefined,
          bankAccountHolder: isCod ? values.bankAccountHolder?.trim().toUpperCase() : undefined,
        },
      });
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errObj?.response?.data?.message ||
        errObj?.message ||
        "Không thể gửi yêu cầu trả hàng/hoàn tiền. Vui lòng thử lại."
      );
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Yêu cầu Trả hàng & Hoàn tiền"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center shrink-0">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                Yêu cầu Trả hàng &amp; Hoàn tiền
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

        {/* Refund Estimated Banner */}
        <div className="p-3.5 rounded-2xl bg-[#0045cc]/5 border border-[#0045cc]/15 flex items-center justify-between gap-2">
          <span className="text-xs text-[#0b1c30] font-medium">Số tiền hoàn dự kiến:</span>
          <span className="text-sm font-bold text-[#0045cc] font-mono">{formatPrice(totalAmount)}</span>
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#0b1c30]">
              Lý do trả hàng <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="flex flex-col gap-2">
              {RETURN_REASONS.map((r) => (
                <label
                  key={r.code}
                  className={`flex items-start gap-2.5 p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                    selectedReasonCode === r.code
                      ? "border-[#0045cc] bg-[#eff4ff]/60 font-semibold text-[#0045cc]"
                      : "border-[#c3c5d9]/30 hover:bg-[#eff4ff]/30 text-[#434656]"
                  }`}
                >
                  <input
                    type="radio"
                    value={r.code}
                    {...register("reasonCode")}
                    className="accent-[#0045cc] mt-0.5 cursor-pointer"
                  />
                  <span>{r.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="return-description" className="text-xs font-bold text-[#0b1c30]">
              Mô tả chi tiết tình trạng <span className="text-[#ba1a1a]">*</span>
            </label>
            <textarea
              id="return-description"
              rows={3}
              aria-invalid={Boolean(errors.description)}
              aria-describedby={errors.description ? "return-description-error" : undefined}
              {...register("description")}
              placeholder="Mô tả chi tiết lỗi sản phẩm, tình trạng đóng gói hoặc vấn đề gặp phải..."
              className="w-full p-3 rounded-2xl border border-[#c3c5d9]/40 bg-[#eff4ff]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all resize-none"
            />
            {errors.description && (
              <p id="return-description-error" className="text-xs text-[#ba1a1a] font-medium">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Evidence Attachments */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-[#0b1c30]">
                Hình ảnh / Bằng chứng (Tối đa 3 ảnh)
              </label>
              <span className="text-[11px] text-[#737688] font-mono">
                {evidenceImages.length}/3
              </span>
            </div>

            <GalleryImageUploader
              value={evidenceImages}
              onChange={setEvidenceImages}
              maxFiles={3}
              maxSize={UPLOAD_LIMITS.RETURN_PROOF}
              presignFn={uploadApi.getReturnProofPresignedUrl}
              onUploadingChange={setIsUploading}
              isCoverSelectorEnabled={false}
              disabled={createReturnRequestMutation.isPending}
            />
          </div>

          {/* COD Bank Details Section */}
          {isCod && (
            <div className="flex flex-col gap-3 p-3.5 rounded-2xl bg-amber-50/60 border border-amber-200/80">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-amber-900">
                  Thông tin nhận hoàn tiền (Đơn COD) <span className="text-[#ba1a1a]">*</span>
                </span>
              </div>
              <p className="text-[11px] text-amber-800 leading-relaxed">
                Đơn hàng thanh toán khi nhận hàng (COD) sẽ được hoàn tiền bằng hình thức chuyển khoản trực tiếp sau khi được duyệt.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <div className="flex flex-col gap-1 sm:col-span-2">
                  <label htmlFor="return-bank-name" className="text-[11px] font-semibold text-[#434656]">
                    Tên ngân hàng <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    id="return-bank-name"
                    type="text"
                    aria-invalid={Boolean(errors.bankName)}
                    {...register("bankName")}
                    placeholder="Ví dụ: Vietcombank, MB Bank, Techcombank..."
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all"
                  />
                  {errors.bankName && (
                    <span className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankName.message}</span>
                  )}
                </div>

                <div className="flex flex-col gap-1">
                  <label htmlFor="return-bank-acc" className="text-[11px] font-semibold text-[#434656]">
                    Số tài khoản <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    id="return-bank-acc"
                    type="text"
                    aria-invalid={Boolean(errors.bankAccountNumber)}
                    {...register("bankAccountNumber")}
                    placeholder="Nhập số tài khoản"
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all font-mono"
                  />
                  {errors.bankAccountNumber && (
                    <span className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankAccountNumber.message}</span>
                  )}
                </div>

                <div className="flex flex-col gap-1">
                  <label htmlFor="return-bank-holder" className="text-[11px] font-semibold text-[#434656]">
                    Chủ tài khoản <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    id="return-bank-holder"
                    type="text"
                    aria-invalid={Boolean(errors.bankAccountHolder)}
                    {...register("bankAccountHolder")}
                    placeholder="NGUYEN VAN A"
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all uppercase"
                  />
                  {errors.bankAccountHolder && (
                    <span className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankAccountHolder.message}</span>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#eff4ff]">
            <button
              type="button"
              onClick={onClose}
              disabled={createReturnRequestMutation.isPending}
              className="px-5 py-2.5 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-xs font-semibold text-[#434656] transition-colors cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={createReturnRequestMutation.isPending || isUploading}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold shadow-sm transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang tải ảnh...</span>
                </>
              ) : createReturnRequestMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang gửi...</span>
                </>
              ) : (
                "Gửi yêu cầu trả hàng"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
