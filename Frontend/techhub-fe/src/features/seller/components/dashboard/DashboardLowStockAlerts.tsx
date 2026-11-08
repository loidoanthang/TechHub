"use client";

import React from "react";
import Link from "next/link";
import { AlertTriangle, CheckCircle2, ChevronRight, Package, ArrowUpRight } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { LowStockVariantDto } from "@/types/sellerDashboard";

interface DashboardLowStockAlertsProps {
  alerts?: LowStockVariantDto[];
  isLoading?: boolean;
}

export function DashboardLowStockAlerts({
  alerts = [],
  isLoading,
}: DashboardLowStockAlertsProps) {
  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[#f1f5f9] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-red-500/10 text-red-600 flex items-center justify-center font-bold">
            <AlertTriangle className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-heading text-sm sm:text-base font-bold text-[#0b1c30]">
              Cảnh Báo Tồn Kho Thấp
            </h3>
            <p className="text-[11px] text-[#737688]">
              Biến thể có tồn kho &le; 5 sản phẩm cần nhập thêm để tránh hủy đơn.
            </p>
          </div>
        </div>
        <Link
          href="/seller/products"
          className="text-xs font-bold text-[#0045cc] hover:text-[#0d5bff] flex items-center gap-1 transition-colors"
        >
          <span>Quản lý kho</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* List */}
      {isLoading ? (
        <div className="flex flex-col gap-3">
          {[1, 2].map((i) => (
            <div key={i} className="flex items-center gap-3 p-3 rounded-2xl bg-slate-50 animate-pulse">
              <div className="w-10 h-10 rounded-xl bg-slate-200" />
              <div className="flex-1 space-y-2">
                <div className="h-3.5 bg-slate-200 rounded-sm w-1/2" />
                <div className="h-3 bg-slate-200 rounded-sm w-1/4" />
              </div>
            </div>
          ))}
        </div>
      ) : alerts.length === 0 ? (
        <div className="p-6 text-center flex flex-col items-center gap-2 bg-emerald-50/50 rounded-2xl border border-emerald-100">
          <div className="w-9 h-9 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <span className="text-xs font-bold text-emerald-900">
            Tất cả tồn kho đều an toàn
          </span>
          <p className="text-[11px] text-emerald-700 max-w-xs">
            Hiện không có phân loại sản phẩm nào chạm ngưỡng cảnh báo cạn hàng (&le; 5 cái).
          </p>
        </div>
      ) : (
        <div className="flex flex-col divide-y divide-[#f1f5f9]">
          {alerts.map((item) => {
            const isOutOfStock = item.stockQuantity === 0;
            return (
              <div
                key={item.variantId}
                className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-3 group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-11 h-11 rounded-xl bg-[#f8f9fc] border border-[#e2e8f0] overflow-hidden shrink-0 flex items-center justify-center">
                    {item.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={item.imageUrl}
                        alt={item.productName}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <Package className="w-5 h-5 text-slate-400" />
                    )}
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs font-bold text-[#0b1c30] truncate group-hover:text-[#0045cc] transition-colors">
                        {item.productName}
                      </h4>
                      {item.sku && (
                        <span className="text-[10px] font-mono px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded-md border border-slate-200 shrink-0">
                          {item.sku}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-[#737688]">
                      Phân loại: <span className="font-semibold text-[#434656]">{item.variantName}</span> • Giá: {formatPrice(item.price)}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <span
                    className={`text-xs font-bold px-2.5 py-1 rounded-xl border ${
                      isOutOfStock
                        ? "bg-red-50 text-red-700 border-red-200"
                        : "bg-amber-50 text-amber-700 border-amber-200"
                    }`}
                  >
                    {isOutOfStock ? "Hết hàng" : `Còn ${item.stockQuantity} cái`}
                  </span>

                  <Link
                    href={`/seller/products?search=${encodeURIComponent(item.sku || item.productName)}`}
                    className="p-1.5 rounded-lg text-[#737688] hover:text-[#0045cc] hover:bg-[#eff4ff] transition-colors"
                    title="Cập nhật tồn kho"
                  >
                    <ArrowUpRight className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
