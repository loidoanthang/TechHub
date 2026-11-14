"use client";

import { useState } from "react";
import Image from "next/image";
import {
  BadgeCheck,
  MapPin,
  Calendar,
  ShieldCheck,
  Package,
  UserPlus,
  UserCheck,
  Users,
  MessageCircle,
} from "lucide-react";
import type { PublicShopResponse } from "../types/shop";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useChat } from "@/features/chat";
import { useSellerShopQuery } from "@/features/seller/hooks/useSellerQueries";
import {
  useFollowedShopSlugsQuery,
  useFollowShopMutation,
  useUnfollowShopMutation,
} from "../api/shopFollowingApi";
import { toast } from "@/lib/toast";

interface ShopHeaderBannerProps {
  shop: PublicShopResponse;
  productCount?: number;
}

export function ShopHeaderBanner({ shop, productCount = 0 }: ShopHeaderBannerProps) {
  const { user, isAuthenticated } = useAuth();
  const { openChatWithSeller } = useChat();
  const { data: followedData } = useFollowedShopSlugsQuery();
  const followMutation = useFollowShopMutation();
  const unfollowMutation = useUnfollowShopMutation();

  const isFollowing = !!(
    shop.sellerName &&
    followedData?.slugs.has(shop.sellerName.toLowerCase())
  );
  const isPending = followMutation.isPending || unfollowMutation.isPending;

  // Track live follower count when mutation succeeds
  const [liveFollowerCount, setLiveFollowerCount] = useState<number | null>(null);

  // BR-23 Anti-Self-Follow: Check if current user owns this shop
  const isSeller = !!(isAuthenticated && user?.roles?.includes("SELLER"));
  const { data: sellerShop } = useSellerShopQuery({ enabled: isSeller });
  const isOwnShop =
    isSeller &&
    sellerShop?.sellerName?.toLowerCase() === shop.sellerName?.toLowerCase();

  const handleToggleFollow = () => {
    if (!isAuthenticated) {
      toast.info("Vui lòng đăng nhập để theo dõi gian hàng này");
      return;
    }
    if (isOwnShop) {
      toast.error("Bạn không thể tự theo dõi gian hàng của chính mình (BR-23)");
      return;
    }
    if (!shop.sellerName) return;

    if (isFollowing) {
      unfollowMutation.mutate(shop.sellerName, {
        onSuccess: () => {
          setLiveFollowerCount((prev) => (prev !== null && prev > 0 ? prev - 1 : null));
        },
      });
    } else {
      followMutation.mutate(shop.sellerName, {
        onSuccess: (res) => {
          if (res?.data?.followerCount != null) {
            setLiveFollowerCount(res.data.followerCount);
          } else {
            setLiveFollowerCount((prev) => (prev ?? 0) + 1);
          }
        },
      });
    }
  };

  const handleChatWithShop = async () => {
    if (!isAuthenticated) {
      toast.info("Vui lòng đăng nhập để chat với shop");
      return;
    }
    if (isOwnShop) {
      toast.error("Bạn không thể tự chat với gian hàng của chính mình");
      return;
    }
    if (!shop.sellerUserId) {
      toast.error("Không thể xác định người bán của gian hàng này");
      return;
    }
    await openChatWithSeller(shop.sellerUserId);
  };

  const joinedDate = shop.createdAt
    ? new Date(shop.createdAt).toLocaleDateString("vi-VN", {
        year: "numeric",
        month: "long",
      })
    : null;

  const warehouseLocation = [shop.warehouseDistrict, shop.warehouseProvince]
    .filter(Boolean)
    .join(", ");

  const defaultAvatar = "/images/default-avatar.svg";

  return (
    <div className="w-full bg-white rounded-3xl overflow-hidden border border-[#c3c5d9]/30 shadow-sm transition-all flex flex-col">
      {/* 1. Cover Banner Image */}
      <div className="relative w-full h-44 sm:h-56 md:h-64 bg-slate-900 overflow-hidden">
        {shop.bannerUrl ? (
          <Image
            src={shop.bannerUrl}
            alt={shop.name}
            fill
            priority
            className="object-cover opacity-90 hover:scale-105 transition-transform duration-700 ease-out"
            unoptimized
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-950 opacity-95" />
        )}
        {/* Subtle cinematic gradient */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/70 via-slate-900/10 to-transparent" />

        {/* Status Pill on Top-Right */}
        <div className="absolute top-4 right-4 sm:top-5 sm:right-6 z-10 flex items-center gap-2">
          <span
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold tracking-wide uppercase shadow-md flex items-center gap-1.5 backdrop-blur-md ${
              shop.status === "ACTIVE"
                ? "bg-emerald-500/90 text-white"
                : shop.status === "PAUSED"
                ? "bg-amber-500/90 text-white"
                : "bg-slate-600/90 text-white"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-white animate-pulse" />
            {shop.status === "ACTIVE"
              ? "Đang mở cửa"
              : shop.status === "PAUSED"
              ? "Tạm nghỉ"
              : "Ngừng hoạt động"}
          </span>
        </div>
      </div>

      {/* 2. Shop Identity & Metrics */}
      <div className="px-6 sm:px-8 pb-6 pt-0 relative flex flex-col gap-6">
        <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
          {/* Left: Avatar Overlapping Banner Border + Shop Name & Meta */}
          <div className="flex flex-col sm:flex-row items-start gap-4 sm:gap-5">
            {/* Avatar Ring */}
            <div className="-mt-12 sm:-mt-16 relative w-24 h-24 sm:w-28 sm:h-28 rounded-2xl overflow-hidden bg-white p-1.5 ring-4 ring-white shadow-xl shrink-0 z-10">
              <div className="relative w-full h-full rounded-xl overflow-hidden bg-[#eff4ff]">
                <Image
                  src={shop.avatarUrl || defaultAvatar}
                  alt={shop.name}
                  fill
                  className="object-cover"
                  unoptimized
                />
              </div>
            </div>

            {/* Shop Name & Details */}
            <div className="pt-2 sm:pt-4 flex flex-col gap-1.5">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-[#0b1c30] font-heading leading-tight tracking-tight">
                  {shop.name}
                </h1>
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#0045cc]/10 text-[#0045cc] text-xs font-bold border border-[#0045cc]/20">
                  <BadgeCheck className="w-4 h-4 text-[#0045cc]" />
                  <span>Chính hãng TechHub</span>
                </span>

                {/* Follow Button */}
                {!isOwnShop ? (
                  <button
                    type="button"
                    onClick={handleToggleFollow}
                    disabled={isPending}
                    className={`ml-1 px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all duration-200 flex items-center gap-1.5 shadow-sm active:scale-95 cursor-pointer ${
                      isFollowing
                        ? "bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-600 border border-slate-200 hover:border-rose-200"
                        : "bg-[#0045cc] hover:bg-[#0037a3] text-white shadow-blue-200 hover:shadow-md"
                    }`}
                  >
                    {isFollowing ? (
                      <>
                        <UserCheck className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Đang theo dõi</span>
                      </>
                    ) : (
                      <>
                        <UserPlus className="w-3.5 h-3.5" />
                        <span>Theo dõi Shop</span>
                      </>
                    )}
                  </button>
                ) : (
                  <span className="ml-1 px-3 py-1 rounded-xl text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
                    Gian hàng của bạn
                  </span>
                )}

                {/* Chat Button */}
                {!isOwnShop && shop.sellerUserId ? (
                  <button
                    type="button"
                    onClick={handleChatWithShop}
                    className="ml-1 px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all duration-200 flex items-center gap-1.5 bg-white hover:bg-[#eff4ff] text-[#0045cc] border border-[#0045cc]/30 shadow-sm active:scale-95 cursor-pointer"
                  >
                    <MessageCircle className="w-3.5 h-3.5" />
                    <span>Chat với Shop</span>
                  </button>
                ) : null}
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs font-mono font-medium text-[#737688]">
                  @{shop.sellerName}
                </span>
              </div>

              {shop.description && (
                <p className="text-xs sm:text-sm text-[#434656] max-w-xl line-clamp-2 leading-relaxed mt-0.5">
                  {shop.description}
                </p>
              )}
            </div>
          </div>

          {/* Right: Bento Metrics / Stats Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:flex lg:flex-wrap items-center gap-2.5 sm:gap-3 text-xs pt-2 lg:pt-4 border-t lg:border-t-0 border-slate-100">
            {warehouseLocation && (
              <div
                className="flex items-center gap-2.5 bg-[#f8faff] hover:bg-[#eff4ff] px-3.5 py-2 rounded-xl border border-[#c3c5d9]/30 transition-colors"
                title={warehouseLocation}
              >
                <div className="w-8 h-8 rounded-lg bg-[#0045cc]/10 flex items-center justify-center shrink-0">
                  <MapPin className="w-4 h-4 text-[#0045cc]" />
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-[10px] text-[#737688] font-medium leading-none">Kho hàng</span>
                  <span className="text-xs font-semibold text-[#0b1c30] truncate max-w-[130px] sm:max-w-[160px] mt-1">
                    {warehouseLocation}
                  </span>
                </div>
              </div>
            )}

            {liveFollowerCount !== null && (
              <div className="flex items-center gap-2.5 bg-[#f8faff] hover:bg-[#eff4ff] px-3.5 py-2 rounded-xl border border-[#c3c5d9]/30 transition-colors">
                <div className="w-8 h-8 rounded-lg bg-[#0045cc]/10 flex items-center justify-center shrink-0">
                  <Users className="w-4 h-4 text-[#0045cc]" />
                </div>
                <div className="flex flex-col">
                  <span className="text-[10px] text-[#737688] font-medium leading-none">Người theo dõi</span>
                  <span className="text-xs font-semibold text-[#0b1c30] mt-1 font-mono">
                    {liveFollowerCount}
                  </span>
                </div>
              </div>
            )}

            {joinedDate && (
              <div className="flex items-center gap-2.5 bg-[#f8faff] hover:bg-[#eff4ff] px-3.5 py-2 rounded-xl border border-[#c3c5d9]/30 transition-colors">
                <div className="w-8 h-8 rounded-lg bg-[#0045cc]/10 flex items-center justify-center shrink-0">
                  <Calendar className="w-4 h-4 text-[#0045cc]" />
                </div>
                <div className="flex flex-col">
                  <span className="text-[10px] text-[#737688] font-medium leading-none">Tham gia</span>
                  <span className="text-xs font-semibold text-[#0b1c30] mt-1">
                    {joinedDate}
                  </span>
                </div>
              </div>
            )}

            <div className="flex items-center gap-2.5 bg-[#f8faff] hover:bg-[#eff4ff] px-3.5 py-2 rounded-xl border border-[#c3c5d9]/30 transition-colors">
              <div className="w-8 h-8 rounded-lg bg-[#0045cc]/10 flex items-center justify-center shrink-0">
                <Package className="w-4 h-4 text-[#0045cc]" />
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] text-[#737688] font-medium leading-none">Sản phẩm</span>
                <span className="text-xs font-semibold text-[#0b1c30] mt-1">
                  {productCount}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2.5 bg-emerald-50/80 hover:bg-emerald-50 px-3.5 py-2 rounded-xl border border-emerald-200 text-emerald-800 transition-colors">
              <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-4 h-4 text-emerald-600" />
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] text-emerald-600 font-medium leading-none">Cam kết</span>
                <span className="text-xs font-semibold text-emerald-800 mt-1">
                  100% Real
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
