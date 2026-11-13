"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Store,
  BadgeCheck,
  MapPin,
  Package,
  Users,
  ExternalLink,
  UserX,
  AlertCircle,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ShoppingBag,
} from "lucide-react";
import {
  useFollowedShopsQuery,
  useUnfollowShopMutation,
} from "@/features/shops/api/shopFollowingApi";
import type { FollowedShopResponse } from "@/types/shopFollowing";
import { ROUTES } from "@/lib/constants";

export function FollowedShopsView() {
  const [page, setPage] = useState(0);
  const pageSize = 8;

  const { data, isLoading, error } = useFollowedShopsQuery(page, pageSize);
  const unfollowMutation = useUnfollowShopMutation();

  const shops = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 1;

  const handleUnfollow = (sellerName: string) => {
    if (unfollowMutation.isPending) return;
    unfollowMutation.mutate(sellerName);
  };

  const defaultBanner =
    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80";
  const defaultAvatar =
    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=200&q=80";

  return (
    <div className="flex flex-col gap-6">
      {/* 1. Header Bento Card */}
      <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/30 shadow-sm relative overflow-hidden flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex flex-col gap-1 z-10">
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl sm:text-2xl font-bold text-[#0b1c30] font-heading">
              Gian hàng đang theo dõi
            </h1>
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-[#0045cc] border border-blue-200">
              {totalElements} gian hàng
            </span>
          </div>
          <p className="text-xs sm:text-sm text-[#434656]">
            Theo dõi các thương hiệu & shop công nghệ để cập nhật sản phẩm mới và voucher ưu đãi độc quyền.
          </p>
        </div>

        {/* Decorative blur backdrop */}
        <div className="absolute -right-8 -top-8 w-40 h-40 bg-[#0045cc]/5 rounded-full pointer-events-none blur-2xl" />
      </div>

      {/* 2. Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Array.from({ length: 4 }).map((_, idx) => (
            <div
              key={idx}
              className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm animate-pulse flex flex-col gap-4"
            >
              <div className="h-24 bg-slate-100 rounded-xl" />
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-slate-100 rounded-xl" />
                <div className="flex flex-col gap-2 flex-1">
                  <div className="h-4 bg-slate-100 rounded w-1/2" />
                  <div className="h-3 bg-slate-100 rounded w-1/3" />
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-white rounded-2xl p-12 border border-slate-200 text-center flex flex-col items-center justify-center gap-3">
          <AlertCircle className="w-10 h-10 text-rose-500" />
          <h3 className="text-base font-bold text-[#0b1c30]">
            Không thể tải danh sách gian hàng
          </h3>
          <p className="text-xs text-slate-500 max-w-sm">
            Đã có lỗi xảy ra khi kết nối máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.
          </p>
        </div>
      ) : shops.length === 0 ? (
        /* Empty State */
        <div className="bg-white rounded-2xl p-12 sm:p-16 border border-dashed border-slate-200 text-center flex flex-col items-center justify-center gap-4">
          <div className="w-20 h-20 rounded-2xl bg-blue-50 flex items-center justify-center text-[#0045cc] mb-1">
            <Store className="w-10 h-10 text-[#0045cc]" />
          </div>
          <h3 className="text-lg font-bold text-[#0b1c30]">
            Bạn chưa theo dõi gian hàng nào
          </h3>
          <p className="text-xs sm:text-sm text-[#434656] max-w-md leading-relaxed">
            Khám phá các gian hàng chính hãng trên TechHub và nhấn "Theo dõi" để không bỏ lỡ các mã giảm giá và sản phẩm công nghệ hot nhất!
          </p>
          <Link
            href={ROUTES.PRODUCTS}
            className="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0037a3] text-white text-xs font-bold shadow-md hover:shadow-lg transition-all"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Khám phá Gian hàng & Sản phẩm</span>
          </Link>
        </div>
      ) : (
        /* List of Followed Shops */
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {shops.map((shop: FollowedShopResponse) => {
              const shopUrl = `/shops/${shop.sellerName}`;
              const followedDate = shop.followedAt
                ? new Date(shop.followedAt).toLocaleDateString("vi-VN", {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })
                : null;

              return (
                <div
                  key={shop.shopId}
                  className="bg-white rounded-2xl overflow-hidden border border-[#eff4ff] shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between group"
                >
                  {/* Top Cover Banner */}
                  <div className="relative w-full h-24 bg-slate-900 overflow-hidden">
                    <Image
                      src={shop.bannerUrl || defaultBanner}
                      alt={shop.shopName}
                      fill
                      className="object-cover opacity-80 group-hover:scale-105 transition-transform duration-500"
                      unoptimized
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-slate-950/60 to-transparent" />

                    {/* Status badge */}
                    <div className="absolute top-2.5 right-3 z-10">
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold tracking-wide uppercase shadow-sm flex items-center gap-1 backdrop-blur-md ${
                          shop.status === "ACTIVE"
                            ? "bg-emerald-500/90 text-white"
                            : "bg-amber-500/90 text-white"
                        }`}
                      >
                        <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                        {shop.status === "ACTIVE" ? "Đang mở cửa" : "Tạm nghỉ"}
                      </span>
                    </div>
                  </div>

                  {/* Body with overlapping avatar */}
                  <div className="p-4 pt-0 flex flex-col flex-1">
                    <div className="flex items-start justify-between gap-3 -mt-6 mb-3">
                      <div className="relative w-14 h-14 rounded-xl overflow-hidden bg-white p-1 ring-2 ring-white shadow-md shrink-0 z-10">
                        <div className="relative w-full h-full rounded-lg overflow-hidden bg-[#eff4ff]">
                          <Image
                            src={shop.avatarUrl || defaultAvatar}
                            alt={shop.shopName}
                            fill
                            className="object-cover"
                            unoptimized
                          />
                        </div>
                      </div>

                      {/* Unfollow button */}
                      <button
                        type="button"
                        onClick={() => handleUnfollow(shop.sellerName)}
                        disabled={unfollowMutation.isPending}
                        className="mt-8 px-3 py-1 rounded-xl text-xs font-semibold text-slate-500 hover:text-rose-600 hover:bg-rose-50 border border-slate-200 hover:border-rose-200 transition-colors flex items-center gap-1 cursor-pointer"
                        title="Bỏ theo dõi shop này"
                      >
                        <UserX className="w-3.5 h-3.5" />
                        <span>Bỏ theo dõi</span>
                      </button>
                    </div>

                    {/* Shop Identity */}
                    <div className="flex flex-col gap-1 mb-3">
                      <div className="flex items-center gap-1.5">
                        <Link
                          href={shopUrl}
                          className="font-bold text-sm sm:text-base text-[#0b1c30] hover:text-[#0045cc] transition-colors truncate"
                          title={shop.shopName}
                        >
                          {shop.shopName}
                        </Link>
                        <BadgeCheck className="w-4 h-4 text-[#0045cc] shrink-0" />
                      </div>
                      <span className="text-xs font-mono text-[#737688]">
                        @{shop.sellerName}
                      </span>
                    </div>

                    {/* Metrics row */}
                    <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 mb-3 pt-2 border-t border-slate-100">
                      <div className="flex items-center gap-1.5 bg-[#f8faff] px-2.5 py-1.5 rounded-lg border border-slate-100">
                        <Package className="w-3.5 h-3.5 text-[#0045cc]" />
                        <span className="font-semibold text-[#0b1c30]">
                          {shop.totalProducts}
                        </span>
                        <span className="text-[11px] text-slate-500">sản phẩm</span>
                      </div>

                      <div className="flex items-center gap-1.5 bg-[#f8faff] px-2.5 py-1.5 rounded-lg border border-slate-100">
                        <Users className="w-3.5 h-3.5 text-[#0045cc]" />
                        <span className="font-semibold text-[#0b1c30]">
                          {shop.followerCount}
                        </span>
                        <span className="text-[11px] text-slate-500">theo dõi</span>
                      </div>
                    </div>

                    {/* Footer with date & visit shop */}
                    <div className="mt-auto pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                      {followedDate ? (
                        <span className="text-[11px] text-slate-400">
                          Theo dõi: {followedDate}
                        </span>
                      ) : (
                        <span />
                      )}

                      <Link
                        href={shopUrl}
                        className="inline-flex items-center gap-1 text-xs font-bold text-[#0045cc] hover:underline"
                      >
                        <span>Truy cập Shop</span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* 3. Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t border-slate-200 text-xs text-slate-600">
              <span>
                Hiển thị {page * pageSize + 1} -{" "}
                {Math.min((page + 1) * pageSize, totalElements)} trên {totalElements} gian hàng
              </span>

              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-medium"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                  <span>Trước</span>
                </button>

                <span className="px-3 py-1.5 font-semibold text-[#0b1c30]">
                  Trang {page + 1} / {totalPages}
                </span>

                <button
                  type="button"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-medium"
                >
                  <span>Sau</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
