"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  Edit,
  Send,
  Trash2,
  Lock,
  Layers,
  Package,
} from "lucide-react";
import type { SellerProductSummaryResponse } from "@/types/sellerProduct";
import {
  canPublishProduct,
  getApprovalStatusMeta,
} from "../../utils/sellerProductUtils";
import { ProductHoverCard } from "./ProductHoverCard";

interface SellerProductRowProps {
  product: SellerProductSummaryResponse;
  onToggleVisibility: (productId: string, currentStatus: "PUBLISHED" | "UNPUBLISHED") => void;
  onSubmitApproval: (product: SellerProductSummaryResponse) => void;
  onDeleteProduct: (product: SellerProductSummaryResponse) => void;
  isUpdatingVisibility?: boolean;
  isBottomRow?: boolean;
}

export function SellerProductRow({
  product,
  onToggleVisibility,
  onSubmitApproval,
  onDeleteProduct,
  isUpdatingVisibility = false,
  isBottomRow = false,
}: SellerProductRowProps) {
  const [imageError, setImageError] = useState(false);

  const approvalMeta = getApprovalStatusMeta(product.approvalStatus);
  const publishCheck = canPublishProduct(product.approvalStatus, product.isSuspended);
  const isPublished = product.visibilityStatus === "PUBLISHED";

  // Stock status checks
  const isOutOfStock = product.totalStock <= 0;
  const isLowStock = product.totalStock > 0 && product.totalStock < 5;

  // Multiple price check
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
    <tr className="border-b border-slate-100 hover:bg-slate-50/70 transition-colors group">
      {/* 1. Product Info Column (with truncation & light hover modal with image preview) */}
      <td className="py-2.5 px-3 overflow-hidden max-w-0">
        <div className="flex items-center gap-2.5 min-w-0">
          {/* Table Thumbnail */}
          <div className="w-9 h-9 rounded-xl bg-slate-100 border border-slate-200 shrink-0 overflow-hidden relative flex items-center justify-center">
            {product.coverImageUrl && !imageError ? (
              <img
                src={product.coverImageUrl}
                alt={product.name}
                onError={() => setImageError(true)}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
              />
            ) : (
              <Package className="w-4 h-4 text-slate-400" />
            )}
          </div>

          {/* Title & Metadata */}
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5 flex-wrap">
              {product.brandName && (
                <span className="px-1.5 py-0.2 rounded text-[10px] font-semibold bg-slate-100 text-slate-700 shrink-0">
                  {product.brandName}
                </span>
              )}
              {product.variantsCount > 1 && (
                <span className="inline-flex items-center gap-1 px-1.5 py-0.2 rounded text-[10px] font-medium bg-blue-50 text-blue-700 shrink-0">
                  <Layers className="w-2.5 h-2.5" />
                  {product.variantsCount} phân loại
                </span>
              )}
              {product.isSuspended && (
                <span className="inline-flex items-center gap-1 px-1.5 py-0.2 rounded text-[10px] font-bold bg-rose-100 text-rose-700 shrink-0">
                  <Lock className="w-2.5 h-2.5" />
                  Bị khóa
                </span>
              )}
            </div>

            {/* Truncated Name with Floating Light Hover Modal via Portal (Including Large Image Preview) */}
            <div className="min-w-0 mt-0.5">
              <ProductHoverCard product={product}>
                <Link
                  href={`/seller/products/${product.id}/edit`}
                  className="font-headline font-bold text-xs text-slate-900 hover:text-[#0045cc] transition-colors truncate block"
                >
                  {product.name}
                </Link>
              </ProductHoverCard>
            </div>

            <span className="text-[10px] text-slate-400 block truncate">
              {product.categoryName || "Linh kiện điện tử"}
            </span>
          </div>
        </div>
      </td>

      {/* 2. Giá bán (VND) - ALIGN RIGHT (Xuống hàng nếu có nhiều giá) */}
      <td className="py-2.5 px-2 whitespace-nowrap text-right">
        {hasPriceRange ? (
          <div className="flex flex-col items-end justify-center leading-tight gap-0.5">
            <span className="font-headline font-bold text-xs text-slate-900">
              {formatVND(product.minPrice)}
            </span>
            <span className="font-headline font-medium text-[10px] text-slate-500">
              - {formatVND(product.maxPrice)}
            </span>
          </div>
        ) : (
          <span className="font-headline font-bold text-xs text-slate-900">
            {formatVND(product.minPrice || product.maxPrice || 0)}
          </span>
        )}
      </td>

      {/* 3. Tồn kho - ALIGN CENTER */}
      <td className="py-2.5 px-1.5 whitespace-nowrap text-center">
        {isOutOfStock ? (
          <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-50 text-rose-600 border border-rose-200">
            Hết
          </span>
        ) : isLowStock ? (
          <span className="font-bold text-xs text-amber-600">
            {product.totalStock}
          </span>
        ) : (
          <span className="font-semibold text-xs text-slate-800">
            {product.totalStock}
          </span>
        )}
      </td>

      {/* 4. Trạng thái kiểm duyệt - ALIGN CENTER */}
      <td className="py-2.5 px-1.5 whitespace-nowrap text-center">
        <span
          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${approvalMeta.badgeClass}`}
        >
          <span className={`w-1.5 h-1.5 rounded-full ${approvalMeta.dotClass}`} />
          {approvalMeta.label}
        </span>
      </td>

      {/* 5. Mở bán (Switch Toggle only) - ALIGN CENTER */}
      <td className="py-2.5 px-1 whitespace-nowrap text-center">
        <label
          className={`relative inline-flex items-center justify-center ${
            publishCheck.canPublish ? "cursor-pointer" : "cursor-not-allowed opacity-60"
          }`}
          title={publishCheck.reason || (isPublished ? "Đang mở bán trên sàn" : "Đã tạm ẩn")}
        >
          <input
            type="checkbox"
            checked={isPublished}
            disabled={!publishCheck.canPublish || isUpdatingVisibility}
            onChange={() => onToggleVisibility(product.id, product.visibilityStatus)}
            className="sr-only peer"
          />
          <div className="w-7 h-4 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#0045cc]"></div>
        </label>
      </td>

      {/* 6. Thao tác - ALIGN RIGHT */}
      <td className="py-2.5 px-2.5 whitespace-nowrap text-right">
        <div className="inline-flex items-center justify-end gap-1">
          {/* Submit approval button for DRAFT or REJECTED */}
          {(product.approvalStatus === "DRAFT" || product.approvalStatus === "REJECTED") && (
            <button
              onClick={() => onSubmitApproval(product)}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-bold text-[#0045cc] bg-blue-50 hover:bg-blue-100 transition-colors shrink-0"
              title="Gửi sản phẩm để Ban quản trị phê duyệt"
            >
              <Send className="w-3 h-3" />
              <span>Gửi duyệt</span>
            </button>
          )}

          {/* Edit link */}
          <Link
            href={`/seller/products/${product.id}/edit`}
            className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 hover:text-slate-900 transition-colors shrink-0"
            title="Chỉnh sửa sản phẩm"
          >
            <Edit className="w-3 h-3 text-slate-500" />
            <span>Sửa</span>
          </Link>

          {/* Delete button */}
          <button
            onClick={() => onDeleteProduct(product)}
            className="w-6.5 h-6.5 flex items-center justify-center rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors shrink-0"
            title="Xóa sản phẩm"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </td>
    </tr>
  );
}
