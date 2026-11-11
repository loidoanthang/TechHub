"use client";

import React, { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { formatCurrencyVND, parseCurrencyVND } from "@/lib/utils";
import {
  Truck,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
  Coins,
  PackageCheck,
  Sparkles,
} from "lucide-react";
import {
  shippingOptionSchema,
  type ShippingOptionFormValues,
} from "../../schemas/settingsSchema";
import {
  useShippingOptionQuery,
  useUpdateShippingOptionMutation,
} from "../../hooks/useSellerQueries";

// Gợi ý mức cước phí tiêu chuẩn phổ biến cho hàng linh kiện & thiết bị điện tử
const PRESET_FEES = [
  { label: "30.000 ₫", value: 30000, desc: "Cơ bản / Nội tỉnh" },
  { label: "35.000 ₫", value: 35000, desc: "Phổ biến toàn quốc" },
  { label: "40.000 ₫", value: 40000, desc: "Kiện lớn / Bảo hiểm" },
  { label: "50.000 ₫", value: 50000, desc: "Hàng cồng kềnh" },
];

export function ShippingOptionsTab() {
  const { data: shippingOption, isLoading, isError, refetch } = useShippingOptionQuery();
  const updateShippingMutation = useUpdateShippingOptionMutation();

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
    control,
    formState: { errors, isDirty },
  } = useForm<ShippingOptionFormValues>({
    resolver: zodResolver(shippingOptionSchema),
    defaultValues: {
      shippingFee: 30000,
    },
  });

  // Sync shipping option data when fetched
  useEffect(() => {
    if (shippingOption) {
      reset({
        shippingFee: Number(shippingOption.shippingFee) || 0,
      });
    }
  }, [shippingOption, reset]);

  const watchedFee = watch("shippingFee");

  const onSubmit = async (values: ShippingOptionFormValues) => {
    setFeedback(null);
    try {
      await updateShippingMutation.mutateAsync({
        shippingFee: values.shippingFee,
      });
      setFeedback({
        type: "success",
        message: "Đã cập nhật mức phí vận chuyển tiêu chuẩn thành công!",
      });
      reset(values);
    } catch (err: unknown) {
      console.error("Update shipping fee error:", err);
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
          Đang tải cấu hình vận chuyển...
        </span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
        <AlertCircle className="w-8 h-8 text-rose-500" />
        <p className="text-sm font-semibold text-slate-800">
          Không thể tải thông tin cấu hình vận chuyển
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
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-[#0b1c30]">
                Cấu hình Biểu phí Vận chuyển
              </h2>
              <p className="text-xs text-slate-500">
                Thiết lập mức phí giao hàng tiêu chuẩn cho các đơn hàng phát sinh tại gian hàng.
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

        {/* Active Shipping Method Card */}
        <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white border border-slate-200 text-[#0045cc] flex items-center justify-center shadow-2xs">
              <PackageCheck className="w-5 h-5" />
            </div>
            <div className="flex flex-col">
              <span className="text-xs font-bold text-slate-900">
                {shippingOption?.name || "Giao hàng tiêu chuẩn (Standard Delivery)"}
              </span>
              <span className="text-[11px] text-slate-500">
                Áp dụng mặc định cho toàn bộ danh mục sản phẩm của shop
              </span>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
            Đang hoạt động
          </span>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
          {/* Mức phí vận chuyển tiêu chuẩn */}
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-slate-700 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <Coins className="w-3.5 h-3.5 text-slate-500" />
                <span>Mức phí vận chuyển tiêu chuẩn (VND)</span>
                <span className="text-rose-500">*</span>
              </span>
              <span className="text-xs font-mono font-bold text-[#0045cc]">
                Hiển thị: {new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(watchedFee) || 0)}
              </span>
            </label>

            <div className="relative">
              <Controller
                name="shippingFee"
                control={control}
                render={({ field }) => (
                  <input
                    type="text"
                    inputMode="numeric"
                    value={formatCurrencyVND(field.value, true)}
                    onChange={(e) => {
                      const num = parseCurrencyVND(e.target.value);
                      field.onChange(num);
                    }}
                    placeholder="VD: 30.000"
                    className={`w-full h-11 px-3.5 pr-12 rounded-xl border text-sm font-bold font-mono text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                      errors.shippingFee
                        ? "border-rose-400 bg-rose-50/20"
                        : "border-slate-200 bg-white"
                    }`}
                  />
                )}
              />
              <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 select-none pointer-events-none">
                VNĐ
              </span>
            </div>

            {/* Quick preset chips */}
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className="text-[11px] text-slate-400 flex items-center gap-1">
                Mức cước mẫu:
              </span>
              {PRESET_FEES.map((preset) => (
                <button
                  key={preset.value}
                  type="button"
                  onClick={() => setValue("shippingFee", preset.value, { shouldDirty: true })}
                  className="px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-slate-100 hover:bg-[#eff4ff] hover:text-[#0045cc] text-slate-700 transition-colors border border-slate-200"
                >
                  <span>{preset.label}</span>
                  <span className="text-[9px] text-slate-400 ml-1">({preset.desc})</span>
                </button>
              ))}
            </div>

            {errors.shippingFee && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.shippingFee.message}
              </p>
            )}
          </div>

          {/* Action Footer */}
          <div className="border-t border-slate-100 pt-4 flex items-center justify-between">
            <span className="text-[11px] text-slate-400">
              {isDirty ? "Có thay đổi chưa lưu" : "Biểu phí đã được cập nhật"}
            </span>
            <button
              type="submit"
              disabled={updateShippingMutation.isPending || !isDirty}
              className="h-10 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-xs font-bold transition-all shadow-xs flex items-center gap-2"
            >
              {updateShippingMutation.isPending && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              <span>Lưu biểu phí vận chuyển</span>
            </button>
          </div>
        </form>
      </div>

      {/* RIGHT INFORMATION COLUMN */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        {/* Shipping Policy Card */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-800 border-b border-slate-100 pb-3">
            <ShieldCheck className="w-4 h-4 text-[#0045cc]" />
            <span>Chính sách Giao nhận Thiết bị Công nghệ</span>
          </div>

          <div className="flex flex-col gap-3 text-xs text-slate-600 leading-relaxed">
            <div className="flex items-start gap-2.5">
              <span className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                1
              </span>
              <span>
                <strong>Đồng giá vận chuyển:</strong> Mức phí bạn cài đặt sẽ áp dụng thống nhất cho khách hàng khi họ đặt mua sản phẩm của shop bạn trong giỏ hàng.
              </span>
            </div>

            <div className="flex items-start gap-2.5">
              <span className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                2
              </span>
              <span>
                <strong>Bảo hiểm hàng dễ vỡ:</strong> Hệ thống đối tác logistics của TechHub cam kết bồi thường 100% giá trị linh kiện điện tử nếu xảy ra móp méo, rơi vỡ trong quá trình vận chuyển.
              </span>
            </div>

            <div className="flex items-start gap-2.5">
              <span className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                3
              </span>
              <span>
                <strong>Đồng kiểm khi nhận hàng:</strong> Khách hàng được phép mở hộp kiểm tra ngoại quan cùng nhân viên bưu tá trước khi nhận hàng.
              </span>
            </div>
          </div>
        </div>

        {/* Free Shipping Tip */}
        <div className="bg-[#eff4ff]/60 border border-[#0045cc]/15 rounded-2xl p-4 flex items-start gap-3">
          <Coins className="w-5 h-5 text-[#0045cc] shrink-0 mt-0.5" />
          <div className="flex flex-col gap-1 text-xs">
            <span className="font-bold text-[#0b1c30]">
              Tối ưu phí vận chuyển
            </span>
            <span className="text-slate-600 leading-relaxed text-[11px]">
              Để kích thích sức mua, bạn có thể tạo thêm các <strong>Mã giảm giá Shop (Voucher)</strong> tại mục Quản lý Mã Giảm Giá để hỗ trợ một phần cước phí ship cho khách hàng thân thiết.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
