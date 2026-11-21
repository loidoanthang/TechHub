"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Edit, Send, Trash2, Layers, Package, Loader2 } from "lucide-react";
import type { SellerProductSummaryResponse } from "@/types/sellerProduct";
import { canPublishProduct, getApprovalStatusMeta } from "../../utils/sellerProductUtils";

interface SellerProductMobileCardProps {
  product: SellerProductSummaryResponse;
  onToggleVisibility: (productId: string, currentStatus: "PUBLISHED" | "UNPUBLISHED") => void;
  onSubmitApproval: (product: SellerProductSummaryResponse) => void;
  onDeleteProduct: (product: SellerProductSummaryResponse) => void;
  isUpdatingVisibility?: boolean;
}

export function SellerProductMobileCard({
  product,
  onToggleVisibility,
  onSubmitApproval,
  onDeleteProduct,
  isUpdatingVisibility = false,
}: SellerProductMobileCardProps) {
  const [imageError, setImageError] = useState(false);

  const approvalMeta = getApprovalStatusMeta(product.approvalStatus);
  const publishCheck = canPublishProduct(product.approvalStatus, product.isSuspended);
  const isPublished = product.visibilityStatus === "PUBLISHED";

  const isOutOfStock = product.totalStock <= 0;
  const isLowStock = product.totalStock > 0 && product.totalStock < 5;

  const hasPriceRange =
    product.variantsCount > 1 &&
    product.minPrice !== product.maxPrice &&
    product.minPrice > 0 &&
    product.maxPrice > 0;

  const formatVND = (val: number) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(val);

  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl p-4 flex flex-col gap-3 shadow-2xs">
      {/* Top row: Image + Name + Brand + Price */}
      <div className="flex items-start gap-3">
        {/* Thumbnail */}
        <div className="w-14 h-14 rounded-xl bg-slate-100 border border-slate-200 shrink-0 overflow-hidden relative flex items-center justify-center">
          {product.coverImageUrl && !imageError ? (
            <img
              src={product.coverImageUrl}
              alt={product.name}
              onError={() => setImageError(true)}
              className="w-full h-full object-cover"
            />
          ) : (
            <Package className="w-6 h-6 text-slate-400" />
          )}
        </div>

        {/* Title, Brand, Variants */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap mb-0.5">
            {product.brandName && (
              <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-700">
                {product.brandName}
              </span>
            )}
            {product.variantsCount > 1 && (
              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-blue-50 text-blue-700">
                <Layers className="w-2.5 h-2.5" />
                {product.variantsCount} phân loại
              </span>
            )}
          </div>
          <h4 className="font-bold text-xs text-slate-900 line-clamp-2 leading-snug">
            {product.name}
          </h4>
        </div>
      </div>

      {/* Middle row: Price, Stock, Approval status */}
      <div className="grid grid-cols-2 gap-2 py-2 px-3 bg-slate-50/70 rounded-xl text-xs border border-slate-100">
        <div>
          <span className="text-[10px] text-slate-500 block">Giá bán:</span>
          {hasPriceRange ? (
            <span className="font-mono font-bold text-slate-900 text-xs">
              {formatVND(product.minPrice)} ~ {formatVND(product.maxPrice)}
            </span>
          ) : (
            <span className="font-mono font-bold text-slate-900 text-xs">
              {formatVND(product.minPrice || product.maxPrice || 0)}
            </span>
          )}
        </div>

        <div>
          <span className="text-[10px] text-slate-500 block">Tồn kho:</span>
          <span
            className={`font-mono font-bold ${
              isOutOfStock
                ? "text-red-600"
                : isLowStock
                ? "text-amber-600"
                : "text-slate-900"
            }`}
          >
            {product.totalStock}{" "}
            <span className="text-[10px] font-normal text-slate-500">cái</span>
          </span>
        </div>
      </div>

      {/* Status & Visibility Row */}
      <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-100 text-xs">
        {/* Approval Badge */}
        <span
          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${approvalMeta.badgeClass}`}
        >
          {approvalMeta.label}
        </span>

        {/* Toggle Publish Switch */}
        <div className="flex items-center gap-2">
          <span className="text-[11px] text-slate-500 font-medium">Mở bán:</span>
          <button
            type="button"
            disabled={!publishCheck.canPublish || isUpdatingVisibility}
            onClick={() =>
              onToggleVisibility(
                product.id,
                isPublished ? "UNPUBLISHED" : "PUBLISHED"
              )
            }
            title={publishCheck.reason || (isPublished ? "Tạm ẩn sản phẩm" : "Mở bán sản phẩm")}
            className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed ${
              isPublished ? "bg-emerald-500" : "bg-slate-300"
            }`}
          >
            {isUpdatingVisibility ? (
              <span className="absolute inset-0 flex items-center justify-center">
                <Loader2 className="w-3 h-3 text-white animate-spin" />
              </span>
            ) : (
              <span
                className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                  isPublished ? "translate-x-4" : "translate-x-0"
                }`}
              />
            )}
          </button>
        </div>
      </div>

      {/* Actions Row */}
      <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
        {/* Submit approval if rejected or draft */}
        {(product.approvalStatus === "DRAFT" || product.approvalStatus === "REJECTED") && (
          <button
            type="button"
            onClick={() => onSubmitApproval(product)}
            className="px-2.5 py-1.5 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#0045cc] font-semibold text-xs flex items-center gap-1 transition-colors"
          >
            <Send className="w-3 h-3" />
            <span>Gửi duyệt</span>
          </button>
        )}

        {/* Edit Button */}
        <Link
          href={`/seller/products/${product.id}/edit`}
          className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs flex items-center gap-1 transition-colors shadow-2xs"
        >
          <Edit className="w-3 h-3 text-slate-500" />
          <span>Sửa</span>
        </Link>

        {/* Delete Button */}
        <button
          type="button"
          onClick={() => onDeleteProduct(product)}
          className="p-1.5 rounded-xl text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
          title="Xóa sản phẩm"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
