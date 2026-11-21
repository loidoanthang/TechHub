"use client";

import { useState } from "react";
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
  { code: "DEFECTIVE", label: "Item defective / Does not power on or function properly" },
  { code: "DAMAGED", label: "Item physically damaged or broken in transit" },
  { code: "WRONG_ITEM", label: "Received wrong item / Incorrect variant delivered" },
  { code: "MISSING_PARTS", label: "Item missing accessories or components" },
  { code: "NOT_AS_DESCRIBED", label: "Item noticeably does not match description or specifications" },
];

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
  const [selectedReasonCode, setSelectedReasonCode] = useState(RETURN_REASONS[0].code);
  const [description, setDescription] = useState("");
  const [evidenceImages, setEvidenceImages] = useState<string[]>([]);
  const [bankName, setBankName] = useState("");
  const [bankAccountNumber, setBankAccountNumber] = useState("");
  const [bankAccountHolder, setBankAccountHolder] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const isCod = paymentMethod === "COD";

  const createReturnRequestMutation = useCreateReturnRequestMutation();

  if (!isOpen) return null;

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const reasonItem = RETURN_REASONS.find((r) => r.code === selectedReasonCode);
    const reasonText = `${reasonItem?.label || selectedReasonCode}: ${description.trim()}`;

    if (isCod) {
      if (!bankName.trim() || !bankAccountNumber.trim() || !bankAccountHolder.trim()) {
        setErrorMessage("Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng để nhận hoàn tiền cho đơn COD.");
        return;
      }
    }

    try {
      await createReturnRequestMutation.mutateAsync({
        orderId,
        data: {
          reason: reasonText,
          proofImages: evidenceImages.length > 0 ? evidenceImages.join(",") : undefined,
          bankName: isCod ? bankName.trim() : undefined,
          bankAccountNumber: isCod ? bankAccountNumber.trim() : undefined,
          bankAccountHolder: isCod ? bankAccountHolder.trim().toUpperCase() : undefined,
        },
      });
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errObj?.response?.data?.message ||
        errObj?.message ||
        "Failed to submit return request. Please try again."
      );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center shrink-0">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                Return &amp; Refund Request
              </h3>
              <p className="text-xs text-[#737688] font-mono">
                Order #{orderNumber} • {shopName}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-[#eff4ff] hover:bg-[#c3c5d9]/30 text-[#434656] flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Refund Estimated Banner */}
        <div className="p-3.5 rounded-2xl bg-[#0045cc]/5 border border-[#0045cc]/15 flex items-center justify-between">
          <span className="text-xs text-[#0b1c30] font-medium">Estimated Refund:</span>
          <span className="text-sm font-bold text-[#0045cc]">{formatPrice(totalAmount)}</span>
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#0b1c30]">
              Reason for Return <span className="text-[#ba1a1a]">*</span>
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
                    name="return_reason"
                    value={r.code}
                    checked={selectedReasonCode === r.code}
                    onChange={() => setSelectedReasonCode(r.code)}
                    className="accent-[#0045cc] mt-0.5 cursor-pointer"
                  />
                  <span>{r.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#0b1c30]">
              Detailed Description <span className="text-[#ba1a1a]">*</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="Please provide details about the issue with photos/video timestamps..."
              required
              className="w-full p-3 rounded-2xl border border-[#c3c5d9]/40 bg-[#eff4ff]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all resize-none"
            />
          </div>

          {/* Evidence Attachments */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-[#0b1c30]">
                Evidence Media (Max 3)
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
                  <label className="text-[11px] font-semibold text-[#434656]">
                    Tên ngân hàng <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    type="text"
                    value={bankName}
                    onChange={(e) => setBankName(e.target.value)}
                    placeholder="Ví dụ: Vietcombank, MB Bank, Techcombank..."
                    required={isCod}
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all"
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[11px] font-semibold text-[#434656]">
                    Số tài khoản <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    type="text"
                    value={bankAccountNumber}
                    onChange={(e) => setBankAccountNumber(e.target.value.replace(/\s+/g, ""))}
                    placeholder="Nhập số tài khoản"
                    required={isCod}
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all font-mono"
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[11px] font-semibold text-[#434656]">
                    Chủ tài khoản <span className="text-[#ba1a1a]">*</span>
                  </label>
                  <input
                    type="text"
                    value={bankAccountHolder}
                    onChange={(e) => setBankAccountHolder(e.target.value.toUpperCase())}
                    placeholder="NGUYEN VAN A"
                    required={isCod}
                    className="w-full px-3 py-2 rounded-xl border border-[#c3c5d9]/40 bg-white text-xs text-[#0b1c30] placeholder:text-[#737688] focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all uppercase"
                  />
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
              Cancel
            </button>
            <button
              type="submit"
              disabled={createReturnRequestMutation.isPending || isUploading}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold shadow-sm transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Uploading evidence...</span>
                </>
              ) : createReturnRequestMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Submitting...</span>
                </>
              ) : (
                "Submit Return Request"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
