"use client";

import React from "react";
import Link from "next/link";
import { Trophy, ChevronRight, Package, ArrowUpRight } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { TopSellingProductDto } from "@/types/sellerDashboard";

interface DashboardTopProductsProps {
  products?: TopSellingProductDto[];
  isLoading?: boolean;
}

export function DashboardTopProducts({ products = [], isLoading }: DashboardTopProductsProps) {
  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[#f1f5f9] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-600 flex items-center justify-center font-bold">
            <Trophy className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-heading text-sm sm:text-base font-bold text-[#0b1c30]">
              Top Sản Phẩm Bán Chạy
            </h3>
            <p className="text-[11px] text-[#737688]">
              Xếp hạng theo sản lượng và doanh số đóng góp trong kỳ này.
            </p>
          </div>
        </div>
        <Link
          href="/seller/products"
          className="text-xs font-bold text-[#0045cc] hover:text-[#0d5bff] flex items-center gap-1 transition-colors"
        >
          <span>Xem kho hàng</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* List */}
      {isLoading ? (
        <div className="flex flex-col gap-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center gap-3 p-3 rounded-2xl bg-slate-50 animate-pulse">
              <div className="w-12 h-12 rounded-xl bg-slate-200" />
              <div className="flex-1 space-y-2">
                <div className="h-3.5 bg-slate-200 rounded-sm w-3/4" />
                <div className="h-3 bg-slate-200 rounded-sm w-1/3" />
              </div>
            </div>
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="p-8 text-center flex flex-col items-center gap-2.5 bg-[#f8f9fc] rounded-2xl border border-dashed border-[#e2e8f0]">
          <div className="w-10 h-10 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center">
            <Package className="w-5 h-5" />
          </div>
          <span className="text-xs font-bold text-[#434656]">
            Chưa có sản phẩm nào bán ra trong khoảng thời gian này
          </span>
          <p className="text-[11px] text-[#737688] max-w-xs">
            Hãy đẩy mạnh các chương trình khuyến mãi hoặc kiểm tra lại tồn kho và giá niêm yết của gian hàng.
          </p>
          <Link
            href="/seller/products"
            className="mt-1 px-4 py-1.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-2xs"
          >
            Quản lý sản phẩm
          </Link>
        </div>
      ) : (
        <div className="flex flex-col divide-y divide-[#f1f5f9]">
          {products.map((item, index) => {
            const rank = index + 1;
            return (
              <div
                key={item.productId}
                className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-3 group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  {/* Rank Badge */}
                  <div
                    className={`w-6 h-6 rounded-lg font-heading text-xs font-extrabold flex items-center justify-center shrink-0 ${
                      rank === 1
                        ? "bg-amber-100 text-amber-700"
                        : rank === 2
                        ? "bg-slate-200 text-slate-700"
                        : rank === 3
                        ? "bg-amber-800/10 text-amber-900"
                        : "bg-slate-100 text-[#737688]"
                    }`}
                  >
                    {rank}
                  </div>

                  {/* Image */}
                  <div className="w-12 h-12 rounded-xl bg-[#f8f9fc] border border-[#e2e8f0] overflow-hidden shrink-0 flex items-center justify-center">
                    {item.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={item.imageUrl}
                        alt={item.productName}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      />
                    ) : (
                      <Package className="w-5 h-5 text-slate-400" />
                    )}
                  </div>

                  {/* Name and Units */}
                  <div className="min-w-0">
                    <h4 className="text-xs font-bold text-[#0b1c30] truncate group-hover:text-[#0045cc] transition-colors">
                      {item.productName}
                    </h4>
                    <span className="text-[11px] text-[#737688]">
                      Đã bán: <strong className="text-[#0b1c30]">{item.unitsSold}</strong> chiếc
                    </span>
                  </div>
                </div>

                {/* Revenue */}
                <div className="text-right shrink-0">
                  <span className="font-heading text-xs sm:text-sm font-extrabold text-[#0045cc]">
                    {formatPrice(item.totalRevenue)}
                  </span>
                  <p className="text-[10px] text-[#94a3b8]">doanh thu</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
