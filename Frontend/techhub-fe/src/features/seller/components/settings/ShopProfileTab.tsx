"use client";

import React, { useState, useEffect } from "react";
import Image from "next/image";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { UserAvatar } from "@/components/shared/UserAvatar";
import {
  Store,
  Image as ImageIcon,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ExternalLink,
  ShieldCheck,
  Star,
} from "lucide-react";
import {
  shopProfileSchema,
  type ShopProfileFormValues,
} from "../../schemas/settingsSchema";
import {
  useSellerShopQuery,
  useUpdateSellerShopMutation,
} from "../../hooks/useSellerQueries";

// Preset curated avatar and banner images for electronics sellers
const PRESET_AVATARS = [
  {
    name: "TechHub Official",
    url: "/images/default-avatar.svg",
  },
];

const PRESET_BANNERS = [
  {
    name: "TechHub Hardware Banner",
    url: "/images/sample-evidence.svg",
  },
];

export function ShopProfileTab() {
  const { data: shop, isLoading, isError, refetch } = useSellerShopQuery();
  const updateShopMutation = useUpdateSellerShopMutation();

  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<ShopProfileFormValues>({
    resolver: zodResolver(shopProfileSchema),
    defaultValues: {
      name: "",
      description: "",
      avatarUrl: "",
      bannerUrl: "",
    },
  });

  // Sync shop data when fetched
  useEffect(() => {
    if (shop) {
      reset({
        name: shop.name || shop.shopName || "",
        description: shop.description || "",
        avatarUrl: shop.avatarUrl || "",
        bannerUrl: shop.bannerUrl || "",
      });
    }
  }, [shop, reset]);

  // Watch for live preview
  const watchedName = watch("name");
  const watchedDescription = watch("description");
  const watchedAvatar = watch("avatarUrl");
  const watchedBanner = watch("bannerUrl");

  const onSubmit = async (values: ShopProfileFormValues) => {
    setFeedback(null);
    try {
      await updateShopMutation.mutateAsync({
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        avatarUrl: values.avatarUrl?.trim() || undefined,
        bannerUrl: values.bannerUrl?.trim() || undefined,
      });
      setFeedback({
        type: "success",
        message: "Đã cập nhật hồ sơ gian hàng thành công!",
      });
      // Re-reset form state dirty flag with new values
      reset(values);
    } catch (err: unknown) {
      console.error("Update shop error:", err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật thất bại. Vui lòng thử lại sau.";
      setFeedback({ type: "error", message: msg });
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200/80 p-8 flex flex-col items-center justify-center min-h-[360px] gap-3">
        <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
        <span className="text-xs text-slate-500 font-medium">
          Đang tải thông tin gian hàng...
        </span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
        <AlertCircle className="w-8 h-8 text-rose-500" />
        <p className="text-sm font-semibold text-slate-800">
          Không thể tải dữ liệu hồ sơ gian hàng
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          className="px-4 py-2 text-xs font-bold text-white bg-[#0045cc] rounded-xl hover:bg-[#0d5bff] transition-colors"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* LEFT FORM COLUMN */}
      <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 flex flex-col gap-6">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
              <Store className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-[#0b1c30]">
                Thông tin Hồ sơ & Thương hiệu
              </h2>
              <p className="text-xs text-slate-500">
                Hiển thị công khai với khách hàng khi truy cập gian hàng của bạn.
              </p>
            </div>
          </div>
        </div>

        {/* Feedback Alert */}
        {feedback && (
          <div
            className={`p-3.5 rounded-xl text-xs flex items-center gap-2.5 border animate-in fade-in ${
              feedback.type === "success"
                ? "bg-emerald-50 border-emerald-200 text-emerald-800"
                : "bg-rose-50 border-rose-200 text-rose-800"
            }`}
          >
            {feedback.type === "success" ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span className="font-medium">{feedback.message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
          {/* Tên gian hàng */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
              <span>Tên gian hàng</span>
              <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              {...register("name")}
              placeholder="VD: ASUS Official Store, TechHub Studio..."
              className={`w-full h-10 px-3.5 rounded-xl border text-xs font-semibold text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                errors.name
                  ? "border-rose-400 bg-rose-50/20"
                  : "border-slate-200 bg-white"
              }`}
            />
            {errors.name ? (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.name.message}
              </p>
            ) : (
              <span className="text-[11px] text-slate-400">
                Độ dài từ 2 đến 200 ký tự. Tên gian hàng sẽ đại diện trên toàn sàn.
              </span>
            )}
          </div>

          {/* Giới thiệu / Slogan */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">
                Giới thiệu gian hàng / Slogan
              </label>
              <span className="text-[11px] text-slate-400">
                {watchedDescription?.length || 0}/1000
              </span>
            </div>
            <textarea
              rows={3}
              {...register("description")}
              placeholder="Mô tả ngắn gọn về thế mạnh, chính sách bảo hành, cam kết chính hãng của gian hàng bạn..."
              className={`w-full p-3 rounded-xl border text-xs text-slate-800 resize-none transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                errors.description
                  ? "border-rose-400 bg-rose-50/20"
                  : "border-slate-200 bg-white"
              }`}
            />
            {errors.description && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Ảnh đại diện (Avatar URL) */}
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-slate-700 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <ImageIcon className="w-3.5 h-3.5 text-slate-500" />
                <span>URL Ảnh đại diện (Logo Shop)</span>
              </span>
              <span className="text-[11px] font-normal text-slate-400">
                Tỉ lệ 1:1 (khuyên dùng 200x200px)
              </span>
            </label>
            <input
              type="url"
              {...register("avatarUrl")}
              placeholder="https://example.com/logo.png hoặc link ảnh online"
              className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                errors.avatarUrl
                  ? "border-rose-400 bg-rose-50/20"
                  : "border-slate-200 bg-white"
              }`}
            />
            {/* Quick avatar presets */}
            <div className="flex flex-wrap items-center gap-2 pt-0.5">
              <span className="text-[11px] text-slate-400 flex items-center gap-1">
                Mẫu gợi ý:
              </span>
              {PRESET_AVATARS.map((p) => (
                <button
                  key={p.name}
                  type="button"
                  onClick={() => setValue("avatarUrl", p.url, { shouldDirty: true })}
                  className="text-[10px] px-2 py-0.5 rounded-md bg-slate-100 hover:bg-[#eff4ff] hover:text-[#0045cc] text-slate-600 transition-colors border border-slate-200"
                >
                  {p.name}
                </button>
              ))}
            </div>
            {errors.avatarUrl && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.avatarUrl.message}
              </p>
            )}
          </div>

          {/* Ảnh bìa (Banner URL) */}
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-slate-700 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <ImageIcon className="w-3.5 h-3.5 text-slate-500" />
                <span>URL Ảnh bìa (Cover Banner)</span>
              </span>
              <span className="text-[11px] font-normal text-slate-400">
                Tỉ lệ 3:1 hoặc 16:9 (khuyên dùng 1200x400px)
              </span>
            </label>
            <input
              type="url"
              {...register("bannerUrl")}
              placeholder="https://example.com/banner.jpg hoặc link ảnh bìa banner"
              className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                errors.bannerUrl
                  ? "border-rose-400 bg-rose-50/20"
                  : "border-slate-200 bg-white"
              }`}
            />
            {/* Quick banner presets */}
            <div className="flex flex-wrap items-center gap-2 pt-0.5">
              <span className="text-[11px] text-slate-400 flex items-center gap-1">
                Mẫu gợi ý:
              </span>
              {PRESET_BANNERS.map((p) => (
                <button
                  key={p.name}
                  type="button"
                  onClick={() => setValue("bannerUrl", p.url, { shouldDirty: true })}
                  className="text-[10px] px-2 py-0.5 rounded-md bg-slate-100 hover:bg-[#eff4ff] hover:text-[#0045cc] text-slate-600 transition-colors border border-slate-200"
                >
                  {p.name}
                </button>
              ))}
            </div>
            {errors.bannerUrl && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.bannerUrl.message}
              </p>
            )}
          </div>

          {/* Action Footer */}
          <div className="border-t border-slate-100 pt-4 flex items-center justify-between">
            <span className="text-[11px] text-slate-400">
              {isDirty ? "Có thay đổi chưa lưu" : "Dữ liệu đã được đồng bộ"}
            </span>
            <button
              type="submit"
              disabled={updateShopMutation.isPending || !isDirty}
              className="h-10 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-xs font-bold transition-all shadow-xs flex items-center gap-2"
            >
              {updateShopMutation.isPending && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              <span>Lưu thay đổi hồ sơ</span>
            </button>
          </div>
        </form>
      </div>

      {/* RIGHT PREVIEW COLUMN */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
          <span>Xem trước Gian hàng Thực tế (Live Preview)</span>
        </div>

        {/* Live Shop Card */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden flex flex-col">
          {/* Banner container */}
          <div className="relative h-32 sm:h-36 w-full bg-slate-800 overflow-hidden">
            {watchedBanner ? (
              <Image
                src={watchedBanner}
                alt="Banner preview"
                fill
                className="object-cover transition-opacity duration-300"
                unoptimized
              />
            ) : (
              <div className="w-full h-full bg-linear-to-r from-[#0045cc] to-[#002b80] flex items-center justify-center text-white/50 text-xs">
                Chưa có ảnh bìa
              </div>
            )}
            <div className="absolute inset-0 bg-linear-to-t from-black/70 via-black/20 to-transparent" />
            
            <div className="absolute top-3 right-3">
              <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold tracking-wide uppercase bg-emerald-500/90 text-white backdrop-blur-xs flex items-center gap-1 shadow-2xs">
                <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                Đang mở cửa
              </span>
            </div>
          </div>

          {/* Content container */}
          <div className="p-4 sm:p-5 flex flex-col gap-3 relative -mt-8 pt-0">
            {/* Avatar & Header */}
            <div className="flex items-end gap-3.5">
              <div className="w-16 h-16 rounded-2xl bg-white p-1 border-2 border-white shadow-md shrink-0 overflow-hidden flex items-center justify-center">
                {watchedAvatar ? (
                  <UserAvatar
                    src={watchedAvatar}
                    alt={watchedName || "Shop avatar"}
                    size={56}
                    className="rounded-xl"
                    fallbackSrc="/images/default-avatar.svg"
                  />
                ) : (
                  <span className="text-xl font-heading font-black text-[#0045cc]">
                    {(watchedName || "S").charAt(0).toUpperCase()}
                  </span>
                )}
              </div>

              <div className="flex-1 min-w-0 pb-1">
                <h3 className="font-heading font-bold text-sm sm:text-base text-slate-900 truncate">
                  {watchedName || "Tên gian hàng mẫu"}
                </h3>
                <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
                  <span className="text-amber-500 flex items-center">
                    <Star className="w-3 h-3 fill-amber-400 text-amber-400 inline mr-0.5" />
                    5.0
                  </span>
                  <span>•</span>
                  <span className="text-[#0045cc] font-medium flex items-center gap-0.5">
                    <ShieldCheck className="w-3 h-3 inline" />
                    Chính hãng
                  </span>
                </div>
              </div>
            </div>

            {/* Description */}
            <p className="text-xs text-slate-600 line-clamp-3 leading-relaxed bg-slate-50/70 p-3 rounded-xl border border-slate-100">
              {watchedDescription ||
                "Gian hàng điện tử chính hãng cam kết cung cấp sản phẩm công nghệ chất lượng cao, bảo hành uy tín và hỗ trợ giao nhanh toàn quốc."}
            </p>

            {/* Footer dummy action */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
              <span className="text-[11px] text-slate-400 font-medium">
                URL: /shops/{shop?.sellerName || "your-shop"}
              </span>
              <span className="text-xs font-bold text-[#0045cc] flex items-center gap-1">
                <span>Xem gian hàng</span>
                <ExternalLink className="w-3 h-3" />
              </span>
            </div>
          </div>
        </div>

        {/* Pro Tip Card */}
        <div className="bg-[#eff4ff]/60 border border-[#0045cc]/15 rounded-2xl p-4 flex items-start gap-3">
          <ShieldCheck className="w-5 h-5 text-[#0045cc] shrink-0 mt-0.5" />
          <div className="flex flex-col gap-1 text-xs">
            <span className="font-bold text-[#0b1c30]">
              Tối ưu hình ảnh nhận diện
            </span>
            <span className="text-slate-600 leading-relaxed text-[11px]">
              Sử dụng ảnh đại diện logo sắc nét và ảnh bìa thể hiện không gian công nghệ hoặc dòng sản phẩm chủ đạo để gia tăng tỉ lệ chuyển đổi đơn hàng lên tới 25%.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
