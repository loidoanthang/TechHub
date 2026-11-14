"use client";

import { useState } from "react";
import { X, RotateCcw, Upload, AlertCircle, Loader2 } from "lucide-react";
import { useCreateReturnRequestMutation } from "@/features/orders/hooks/useOrderQueries";

interface ReturnRefundModalProps {
  orderId: string;
  orderNumber: string;
  shopName: string;
  totalAmount: number;
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
  isOpen,
  onClose,
  onSuccess,
}: ReturnRefundModalProps) {
  const [selectedReasonCode, setSelectedReasonCode] = useState(RETURN_REASONS[0].code);
  const [description, setDescription] = useState("");
  const [evidenceImages, setEvidenceImages] = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const createReturnRequestMutation = useCreateReturnRequestMutation();

  if (!isOpen) return null;

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const handleAddSampleImage = () => {
    if (evidenceImages.length >= 3) return;
    setEvidenceImages((prev) => [
      ...prev,
      "/images/sample-evidence.svg",
    ]);
  };

  const removeImage = (idx: number) => {
    setEvidenceImages((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const reasonItem = RETURN_REASONS.find((r) => r.code === selectedReasonCode);
    const reasonText = `${reasonItem?.label || selectedReasonCode}: ${description.trim()}`;

    try {
      await createReturnRequestMutation.mutateAsync({
        orderId,
        data: {
          reason: reasonText,
          proofImages: evidenceImages.length > 0 ? evidenceImages.join(",") : undefined,
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
              <button
                type="button"
                onClick={handleAddSampleImage}
                disabled={evidenceImages.length >= 3}
                className="text-[11px] font-semibold text-[#0045cc] hover:underline flex items-center gap-1 cursor-pointer disabled:opacity-40"
              >
                <Upload className="w-3.5 h-3.5" />
                <span>+ Add Sample Photo</span>
              </button>
            </div>

            {evidenceImages.length > 0 && (
              <div className="flex gap-2">
                {evidenceImages.map((img, i) => (
                  <div key={i} className="relative w-16 h-16 rounded-xl overflow-hidden border border-[#c3c5d9]/40 group">
                    <img src={img} alt="Evidence" className="w-full h-full object-cover" />
                    <button
                      type="button"
                      onClick={() => removeImage(i)}
                      className="absolute inset-0 bg-black/50 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

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
              disabled={createReturnRequestMutation.isPending}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold shadow-sm transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {createReturnRequestMutation.isPending ? (
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
