"use client";

import { useState } from "react";
import { X, RotateCcw, Upload, Image as ImageIcon, AlertCircle } from "lucide-react";
import { OrderDTO } from "@/mocks/orders.mock";
import { formatPrice } from "@/lib/utils";

interface ReturnRefundModalProps {
  order: OrderDTO | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (orderId: string, reason: string, description: string, mediaCount: number) => void;
}

const RETURN_REASONS = [
  "Item defective / Does not power on or function properly",
  "Item physically damaged or broken in transit",
  "Received wrong item / Incorrect variant delivered",
  "Item missing accessories or components",
  "Item noticeably does not match description or specifications",
];

export function ReturnRefundModal({
  order,
  isOpen,
  onClose,
  onSuccess,
}: ReturnRefundModalProps) {
  const [selectedReason, setSelectedReason] = useState(RETURN_REASONS[0]);
  const [description, setDescription] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen || !order) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const remainingSlots = 5 - uploadedFiles.length;
      const filesToAdd = Array.from(e.target.files).slice(0, remainingSlots);
      const newUrls = filesToAdd.map((file) => URL.createObjectURL(file));
      setUploadedFiles((prev) => [...prev, ...newUrls]);
    }
  };

  const removeFile = (idx: number) => {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    setTimeout(() => {
      setIsSubmitting(false);
      onSuccess(order.id, selectedReason, description, uploadedFiles.length);
      onClose();
    }, 600);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center shrink-0">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                Request Return &amp; Refund
              </h3>
              <p className="text-xs text-[#737688] font-mono">
                Entire Order #{order.orderNumber} • Refund Value:{" "}
                <strong className="text-[#0045cc] font-sans">
                  {formatPrice(order.totalAmount)}
                </strong>
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

        {/* Return & Refund Policy Banner */}
        <div className="p-3.5 rounded-2xl bg-[#eff4ff] text-xs text-[#434656] flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 text-[#0045cc] shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            Return requests apply to the entire order within 7 days of delivery. Once submitted, TechHub Escrow pauses merchant payout until the claim is resolved.
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Reason */}
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-[#0b1c30]">
              Reason for Return <span className="text-[#ba1a1a]">*</span>
            </label>
            <div className="flex flex-col gap-1.5">
              {RETURN_REASONS.map((r) => (
                <label
                  key={r}
                  className={`flex items-center gap-3 p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                    selectedReason === r
                      ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc] font-medium"
                      : "border-[#c3c5d9]/40 hover:bg-[#eff4ff]/40 text-[#434656]"
                  }`}
                >
                  <input
                    type="radio"
                    name="return_reason"
                    checked={selectedReason === r}
                    onChange={() => setSelectedReason(r)}
                    className="accent-[#0045cc]"
                  />
                  <span>{r}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Description */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#0b1c30]">
              Detailed Description <span className="text-[#ba1a1a]">*</span>
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Please provide specific details about the issue with serial numbers, package conditions, etc."
              className="w-full text-xs p-3 rounded-xl border border-[#c3c5d9]/60 focus:outline-none focus:border-[#0045cc] focus:ring-1 focus:ring-[#0045cc]"
              required
            />
          </div>

          {/* Media Evidence Upload (Max 5 items) */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-[#0b1c30]">
                Photo / Video Evidence (Max 5)
              </span>
              <span className="text-[#737688]">{uploadedFiles.length}/5 files</span>
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              {uploadedFiles.map((url, idx) => (
                <div
                  key={idx}
                  className="w-16 h-16 rounded-xl border border-[#c3c5d9]/60 relative overflow-hidden group shrink-0"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={url}
                    alt="Proof"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removeFile(idx)}
                    className="absolute inset-0 bg-black/50 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ))}

              {uploadedFiles.length < 5 && (
                <label className="w-16 h-16 rounded-xl border-2 border-dashed border-[#c3c5d9] hover:border-[#0045cc] bg-[#eff4ff]/40 hover:bg-[#eff4ff] flex flex-col items-center justify-center gap-1 text-[#737688] hover:text-[#0045cc] cursor-pointer transition-colors shrink-0">
                  <Upload className="w-4 h-4" />
                  <span className="text-[9px] font-semibold">Upload</span>
                  <input
                    type="file"
                    accept="image/*,video/*"
                    multiple
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>
            <p className="text-[10px] text-[#737688]">
              Formats: PNG, JPG, MP4 under 25MB. Clear unboxing photos or videos speed up review.
            </p>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#eff4ff]">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-[#434656] hover:bg-[#eff4ff] transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 rounded-xl text-xs font-bold bg-[#0045cc] hover:bg-[#0d5bff] text-white shadow-sm transition-all active:scale-[0.99] flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? "Submitting Request..." : "Submit Return Request"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
