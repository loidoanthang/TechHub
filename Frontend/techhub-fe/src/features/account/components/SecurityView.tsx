"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  KeyRound,
  Eye,
  EyeOff,
  Check,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  Loader2,
  ArrowRight,
} from "lucide-react";
import { useChangePasswordMutation } from "@/features/auth/api/useAuthMutations";
import { clearTokens } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";
import {
  changePasswordSchema,
  type ChangePasswordFormData,
} from "../schemas/securitySchema";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function SecurityView() {
  const router = useRouter();

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

  const changePasswordMutation = useChangePasswordMutation();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
    mode: "onTouched",
  });

  const newPasswordVal = watch("newPassword") || "";
  const confirmPasswordVal = watch("confirmPassword") || "";

  // Dynamic live criteria checkers
  const hasLength = newPasswordVal.length >= 8;
  const hasUpper = /[A-Z]/.test(newPasswordVal);
  const hasLower = /[a-z]/.test(newPasswordVal);
  const hasNumber = /[0-9]/.test(newPasswordVal);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPasswordVal);
  const isMatch = newPasswordVal.length > 0 && newPasswordVal === confirmPasswordVal;

  // Password strength calculation (0 to 4)
  const strengthScore = [
    hasLength,
    hasUpper && hasLower,
    hasNumber,
    hasSpecial,
  ].filter(Boolean).length;

  const getStrengthLabel = (score: number) => {
    if (!newPasswordVal) return { label: "Chưa nhập", color: "text-slate-400" };
    switch (score) {
      case 1:
        return { label: "Yếu", color: "text-rose-500" };
      case 2:
        return { label: "Trung bình", color: "text-amber-500" };
      case 3:
        return { label: "Khá", color: "text-blue-500" };
      case 4:
        return { label: "Rất mạnh", color: "text-emerald-600" };
      default:
        return { label: "Yếu", color: "text-rose-500" };
    }
  };

  const strengthInfo = getStrengthLabel(strengthScore);

  const onSubmit = async (data: ChangePasswordFormData) => {
    setServerError(null);

    try {
      const response = await changePasswordMutation.mutateAsync({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });

      if (response && response.success !== false) {
        setIsSuccessModalOpen(true);
        // Tự động chuyển hướng về trang login sau 3 giây để người dùng đăng nhập lại
        setTimeout(() => {
          handleProceedToLogin();
        }, 3000);
      }
    } catch (err: unknown) {
      const axiosError = err as AxiosError<ApiResponse<unknown>>;
      const errorData = axiosError.response?.data;
      const rawMessage = (errorData?.message || "").toLowerCase();

      let message = "Đổi mật khẩu không thành công. Vui lòng kiểm tra lại thông tin.";
      if (
        rawMessage.includes("current password is incorrect") ||
        rawMessage.includes("wrong_current_password") ||
        rawMessage.includes("current password")
      ) {
        message = "Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại.";
      } else if (
        rawMessage.includes("cannot be the same") ||
        rawMessage.includes("password_same_as_old") ||
        rawMessage.includes("same as the old password")
      ) {
        message = "Mật khẩu mới không được trùng với mật khẩu hiện tại.";
      } else if (errorData?.message) {
        message = errorData.message;
      }

      setServerError(message);
    }
  };

  const handleProceedToLogin = () => {
    clearTokens();
    router.push(`${ROUTES.LOGIN}?changed=true`);
  };

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Top Header */}
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
          Bảo mật &amp; Mật khẩu
        </h1>
        <p className="text-xs text-[#737688]">
          Quản lý mật khẩu đăng nhập và bảo vệ an toàn cho tài khoản TechHub của bạn.
        </p>
      </div>

      {/* Extended Full-Width Bento Card */}
      <div className="w-full bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-6">
        {/* Card Header */}
        <div className="flex items-center gap-3.5 pb-4 border-b border-[#eff4ff]">
          <div className="w-10 h-10 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center shrink-0 shadow-sm">
            <KeyRound className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-[#0b1c30] font-heading">
              Thay đổi mật khẩu đăng nhập
            </h2>
            <p className="text-xs text-[#737688]">
              Tạo mật khẩu mạnh kết hợp chữ cái, số và ký tự đặc biệt để tối ưu bảo mật.
            </p>
          </div>
        </div>

        {/* Server Error Alert Banner */}
        {serverError && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2.5 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
            <span className="font-medium leading-relaxed">{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-6">
          {/* Inputs Grid: Current Password (Full Width) + New & Confirm (2 Columns) */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5 w-full">
            {/* Field 1: Current Password (Spans full 2 columns) */}
            <div className="flex flex-col gap-1.5 md:col-span-2">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="current-password"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Mật khẩu hiện tại <span className="text-rose-500">*</span>
                </label>
                <Link
                  href={ROUTES.FORGOT_PASSWORD}
                  className="text-[11px] font-semibold text-[#0045cc] hover:text-[#0d5bff] hover:underline transition-colors"
                >
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative flex items-center">
                <input
                  id="current-password"
                  type={showCurrentPassword ? "text" : "password"}
                  {...register("currentPassword")}
                  placeholder="Nhập mật khẩu đang sử dụng"
                  className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                    errors.currentPassword
                      ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                      : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  aria-label="Ẩn/hiện mật khẩu hiện tại"
                  className="absolute right-3.5 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
                >
                  {showCurrentPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
              {errors.currentPassword && (
                <p className="text-xs text-rose-500 font-medium">
                  {errors.currentPassword.message}
                </p>
              )}
            </div>

            {/* Field 2: New Password (Column 1) */}
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="new-password"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Mật khẩu mới <span className="text-rose-500">*</span>
              </label>
              <div className="relative flex items-center">
                <input
                  id="new-password"
                  type={showNewPassword ? "text" : "password"}
                  {...register("newPassword")}
                  placeholder="Nhập mật khẩu mới"
                  className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                    errors.newPassword
                      ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                      : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  aria-label="Ẩn/hiện mật khẩu mới"
                  className="absolute right-3.5 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
                >
                  {showNewPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
              {errors.newPassword && (
                <p className="text-xs text-rose-500 font-medium">
                  {errors.newPassword.message}
                </p>
              )}
            </div>

            {/* Field 3: Confirm New Password (Column 2) */}
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="confirm-password"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Xác nhận mật khẩu mới <span className="text-rose-500">*</span>
                </label>
                {confirmPasswordVal.length > 0 && (
                  <span
                    className={`text-[11px] font-semibold ${
                      isMatch ? "text-emerald-600" : "text-rose-500"
                    }`}
                  >
                    {isMatch ? "Khớp mật khẩu" : "Chưa khớp"}
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <input
                  id="confirm-password"
                  type={showConfirmPassword ? "text" : "password"}
                  {...register("confirmPassword")}
                  placeholder="Nhập lại mật khẩu mới"
                  className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                    errors.confirmPassword
                      ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                      : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label="Ẩn/hiện xác nhận mật khẩu"
                  className="absolute right-3.5 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
                >
                  {showConfirmPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="text-xs text-rose-500 font-medium">
                  {errors.confirmPassword.message}
                </p>
              )}
            </div>
          </div>

          {/* Section 3: Extended Dynamic Criteria Checklist (3 Columns on Desktop) */}
          <div className="bg-[#eff4ff]/60 rounded-xl p-4 sm:p-5 flex flex-col gap-3.5 border border-[#c3c5d9]/20">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <p className="text-[11px] font-bold text-[#737688] uppercase tracking-wider">
                Tiêu chuẩn mật khẩu an toàn
              </p>
              <div className="flex items-center gap-2">
                {newPasswordVal && (
                  <span className={`text-[11px] font-semibold ${strengthInfo.color}`}>
                    Độ mạnh: {strengthInfo.label}
                  </span>
                )}
              </div>
            </div>

            {/* Password Strength Meter Bar */}
            {newPasswordVal.length > 0 && (
              <div className="flex items-center gap-1.5">
                {[1, 2, 3, 4].map((step) => {
                  const isActive = strengthScore >= step;
                  return (
                    <div
                      key={step}
                      className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${
                        isActive
                          ? strengthScore <= 1
                            ? "bg-rose-500"
                            : strengthScore === 2
                            ? "bg-amber-500"
                            : strengthScore === 3
                            ? "bg-blue-500"
                            : "bg-emerald-500"
                          : "bg-slate-200"
                      }`}
                    />
                  );
                })}
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5 text-xs">
              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  hasLength ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasLength ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Ít nhất 8 ký tự</span>
              </div>

              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  hasUpper ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasUpper ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Một chữ hoa (A-Z)</span>
              </div>

              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  hasLower ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasLower ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Một chữ thường (a-z)</span>
              </div>

              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  hasNumber ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasNumber ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Một chữ số (0-9)</span>
              </div>

              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  hasSpecial ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasSpecial ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Ký tự đặc biệt (!@#$%...)</span>
              </div>

              <div
                className={`flex items-center gap-1.5 transition-colors ${
                  isMatch ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    isMatch ? "bg-emerald-100 text-emerald-600" : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </div>
                <span>Khớp mật khẩu xác nhận</span>
              </div>
            </div>
          </div>

          {/* Section 4: Compact Security Protection Notice */}
          <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-slate-50 border border-slate-200/60 text-xs text-[#737688]">
            <ShieldCheck className="w-4 h-4 shrink-0 text-[#0045cc]" />
            <span>
              Để đảm bảo an toàn tuyệt đối, hệ thống sẽ tự động thu hồi và đăng xuất toàn bộ các phiên làm việc khác khi bạn cập nhật mật khẩu thành công.
            </span>
          </div>

          {/* Section 5: Form Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#eff4ff]">
            <button
              type="button"
              onClick={() => {
                reset();
                setServerError(null);
              }}
              disabled={isSubmitting || changePasswordMutation.isPending}
              className="px-5 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-50"
            >
              Hủy bỏ
            </button>

            <button
              type="submit"
              disabled={isSubmitting || changePasswordMutation.isPending}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold shadow-sm transition-all cursor-pointer active:scale-[0.99] disabled:opacity-50 flex items-center gap-2"
            >
              {(isSubmitting || changePasswordMutation.isPending) && (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              )}
              <span>
                {isSubmitting || changePasswordMutation.isPending
                  ? "Đang lưu..."
                  : "Cập nhật mật khẩu"}
              </span>
            </button>
          </div>
        </form>
      </div>

      {/* Success Modal */}
      {isSuccessModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200"
        >
          <div className="relative z-10 w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-6 sm:p-7 flex flex-col items-center text-center gap-4">
            <div className="w-14 h-14 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="flex flex-col gap-1">
              <h3 className="font-heading text-lg font-bold text-slate-900">
                Đổi mật khẩu thành công!
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed max-w-sm">
                Mật khẩu của bạn đã được cập nhật an toàn. Toàn bộ phiên đăng nhập khác đã được thu hồi.
              </p>
            </div>

            <div className="w-full bg-[#eff4ff] rounded-xl p-3 text-[11px] text-[#0045cc] font-medium flex items-center justify-center gap-2">
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
              <span>Đang chuyển hướng tới trang Đăng nhập...</span>
            </div>

            <button
              type="button"
              onClick={handleProceedToLogin}
              className="w-full py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>Đăng nhập lại ngay</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
