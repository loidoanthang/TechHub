"use client";

import { useState } from "react";
import { X, AlertTriangle, CheckCircle2 } from "lucide-react";
import { OrderDTO } from "@/mocks/orders.mock";

interface CancelOrderModalProps {
  order: OrderDTO | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (orderId: string, isDirect: boolean, reason: string) => void;
}

const CANCEL_REASONS = [
  "Changed my mind / Need to update delivery address",
  "Found a cheaper price elsewhere",
  "Ordered by mistake / Duplicate order",
  "Payment method issue / Want to change payment",
  "Seller is taking too long to prepare package",
  "Other reason",
];

export function CancelOrderModal({
  order,
  isOpen,
  onClose,
  onSuccess,
}: CancelOrderModalProps) {
  const [selectedReason, setSelectedReason] = useState(CANCEL_REASONS[0]);
  const [customReason, setCustomReason] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen || !order) return null;

  // Direct cancel if PENDING_PAYMENT, otherwise Request Cancellation if CONFIRMED/PROCESSING
  const isDirectCancel = order.status === "PENDING_PAYMENT";

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const finalReason =
      selectedReason === "Other reason" && customReason.trim()
        ? customReason.trim()
        : selectedReason;

    setTimeout(() => {
      setIsSubmitting(false);
      onSuccess(order.id, isDirectCancel, finalReason);
      onClose();
    }, 600);
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
                Order #{order.orderNumber} • {order.shopName}
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

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-[#0b1c30]">
              Reason for Cancellation <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="flex flex-col gap-2">
              {CANCEL_REASONS.map((r) => (
                <label
                  key={r}
                  className={`flex items-center gap-3 p-3 rounded-xl border text-xs cursor-pointer transition-all ${
                    selectedReason === r
                      ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc] font-medium"
                      : "border-[#c3c5d9]/40 hover:bg-[#eff4ff]/40 text-[#434656]"
                  }`}
                >
                  <input
                    type="radio"
                    name="cancel_reason"
                    checked={selectedReason === r}
                    onChange={() => setSelectedReason(r)}
                    className="accent-[#0045cc]"
                  />
                  <span>{r}</span>
                </label>
              ))}
            </div>
          </div>

          {selectedReason === "Other reason" && (
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-[#434656]">
                Provide details
              </label>
              <textarea
                rows={3}
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                placeholder="Explain why you wish to cancel this order..."
                className="w-full text-xs p-3 rounded-xl border border-[#c3c5d9]/60 focus:outline-none focus:border-[#0045cc] focus:ring-1 focus:ring-[#0045cc]"
                required
              />
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#eff4ff]">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#434656] hover:bg-[#eff4ff] transition-colors cursor-pointer"
            >
              Keep Order
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 rounded-xl text-xs font-bold bg-[#ba1a1a] hover:bg-[#93000a] text-white shadow-sm transition-all active:scale-[0.99] flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <span>Processing...</span>
              ) : (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>{isDirectCancel ? "Confirm Cancellation" : "Submit Request"}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
