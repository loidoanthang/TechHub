"use client";

import React from "react";
import Link from "next/link";
import { Package, ArrowRight, AlertTriangle } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import { ProductThumbnail } from "@/components/shared";
import type { TopSellingProductDto, LowStockVariantDto } from "@/types/sellerDashboard";

interface DashboardTopProductsProps {
  products?: TopSellingProductDto[];
  lowStockAlerts?: LowStockVariantDto[];
  isLoading?: boolean;
}

export function DashboardTopProducts({
  products = [],
  lowStockAlerts = [],
  isLoading,
}: DashboardTopProductsProps) {
  // Check if any product has low stock warning
  const lowStockProductIds = new Set(lowStockAlerts.map((a) => a.productId));

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col justify-between h-full">
      {/* Header */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <h3 className="font-heading text-base font-bold text-[#0b1c30]">
            Sản Phẩm Bán Chạy Nhất
          </h3>
        </div>
        <Link
          href="/seller/products"
          className="text-xs font-bold text-[#0045cc] hover:text-[#0d5bff] flex items-center gap-1 transition-colors"
        >
          <span>Xem tất cả</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Product List */}
      <div className="flex-1 my-3 space-y-3">
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="flex items-center justify-between p-2.5 rounded-2xl bg-slate-50 animate-pulse"
              >
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-slate-200" />
                  <div className="space-y-1.5">
                    <div className="h-3.5 bg-slate-200 rounded w-28" />
                    <div className="h-3 bg-slate-200 rounded w-16" />
                  </div>
                </div>
                <div className="h-4 bg-slate-200 rounded w-16" />
              </div>
            ))}
          </div>
        ) : products.length === 0 ? (
          <div className="py-10 text-center flex flex-col items-center gap-2 bg-[#f8f9fc] rounded-2xl border border-dashed border-slate-200">
            <Package className="w-8 h-8 text-slate-300" />
            <span className="text-xs font-semibold text-slate-500">
              Chưa có dữ liệu sản phẩm bán chạy
            </span>
          </div>
        ) : (
          products.slice(0, 4).map((product) => {
            const isLowStock = lowStockProductIds.has(product.productId);

            return (
              <div
                key={product.productId}
                className="flex items-center justify-between p-2 hover:bg-slate-50/80 rounded-2xl transition-colors group"
              >
                {/* Left: Thumbnail & Name & Sales */}
                <div className="flex items-center gap-3 min-w-0">
                  <ProductThumbnail
                    src={product.imageUrl}
                    alt={product.productName}
                    sizeClassName="w-12 h-12"
                    imageClassName="group-hover:scale-105 transition-transform"
                  />
                  <div className="min-w-0">
                    <h4
                      className="text-xs font-bold text-[#0b1c30] truncate max-w-[160px] sm:max-w-[200px]"
                      title={product.productName}
                    >
                      {product.productName}
                    </h4>
                    <p className="text-[11px] text-[#737688] font-medium mt-0.5">
                      {product.unitsSold.toLocaleString("vi-VN")} đã bán •{" "}
                      {formatPrice(product.totalRevenue)}
                    </p>
                  </div>
                </div>

                {/* Right: Availability Status Badge */}
                <div className="text-right shrink-0">
                  {isLowStock ? (
                    <div className="flex items-center gap-1 text-[11px] font-semibold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200/60">
                      <AlertTriangle className="w-3 h-3" />
                      <span>Sắp hết kho</span>
                    </div>
                  ) : (
                    <div className="flex flex-col items-end">
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                        Khả dụng
                      </span>
                      <span className="text-[10px] text-slate-400">Còn hàng</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-[#737688]">
        <span>Cập nhật theo chu kỳ đã chọn</span>
        <Link
          href="/seller/products"
          className="font-semibold text-[#0045cc] hover:underline"
        >
          Quản lý kho
        </Link>
      </div>
    </div>
  );
}
