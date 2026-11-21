"use client";

import React from "react";
import { Eye, Package, Store } from "lucide-react";
import { formatPrice, getThumbnailUrl } from "@/lib/utils";
import type { AdminProductSummaryResponse } from "@/types/adminCatalog";

interface AdminProductMobileCardProps {
  product: AdminProductSummaryResponse;
  onModerate: (productId: string) => void;
}

export function AdminProductMobileCard({
  product,
  onModerate,
}: AdminProductMobileCardProps) {
  const getApprovalBadge = (status: string, isSuspended?: boolean) => {
    if (isSuspended) {
      return "bg-rose-100 text-rose-800 border-rose-200";
    }
    switch (status) {
      case "APPROVED":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "PENDING_APPROVAL":
        return "bg-amber-100 text-amber-800 border-amber-200";
      case "REJECTED":
        return "bg-rose-100 text-rose-800 border-rose-200";
      case "DRAFT":
        return "bg-slate-100 text-slate-700 border-slate-200";
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Thumbnail + Title + Approval badge */}
      <div className="flex items-start gap-3">
        <div className="w-14 h-14 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
          {product.coverImageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={getThumbnailUrl(product.coverImageUrl, 120)}
              alt={product.name}
              width={56}
              height={56}
              loading="lazy"
              decoding="async"
              className="w-full h-full object-cover"
            />
          ) : (
            <Package className="w-5 h-5 text-slate-400" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-1.5">
            <h4 className="text-xs font-bold text-slate-900 line-clamp-2">
              {product.name}
            </h4>
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getApprovalBadge(
                product.approvalStatus,
                product.isSuspended
              )}`}
            >
              {product.isSuspended ? "Đình chỉ" : product.approvalStatus}
            </span>
          </div>

          <div className="flex items-center gap-1.5 text-[10px] text-slate-500 mt-1 flex-wrap">
            {product.brandName && <span>{product.brandName}</span>}
            {product.brandName && product.categoryName && <span>•</span>}
            {product.categoryName && <span>{product.categoryName}</span>}
          </div>
        </div>
      </div>

      {/* 2. Shop info & Stock */}
      <div className="bg-slate-50 p-2.5 rounded-xl flex items-center justify-between text-xs">
        <div className="flex items-start gap-1.5 text-slate-700 min-w-0 flex-1 pr-2">
          <Store className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
          <div className="min-w-0">
            <span className="font-semibold text-slate-900 block truncate">
              {product.shopName}
            </span>
            <span className="text-[10px] text-slate-500 font-mono block truncate">
              (@{product.sellerName})
            </span>
          </div>
        </div>

        <div className="text-[11px] text-slate-600 font-medium shrink-0 self-center">
          Kho: <strong className="font-mono text-slate-900">{product.totalStock}</strong>
        </div>
      </div>

      {/* 3. Price & Action button */}
      <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
        <div>
          <span className="text-[10px] text-slate-500 font-medium block">Giá bán:</span>
          <span className="font-mono font-bold text-slate-900 text-xs">
            {formatPrice(product.minPrice)}
            {product.minPrice !== product.maxPrice && (
              <span className="text-[11px] text-slate-500 font-normal">
                {" "}~ {formatPrice(product.maxPrice)}
              </span>
            )}
          </span>
        </div>

        <button
          type="button"
          onClick={() => onModerate(product.id)}
          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Kiểm duyệt</span>
        </button>
      </div>
    </div>
  );
}
