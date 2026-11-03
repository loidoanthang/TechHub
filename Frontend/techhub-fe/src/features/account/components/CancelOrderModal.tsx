"use client";

import { useState } from "react";
import { X, AlertTriangle, Loader2 } from "lucide-react";
import { useCancelOrderMutation, useRequestCancellationMutation } from "@/features/orders/hooks/useOrderQueries";
import type { CancellationReasonCode, OrderStatus } from "@/types/enums";

interface CancelOrderModalProps {
  orderId: string;
  orderNumber: string;
  shopName: string;
  status: OrderStatus;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (isDirect: boolean) => void;
}

const REASON_OPTIONS: { code: CancellationReasonCode; label: string }[] = [
  { code: "THAY_DOI_Y_DINH", label: "Changed my mind / Need to update delivery address" },
  { code: "TIM_THAY_GIA_TOT_HON", label: "Found a cheaper price elsewhere" },
  { code: "CHANGE_PAYMENT_METHOD", label: "Payment method issue / Want to change payment" },
  { code: "CHANGE_SHIPPING_ADDRESS", label: "Need to change shipping address" },
  { code: "KHAC", label: "Other reason" },
];

export function CancelOrderModal({
  orderId,
  orderNumber,
  shopName,
  status,
  isOpen,
  onClose,
  onSuccess,
}: CancelOrderModalProps) {
  const [selectedReasonCode, setSelectedReasonCode] = useState<CancellationReasonCode>("THAY_DOI_Y_DINH");
  const [customReason, setCustomReason] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const cancelOrderMutation = useCancelOrderMutation();
  const requestCancellationMutation = useRequestCancellationMutation();

  if (!isOpen) return null;

  // Direct cancel if PENDING_PAYMENT, otherwise Request Cancellation if PROCESSING/CONFIRMED
  const isDirectCancel = status === "PENDING_PAYMENT";
  const isPending = cancelOrderMutation.isPending || requestCancellationMutation.isPending;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const reasonNote = selectedReasonCode === "KHAC" ? customReason.trim() : undefined;

    try {
      if (isDirectCancel) {
        await cancelOrderMutation.mutateAsync({
          orderId,
          data: {
            reasonCode: selectedReasonCode,
            reasonNote,
          },
        });
      } else {
        await requestCancellationMutation.mutateAsync({
          orderId,
          data: {
            reasonCode: selectedReasonCode,
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
        "Failed to submit cancellation. Please try again."
      );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#ba1a1a]/10 text-[#ba1a1a] flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                {isDirectCancel ? "Cancel Order" : "Request Order Cancellation"}
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

        {/* Notice */}
        <div className="p-3.5 rounded-2xl bg-[#eff4ff] text-xs text-[#434656] leading-relaxed">
          {isDirectCancel ? (
            <p>
              This order has not been paid yet. Confirming cancellation will immediately release inventory and cancel the order without fee.
            </p>
          ) : (
            <p>
              Your order is already being confirmed or packed by the seller. Submitting this request requires merchant review per TechHub Order Resolution Guidelines.
            </p>
          )}
        </div>

        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs">
            {errorMessage}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-[#0b1c30]">
              Reason for Cancellation <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="flex flex-col gap-2">
              {REASON_OPTIONS.map((opt) => (
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
                    name="cancel_reason"
                    value={opt.code}
                    checked={selectedReasonCode === opt.code}
                    onChange={() => setSelectedReasonCode(opt.code)}
                    className="accent-[#0045cc] cursor-pointer"
                  />
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          {selectedReasonCode === "KHAC" && (
            <div className="flex flex-col gap-1.5 animate-in fade-in duration-150">
              <label className="text-xs font-semibold text-[#0b1c30]">
                Please describe the specific reason:
              </label>
              <textarea
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                rows={3}
                placeholder="Details help us process cancellations and resolve inquiries faster..."
                required
                className="w-full p-3 rounded-2xl border border-[#c3c5d9]/40 bg-[#eff4ff]/30 text-xs text-[#0b1c30] placeholder:text-[#737688] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 transition-all resize-none"
              />
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
              Nevermind
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="px-5 py-2.5 rounded-xl bg-[#ba1a1a] hover:bg-[#93000a] text-white text-xs font-semibold shadow-sm transition-all flex items-center justify-center gap-2 active:scale-[0.99] cursor-pointer disabled:opacity-60"
            >
              {isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Processing...</span>
                </>
              ) : isDirectCancel ? (
                "Confirm Immediate Cancellation"
              ) : (
                "Submit Request to Merchant"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
