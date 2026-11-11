"use client";

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Landmark,
  CreditCard,
  User,
  Globe,
  CheckCircle2,
  Clock,
  AlertCircle,
  Loader2,
  ShieldCheck,
  CalendarDays,
} from "lucide-react";
import {
  payoutProfileSchema,
  type PayoutProfileFormValues,
} from "../../schemas/settingsSchema";
import {
  useSellerProfileQuery,
  useUpdateSellerProfileMutation,
} from "../../hooks/useSellerQueries";
import type { SellerVerificationStatus } from "@/types/enums";

// Danh sách các ngân hàng thương mại phổ biến tại Việt Nam
const VIETNAM_BANKS = [
  "Vietcombank (VCB) - Ngân hàng Ngoại Thương",
  "Techcombank (TCB) - Ngân hàng Kỹ Thương",
  "MBBank (MB) - Ngân hàng Quân Đội",
  "BIDV - Ngân hàng Đầu tư và Phát triển Việt Nam",
  "VietinBank (CTG) - Ngân hàng Công Thương",
  "VPBank (VPB) - Ngân hàng Việt Nam Thịnh Vượng",
  "ACB - Ngân hàng Á Châu",
  "TPBank (TPB) - Ngân hàng Tiên Phong",
  "Sacombank (STB) - Ngân hàng Sài Gòn Thương Tín",
  "Agribank - Ngân hàng Nông nghiệp & PT Nông thôn",
  "HDBank - Ngân hàng Phát triển TP.HCM",
  "VIB - Ngân hàng Quốc Tế",
  "SHB - Ngân hàng Sài Gòn - Hà Nội",
  "MSB - Ngân hàng Hàng Hải",
  "SeABank - Ngân hàng Đông Nam Á",
  "OCB - Ngân hàng Phương Đông",
];

export function PayoutProfileTab() {
  const { data: profile, isLoading, isError, refetch } = useSellerProfileQuery();
  const updateProfileMutation = useUpdateSellerProfileMutation();

  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<PayoutProfileFormValues>({
    resolver: zodResolver(payoutProfileSchema),
    defaultValues: {
      sellerName: "",
      bankName: "",
      bankAccountNumber: "",
      bankAccountHolder: "",
    },
  });

  // Sync profile data when fetched
  useEffect(() => {
    if (profile) {
      reset({
        sellerName: profile.sellerName || "",
        bankName: profile.bankName || "",
        bankAccountNumber: profile.bankAccountNumber || "",
        bankAccountHolder: profile.bankAccountHolder || "",
      });
    }
  }, [profile, reset]);

  const watchedSellerName = watch("sellerName");

  const onSubmit = async (values: PayoutProfileFormValues) => {
    setFeedback(null);
    try {
      await updateProfileMutation.mutateAsync({
        sellerName: values.sellerName.trim(),
        bankName: values.bankName.trim(),
        bankAccountNumber: values.bankAccountNumber.trim(),
        bankAccountHolder: values.bankAccountHolder.trim().toUpperCase(),
      });
      setFeedback({
        type: "success",
        message: "Đã cập nhật thông tin định danh và tài khoản thụ hưởng thành công!",
      });
      reset(values);
    } catch (err: unknown) {
      console.error("Update profile error:", err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật thất bại. Vui lòng kiểm tra lại.";
      setFeedback({ type: "error", message: msg });
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200/80 p-8 flex flex-col items-center justify-center min-h-[360px] gap-3">
        <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
        <span className="text-xs text-slate-500 font-medium">
          Đang tải thông tin tài khoản thụ hưởng...
        </span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
        <AlertCircle className="w-8 h-8 text-rose-500" />
        <p className="text-sm font-semibold text-slate-800">
          Không thể tải thông tin hồ sơ người bán
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
              <Landmark className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-[#0b1c30]">
                Tài khoản Ngân hàng & Quyết toán
              </h2>
              <p className="text-xs text-slate-500">
                Thông tin nhận tiền thanh toán doanh thu từ các đơn hàng thành công trên sàn TechHub.
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

        {/* Verification Status Banner */}
        <div
          className={`p-4 rounded-xl border flex items-center justify-between ${
            profile?.verificationStatus === "APPROVED"
              ? "bg-emerald-50/70 border-emerald-200 text-emerald-900"
              : profile?.verificationStatus === "PENDING"
              ? "bg-amber-50/70 border-amber-200 text-amber-900"
              : "bg-rose-50/70 border-rose-200 text-rose-900"
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                profile?.verificationStatus === "APPROVED"
                  ? "bg-emerald-100 text-emerald-700"
                  : profile?.verificationStatus === "PENDING"
                  ? "bg-amber-100 text-amber-700"
                  : "bg-rose-100 text-rose-700"
              }`}
            >
              {profile?.verificationStatus === "APPROVED" ? (
                <ShieldCheck className="w-5 h-5" />
              ) : profile?.verificationStatus === "PENDING" ? (
                <Clock className="w-5 h-5" />
              ) : (
                <AlertCircle className="w-5 h-5" />
              )}
            </div>
            <div className="flex flex-col">
              <span className="text-xs font-bold">
                Trạng thái thẩm định Đối tác Bán hàng
              </span>
              <span className="text-[11px] opacity-80">
                {profile?.verificationStatus === "APPROVED"
                  ? "Hồ sơ đối tác đã được Ban quản trị phê duyệt chính thức."
                  : profile?.verificationStatus === "PENDING"
                  ? "Hồ sơ đang trong quá trình xét duyệt của Ban quản trị sàn."
                  : "Hồ sơ chưa đạt yêu cầu xét duyệt. Vui lòng kiểm tra lại thông tin."}
              </span>
            </div>
          </div>

          <span
            className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase shrink-0 ${
              profile?.verificationStatus === "APPROVED"
                ? "bg-emerald-600 text-white"
                : profile?.verificationStatus === "PENDING"
                ? "bg-amber-600 text-white"
                : "bg-rose-600 text-white"
            }`}
          >
            {profile?.verificationStatus || "CHƯA XÁC MINH"}
          </span>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
          {/* Tên định danh người bán (sellerName slug) */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
              <Globe className="w-3.5 h-3.5 text-slate-400" />
              <span>Tên định danh gian hàng (Seller Slug)</span>
              <span className="text-rose-500">*</span>
            </label>
            <div className="flex items-center">
              <span className="h-10 px-3 rounded-l-xl border border-r-0 border-slate-200 bg-slate-50 text-slate-500 text-xs font-mono flex items-center select-none">
                techhub.vn/shops/
              </span>
              <input
                type="text"
                {...register("sellerName")}
                placeholder="vd: apple-store"
                className={`w-full h-10 px-3.5 rounded-r-xl border text-xs font-mono font-bold text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.sellerName
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
            </div>
            {errors.sellerName ? (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.sellerName.message}
              </p>
            ) : (
              <span className="text-[11px] text-slate-400">
                Đường dẫn công khai duy nhất của gian hàng bạn trên toàn hệ thống sàn.
              </span>
            )}
          </div>

          {/* Tên ngân hàng */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
              <Landmark className="w-3.5 h-3.5 text-slate-400" />
              <span>Ngân hàng thụ hưởng</span>
              <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              list="vietnam-banks-list"
              {...register("bankName")}
              placeholder="Chọn hoặc nhập tên ngân hàng..."
              className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                errors.bankName
                  ? "border-rose-400 bg-rose-50/20"
                  : "border-slate-200 bg-white"
              }`}
            />
            <datalist id="vietnam-banks-list">
              {VIETNAM_BANKS.map((b) => (
                <option key={b} value={b} />
              ))}
            </datalist>
            {errors.bankName && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.bankName.message}
              </p>
            )}
          </div>

          {/* Row: Số tài khoản & Tên chủ tài khoản */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <CreditCard className="w-3.5 h-3.5 text-slate-400" />
                <span>Số tài khoản ngân hàng</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("bankAccountNumber")}
                placeholder="VD: 1903658291..."
                className={`w-full h-10 px-3.5 rounded-xl border text-xs font-mono font-bold text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.bankAccountNumber
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.bankAccountNumber && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.bankAccountNumber.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-slate-400" />
                <span>Tên chủ tài khoản</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("bankAccountHolder")}
                placeholder="VD: NGUYEN VAN A"
                className={`w-full h-10 px-3.5 rounded-xl border text-xs font-bold uppercase text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.bankAccountHolder
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.bankAccountHolder && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.bankAccountHolder.message}
                </p>
              )}
            </div>
          </div>

          {/* Action Footer */}
          <div className="border-t border-slate-100 pt-4 flex items-center justify-between">
            <span className="text-[11px] text-slate-400">
              {isDirty ? "Có thay đổi chưa lưu" : "Thông tin thụ hưởng đã được lưu"}
            </span>
            <button
              type="submit"
              disabled={updateProfileMutation.isPending || !isDirty}
              className="h-10 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-xs font-bold transition-all shadow-xs flex items-center gap-2"
            >
              {updateProfileMutation.isPending && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              <span>Lưu thông tin ngân hàng</span>
            </button>
          </div>
        </form>
      </div>

      {/* RIGHT INFORMATION COLUMN */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        {/* Payout Schedule Card */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-800 border-b border-slate-100 pb-3">
            <CalendarDays className="w-4 h-4 text-[#0045cc]" />
            <span>Quy trình Đối soát & Quyết toán Doanh thu</span>
          </div>

          <div className="flex flex-col gap-3 text-xs text-slate-600 leading-relaxed">
            <p>
              Hệ thống sàn TechHub thực hiện chốt số liệu và chuyển khoản tự động doanh thu đơn hàng hoàn tất vào tài khoản ngân hàng thụ hưởng của bạn:
            </p>

            <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200/60 flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-slate-700">Chu kỳ đối soát:</span>
                <span className="font-bold text-[#0045cc]">Thứ 2 hàng tuần</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-semibold text-slate-700">Thời gian nhận tiền:</span>
                <span className="font-bold text-slate-900">24h làm việc</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-semibold text-slate-700">Hình thức:</span>
                <span className="font-bold text-slate-900">Chuyển khoản 24/7 Napas</span>
              </div>
            </div>

            <p className="text-[11px] text-slate-500 italic">
              * Lưu ý: Tên chủ tài khoản ngân hàng nên trùng khớp với tên doanh nghiệp hoặc cá nhân đăng ký kinh doanh để tránh chậm trễ trong quá trình đối soát liên ngân hàng.
            </p>
          </div>
        </div>

        {/* Public Store Link Card */}
        <div className="bg-[#eff4ff]/60 border border-[#0045cc]/15 rounded-2xl p-4 flex items-start gap-3">
          <Globe className="w-5 h-5 text-[#0045cc] shrink-0 mt-0.5" />
          <div className="flex flex-col gap-1 text-xs">
            <span className="font-bold text-[#0b1c30]">
              Liên kết gian hàng công khai
            </span>
            <span className="text-slate-600 leading-relaxed text-[11px]">
              Tên định danh <strong>{watchedSellerName || "slug"}</strong> dùng để tạo đường dẫn thân thiện khi bạn chia sẻ gian hàng lên mạng xã hội, diễn đàn công nghệ hoặc ấn phẩm quảng cáo.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
