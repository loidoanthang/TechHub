"use client";

import React from "react";
import Image from "next/image";
import {
  Star,
  ShieldAlert,
  Store,
  Eye,
  Trash2,
  Image as ImageIcon,
} from "lucide-react";
import type { AdminReviewResponse } from "@/types/review";
import { formatDate } from "@/lib/utils";

interface AdminReviewMobileCardProps {
  review: AdminReviewResponse;
  onViewDetail: (review: AdminReviewResponse) => void;
  onDelete: (review: AdminReviewResponse) => void;
}

export function AdminReviewMobileCard({
  review,
  onViewDetail,
  onDelete,
}: AdminReviewMobileCardProps) {
  return (
    <div className="p-4 bg-white border border-slate-200/80 rounded-2xl shadow-xs space-y-3">
      {/* 1. Header: Buyer Info + Date */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          {review.reviewerAvatar ? (
            <div className="relative w-8 h-8 rounded-full overflow-hidden border border-slate-200 shrink-0">
              <Image
                src={review.reviewerAvatar}
                alt={review.reviewerName}
                fill
                className="object-cover"
                sizes="32px"
              />
            </div>
          ) : (
            <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs shrink-0">
              {review.reviewerName.charAt(0).toUpperCase()}
            </div>
          )}
          <div className="min-w-0">
            <p className="text-xs font-bold text-slate-900 truncate">
              {review.reviewerName}
            </p>
            <p className="text-[10px] text-slate-400">
              {formatDate(review.createdAt)}
            </p>
          </div>
        </div>

        {/* Badges */}
        <div className="flex items-center gap-1.5 shrink-0">
          {review.isReported && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700">
              <ShieldAlert className="w-3 h-3" />
              Báo cáo ({review.reportCount || 1})
            </span>
          )}
        </div>
      </div>

      {/* 2. Rating & Comment */}
      <div className="space-y-1.5">
        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <Star
              key={star}
              className={`w-3.5 h-3.5 ${
                star <= review.rating
                  ? "fill-amber-400 text-amber-400"
                  : "fill-slate-100 text-slate-200"
              }`}
            />
          ))}
          <span className="text-xs font-bold text-slate-700 ml-1">
            {review.rating}/5
          </span>
          {review.mediaList && review.mediaList.length > 0 && (
            <span className="inline-flex items-center gap-1 ml-auto text-[10px] font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
              <ImageIcon className="w-3 h-3 text-slate-400" />
              {review.mediaList.length} ảnh
            </span>
          )}
        </div>

        <p className="text-xs text-slate-700 italic line-clamp-2 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          &ldquo;{review.comment || "Không có bình luận chữ"}&rdquo;
        </p>
      </div>

      {/* 3. Product context */}
      <div className="flex items-center gap-2.5 p-2 bg-slate-50/70 rounded-xl border border-slate-100">
        {review.imageUrl && (
          <div className="relative w-9 h-9 rounded-lg overflow-hidden bg-white border border-slate-200 shrink-0">
            <Image
              src={review.imageUrl}
              alt={review.productName}
              fill
              className="object-cover"
              sizes="36px"
            />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium text-slate-800 truncate">
            {review.productName}
          </p>
          {review.shopName && (
            <div className="flex items-center gap-1 text-[11px] text-slate-500 truncate">
              <Store className="w-3 h-3 text-slate-400 shrink-0" />
              <span className="truncate">{review.shopName}</span>
            </div>
          )}
        </div>
      </div>

      {/* 4. Action buttons */}
      <div className="grid grid-cols-2 gap-2 pt-1">
        <button
          type="button"
          onClick={() => onViewDetail(review)}
          className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Chi tiết</span>
        </button>
        <button
          type="button"
          onClick={() => onDelete(review)}
          className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200/60 rounded-xl transition-colors cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5 text-rose-600" />
          <span>Gỡ bỏ</span>
        </button>
      </div>
    </div>
  );
}
