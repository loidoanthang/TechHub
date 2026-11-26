"use client";

import Link from "next/link";
import { ProductCard } from "@/components/shared";
import { useRecentlyViewedProductsQuery } from "@/features/products/hooks/useProductQueries";
import { mapRecentlyViewedToProduct } from "@/features/products/utils/productMapper";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";
import { Clock, ShoppingBag, EyeOff, Sparkles, AlertCircle } from "lucide-react";

export function RecentlyViewedView() {
  const { isAuthenticated, user, isLoading: isAuthLoading } = useAuth();
  const isBuyer = Boolean(user?.roles?.includes("BUYER"));

  const { data: rawItems, isLoading: isQueryLoading, error, refetch } = useRecentlyViewedProductsQuery(
    isAuthenticated && isBuyer
  );

  const isLoading = isAuthLoading || isQueryLoading;
  const products = (rawItems || []).map(mapRecentlyViewedToProduct);

  return (
    <div className="flex flex-col gap-6">
      {/* 1. Header Bento Card */}
      <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/30 shadow-sm relative overflow-hidden flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex flex-col gap-1 z-10">
          <div className="flex items-center gap-2.5">
            <h1 className="font-heading text-xl sm:text-2xl font-bold text-[#0b1c30] tracking-tight">
              Sản phẩm đã xem gần đây
            </h1>
            <span className="px-3 py-1 rounded-full text-xs font-bold border bg-blue-50 text-[#0045cc] border-blue-200">
              {products.length} sản phẩm
            </span>
          </div>
          <p className="text-xs sm:text-sm text-[#737688]">
            Danh sách các thiết bị công nghệ bạn đã tham khảo (lưu tối đa 10 sản phẩm mới nhất).
          </p>
        </div>

        {/* Ambient subtle glow */}
        <div className="absolute -right-8 -bottom-8 w-44 h-44 bg-[#0045cc]/5 rounded-full pointer-events-none blur-3xl"></div>
      </div>

      {/* 2. Content Area */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="bg-white rounded-2xl p-4 border border-[#c3c5d9]/25 shadow-sm flex flex-col gap-3 animate-pulse"
            >
              <div className="w-full aspect-square bg-slate-100 rounded-xl"></div>
              <div className="h-4 bg-slate-100 rounded-md w-3/4"></div>
              <div className="h-4 bg-slate-100 rounded-md w-1/2"></div>
              <div className="h-8 bg-slate-100 rounded-lg w-full mt-2"></div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-white rounded-2xl p-10 border border-slate-200 text-center flex flex-col items-center justify-center gap-3">
          <AlertCircle className="w-10 h-10 text-rose-500" />
          <h3 className="text-base font-bold text-[#0b1c30]">
            Không thể tải sản phẩm đã xem
          </h3>
          <p className="text-xs text-[#737688] max-w-sm">
            Đã xảy ra lỗi khi kết nối máy chủ. Vui lòng thử lại sau.
          </p>
          <button
            type="button"
            onClick={() => refetch()}
            className="mt-1 px-4 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      ) : products.length === 0 ? (
        <div className="bg-white rounded-2xl p-10 sm:p-14 border border-[#c3c5d9]/25 shadow-sm text-center flex flex-col items-center justify-center gap-4">
          <div className="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 border border-slate-100">
            <EyeOff className="w-7 h-7" />
          </div>
          <div className="max-w-md flex flex-col gap-1.5">
            <h3 className="font-heading text-base sm:text-lg font-bold text-[#0b1c30]">
              Chưa có sản phẩm nào được lưu
            </h3>
            <p className="text-xs sm:text-sm text-[#737688]">
              Khi bạn xem các sản phẩm công nghệ trên sàn, lịch sử xem sẽ xuất hiện tại đây để bạn dễ dàng tìm lại.
            </p>
          </div>
          <Link
            href={ROUTES.PRODUCTS}
            className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs sm:text-sm font-semibold transition-all shadow-sm mt-2"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Khám phá sản phẩm ngay</span>
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  );
}
