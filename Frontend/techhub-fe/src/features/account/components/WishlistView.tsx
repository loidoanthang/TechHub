"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Heart,
  Trash2,
  ExternalLink,
  ShoppingBag,
  AlertCircle,
  Loader2,
  PackageX,
  X,
} from "lucide-react";
import {
  useWishlistQuery,
  useRemoveFromWishlistMutation,
} from "../api/wishlistApi";
import type { WishlistResponse } from "@/types/wishlist";
import { ROUTES } from "@/lib/constants";
import { formatPrice } from "@/lib/utils";
import { Pagination } from "@/components/shared/Pagination";

const MAX_WISHLIST_LIMIT = 20;

export function WishlistView() {
  const [page, setPage] = useState(0);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const pageSize = 12;

  const { data, isLoading, error } = useWishlistQuery(page, pageSize);
  const removeMutation = useRemoveFromWishlistMutation();

  const items = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  const handleDeleteItem = (productId: string) => {
    if (removeMutation.isPending) return;
    removeMutation.mutate(productId, {
      onSettled: () => setConfirmDeleteId(null),
    });
  };

  return (
    <div className="flex flex-col gap-6">
      {/* 1. Header Bento Card */}
      <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/30 shadow-sm relative overflow-hidden flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex flex-col gap-1 z-10">
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl sm:text-2xl font-bold text-[#0b1c30] font-heading">
              Danh sách Yêu thích
            </h1>
            <span
              className={`px-3 py-1 rounded-full text-xs font-bold border transition-colors ${
                totalElements >= MAX_WISHLIST_LIMIT
                  ? "bg-rose-50 text-rose-700 border-rose-200"
                  : "bg-blue-50 text-[#0045cc] border-blue-200"
              }`}
            >
              {totalElements}/{MAX_WISHLIST_LIMIT} sản phẩm
            </span>
          </div>
          <p className="text-xs sm:text-sm text-[#434656]">
            Lưu trữ sản phẩm quan tâm để theo dõi giá và đặt mua nhanh. (Tối đa 20 sản phẩm theo quy định sàn BR-05B)
          </p>
        </div>

        {/* Progress Bar indicator */}
        <div className="w-full sm:w-48 flex flex-col gap-1.5 z-10">
          <div className="flex justify-between text-[11px] font-medium text-[#737688]">
            <span>Sức chứa Wishlist</span>
            <span>{Math.round((totalElements / MAX_WISHLIST_LIMIT) * 100)}%</span>
          </div>
          <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                totalElements >= MAX_WISHLIST_LIMIT ? "bg-rose-500" : "bg-[#0045cc]"
              }`}
              style={{
                width: `${Math.min(100, (totalElements / MAX_WISHLIST_LIMIT) * 100)}%`,
              }}
            />
          </div>
        </div>

        {/* Decorative blur backdrop */}
        <div className="absolute -right-8 -top-8 w-40 h-40 bg-[#0045cc]/5 rounded-full pointer-events-none blur-2xl" />
      </div>

      {/* 2. Content View */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div
              key={idx}
              className="bg-white rounded-2xl p-4 border border-slate-100 shadow-sm animate-pulse flex flex-col gap-3"
            >
              <div className="w-full aspect-square bg-slate-100 rounded-xl" />
              <div className="h-4 bg-slate-100 rounded w-3/4" />
              <div className="h-4 bg-slate-100 rounded w-1/2" />
              <div className="h-9 bg-slate-100 rounded-xl mt-2" />
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-white rounded-2xl p-12 border border-slate-200 text-center flex flex-col items-center justify-center gap-3">
          <AlertCircle className="w-10 h-10 text-rose-500" />
          <h3 className="text-base font-bold text-[#0b1c30]">Không thể tải danh sách yêu thích</h3>
          <p className="text-xs text-slate-500 max-w-sm">
            Đã có lỗi xảy ra khi kết nối máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.
          </p>
        </div>
      ) : items.length === 0 ? (
        /* Empty State */
        <div className="bg-white rounded-2xl p-12 sm:p-16 border border-dashed border-slate-200 text-center flex flex-col items-center justify-center gap-4">
          <div className="w-20 h-20 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-500 mb-1">
            <Heart className="w-10 h-10 fill-rose-100 text-rose-500" />
          </div>
          <h3 className="text-lg font-bold text-[#0b1c30]">
            Danh sách yêu thích trống
          </h3>
          <p className="text-xs sm:text-sm text-[#434656] max-w-md leading-relaxed">
            Bạn chưa lưu sản phẩm nào. Hãy khám phá hàng ngàn linh kiện và thiết bị công nghệ chính hãng trên TechHub, nhấn biểu tượng trái tim để lưu lại đây!
          </p>
          <Link
            href={ROUTES.PRODUCTS}
            className="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0037a3] text-white text-xs font-bold shadow-md hover:shadow-lg transition-all"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Khám phá Sản phẩm ngay</span>
          </Link>
        </div>
      ) : (
        /* Grid of Wishlist Items */
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {items.map((item: WishlistResponse) => {
              const productUrl = `/products/${item.slug || item.productId}`;
              const formattedPrice =
                item.minPrice != null
                  ? formatPrice(item.minPrice)
                  : "Liên hệ";

              const savedDate = item.createdAt
                ? new Date(item.createdAt).toLocaleDateString("vi-VN", {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })
                : null;

              const isConfirming = confirmDeleteId === item.productId;
              const isAvailable = item.inStock && !item.isShopPaused;

              return (
                <div
                  key={item.id}
                  className="bg-white rounded-2xl p-4 border border-[#eff4ff] shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between group relative"
                >
                  {/* Top Image Box */}
                  <div className="relative w-full aspect-square rounded-xl bg-[#eff4ff] overflow-hidden mb-3 p-3 flex items-center justify-center">
                    <Link
                      href={productUrl}
                      className="absolute inset-0 flex items-center justify-center"
                    >
                      <Image
                        src={item.coverImageUrl || "/images/placeholder.png"}
                        alt={item.productName}
                        fill
                        className="object-contain p-2 group-hover:scale-105 transition-transform duration-300"
                        unoptimized
                      />
                    </Link>

                    {/* Stock & Shop Warning Badges */}
                    <div className="absolute top-2 left-2 flex flex-col gap-1 z-10 pointer-events-none">
                      {!item.inStock && (
                        <span className="px-2 py-0.5 rounded-md bg-slate-900/80 text-white text-[10px] font-bold backdrop-blur-sm">
                          Tạm hết hàng
                        </span>
                      )}
                      {item.isShopPaused && (
                        <span className="px-2 py-0.5 rounded-md bg-amber-500/90 text-white text-[10px] font-bold backdrop-blur-sm">
                          Shop tạm nghỉ
                        </span>
                      )}
                    </div>

                    {/* S2: Confirmation overlay vs Trash button */}
                    {isConfirming ? (
                      <div className="absolute inset-0 bg-slate-950/85 backdrop-blur-xs rounded-xl z-30 flex flex-col items-center justify-center p-3 text-center animate-in fade-in">
                        <p className="text-white text-xs font-semibold mb-2.5">
                          Bỏ sản phẩm khỏi yêu thích?
                        </p>
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => setConfirmDeleteId(null)}
                            className="px-2.5 py-1 text-xs rounded-lg bg-white/20 hover:bg-white/30 text-white font-medium transition-colors"
                          >
                            Hủy
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteItem(item.productId)}
                            disabled={removeMutation.isPending}
                            className="px-2.5 py-1 text-xs rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-bold flex items-center gap-1 transition-colors disabled:opacity-50"
                          >
                            {removeMutation.isPending ? (
                              <Loader2 className="w-3 h-3 animate-spin" />
                            ) : (
                              <Trash2 className="w-3 h-3" />
                            )}
                            <span>Xóa</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setConfirmDeleteId(item.productId)}
                        disabled={removeMutation.isPending}
                        className="absolute top-2 right-2 z-20 w-8 h-8 rounded-full bg-white/90 hover:bg-rose-50 text-slate-400 hover:text-rose-600 flex items-center justify-center shadow-sm transition-all hover:scale-110 cursor-pointer"
                        title="Xóa khỏi danh sách yêu thích"
                        aria-label="Xóa khỏi yêu thích"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>

                  {/* Product Details */}
                  <div className="flex flex-col flex-1">
                    <Link
                      href={productUrl}
                      className="text-xs sm:text-sm font-semibold text-[#0b1c30] line-clamp-2 hover:text-[#0045cc] transition-colors mb-2 leading-snug"
                      title={item.productName}
                    >
                      {item.productName}
                    </Link>

                    {/* S1: Bottom Action Bar */}
                    <div className="mt-auto pt-2 border-t border-slate-100 flex items-center justify-between gap-2">
                      <div className="flex flex-col min-w-0">
                        <span className="text-sm sm:text-base font-bold text-[#0b1c30] font-mono truncate">
                          {formattedPrice}
                        </span>
                        {savedDate && (
                          <span className="text-[10px] text-slate-400 truncate">
                            Đã lưu: {savedDate}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        {isAvailable ? (
                          <Link
                            href={productUrl}
                            className="px-2.5 py-1.5 rounded-xl bg-[#0045cc] hover:bg-[#0037a3] text-white text-xs font-bold transition-all flex items-center gap-1 shadow-xs hover:shadow-sm"
                            title="Chọn phiên bản & thêm vào giỏ hàng"
                          >
                            <ShoppingBag className="w-3.5 h-3.5" />
                            <span className="hidden sm:inline">Thêm giỏ</span>
                          </Link>
                        ) : (
                          <span
                            className="px-2 py-1 rounded-xl bg-slate-100 text-slate-400 text-[11px] font-medium flex items-center gap-1"
                            title={item.isShopPaused ? "Shop đang tạm nghỉ" : "Sản phẩm tạm hết hàng"}
                          >
                            <PackageX className="w-3.5 h-3.5" />
                            <span className="hidden sm:inline">
                              {item.isShopPaused ? "Shop nghỉ" : "Hết hàng"}
                            </span>
                          </span>
                        )}

                        <Link
                          href={productUrl}
                          className="p-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-semibold transition-all flex items-center justify-center"
                          title="Xem chi tiết"
                          aria-label="Xem chi tiết sản phẩm"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* 3. Pagination Controls (C4) */}
          {totalPages > 1 && (
            <div className="pt-4 border-t border-slate-200">
              <Pagination
                page={page}
                size={pageSize}
                totalElements={totalElements}
                totalPages={totalPages}
                onPageChange={setPage}
                isLoading={isLoading}
                variant="bottom-full"
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
