"use client";

import { useState } from "react";
import Image from "next/image";
import { X, Star, Upload, CheckCircle2 } from "lucide-react";
import { OrderItemDTO } from "@/mocks/orders.mock";

interface ReviewProductModalProps {
  item: OrderItemDTO | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (itemId: string, rating: number, comment: string, mediaCount: number) => void;
}

export function ReviewProductModal({
  item,
  isOpen,
  onClose,
  onSuccess,
}: ReviewProductModalProps) {
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen || !item) return null;

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
      onSuccess(item.id, rating, comment, uploadedFiles.length);
      onClose();
    }, 600);
  };

  const ratingDescriptions = [
    "Very Poor - Significant issues encountered",
    "Poor - Did not meet expectations",
    "Average - Acceptable but room for improvement",
    "Good - Satisfactory hardware performance",
    "Outstanding - Exceeded expectations, highly recommended!",
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-[#c3c5d9]/30 relative flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
              Product Review &amp; Rating
            </h3>
            <p className="text-xs text-[#737688]">
              Share your genuine experience with the community
            </p>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-[#eff4ff] hover:bg-[#c3c5d9]/30 text-[#434656] flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Product Snapshot */}
        <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-[#eff4ff]/60 border border-[#eff4ff]">
          <div className="w-14 h-14 rounded-xl overflow-hidden bg-white shrink-0 border border-[#c3c5d9]/30 relative">
            <Image
              src={item.imageUrl}
              alt={item.productName}
              fill
              className="object-cover"
              unoptimized
            />
          </div>
          <div className="min-w-0">
            <h4 className="text-xs font-bold text-[#0b1c30] line-clamp-2 leading-snug">
              {item.productName}
            </h4>
            <span className="text-[11px] text-[#737688] font-medium">
              Variant: {item.variantName}
            </span>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Star Selection */}
          <div className="flex flex-col items-center justify-center gap-2 py-3 bg-[#f8faff] rounded-2xl border border-[#c3c5d9]/25">
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  type="button"
                  key={star}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  onClick={() => setRating(star)}
                  className="p-1 transition-transform hover:scale-110 cursor-pointer"
                >
                  <Star
                    className={`w-7 h-7 ${
                      (hoverRating || rating) >= star
                        ? "fill-[#f59e0b] text-[#f59e0b]"
                        : "text-[#c3c5d9]"
                    } transition-colors`}
                  />
                </button>
              ))}
            </div>
            <p className="text-xs font-bold text-[#0045cc]">
              {ratingDescriptions[rating - 1]}
            </p>
          </div>

          {/* Comment */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#0b1c30]">
              Your Review &amp; Benchmark Findings
            </label>
            <textarea
              rows={4}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="How was the build quality, temperatures, acoustics, or delivery packaging? Share detailed feedback..."
              className="w-full text-xs p-3 rounded-xl border border-[#c3c5d9]/60 focus:outline-none focus:border-[#0045cc] focus:ring-1 focus:ring-[#0045cc]"
              required
            />
          </div>

          {/* Media Evidence (Max 5 items) */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-[#0b1c30]">
                Photos &amp; Unboxing Videos (Max 5)
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
                  <span className="text-[9px] font-semibold">Add Media</span>
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
              {isSubmitting ? (
                "Submitting..."
              ) : (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Submit Review</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
