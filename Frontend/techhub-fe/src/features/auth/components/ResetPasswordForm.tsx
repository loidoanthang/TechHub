"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, ArrowRight, Check, Loader2, CheckCircle2, AlertCircle, KeyRound } from "lucide-react";
import { useResetPasswordMutation } from "@/features/auth/api/useAuthMutations";
import { getPendingAuth, clearPendingAuth } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";
import { resetPasswordSchema, type ResetPasswordFormData } from "@/features/auth/schemas/authSchemas";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [email, setEmail] = useState("");
  const [hasStoredToken, setHasStoredToken] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const resetMutation = useResetPasswordMutation();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      token: "",
      newPassword: "",
      confirmPassword: "",
    },
    mode: "onTouched",
  });

  const newPasswordVal = watch("newPassword") || "";
  const confirmPasswordVal = watch("confirmPassword") || "";

  useEffect(() => {
    const pending = getPendingAuth();
    const resolvedEmail = pending.email || searchParams.get("email") || "";
    const resolvedToken = pending.resetToken || searchParams.get("token") || searchParams.get("code") || "";

    if (!resolvedEmail) {
      router.replace(ROUTES.LOGIN);
      return;
    }

    setEmail(resolvedEmail);
    if (resolvedToken) {
      setValue("token", resolvedToken, { shouldValidate: true });
      setHasStoredToken(true);
    }
  }, [searchParams, router, setValue]);

  const hasLength = newPasswordVal.length >= 8;
  const hasUpper = /[A-Z]/.test(newPasswordVal);
  const hasLower = /[a-z]/.test(newPasswordVal);
  const hasNumber = /[0-9]/.test(newPasswordVal);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPasswordVal);
  const isMatch = newPasswordVal.length > 0 && newPasswordVal === confirmPasswordVal;

  const onSubmit = async (data: ResetPasswordFormData) => {
    setServerError(null);

    if (!email.trim()) {
      setServerError("Thiếu thông tin email. Vui lòng bắt đầu lại từ bước Quên mật khẩu.");
      return;
    }

    try {
      const response = await resetMutation.mutateAsync({
        email: email.trim(),
        token: data.token.trim(),
        newPassword: data.newPassword,
      });

      if (response && response.success) {
        clearPendingAuth();
        setTimeout(() => {
          router.push(`${ROUTES.LOGIN}?reset=true`);
        }, 1200);
      }
    } catch (err) {
      const axiosError = err as AxiosError<ApiResponse<unknown>>;
      const errorData = axiosError.response?.data;
      const rawMessage = (errorData?.message || "").toLowerCase();

      let message =
        errorData?.message ||
        "Đặt lại mật khẩu không thành công. Vui lòng kiểm tra lại mã xác thực.";

      if (rawMessage.includes("same as the old")) {
        message = "Mật khẩu mới không được trùng với mật khẩu cũ của bạn.";
      } else if (rawMessage.includes("expired")) {
        message = "Mã xác thực đã hết hạn. Vui lòng yêu cầu lại mã từ bước quên mật khẩu.";
        setHasStoredToken(false);
        setValue("token", "");
      } else if (rawMessage.includes("invalid") || rawMessage.includes("not found")) {
        message = "Mã xác thực không hợp lệ. Vui lòng nhập lại mã OTP chính xác.";
        setHasStoredToken(false);
        setValue("token", "");
      }

      if (errorData?.data && typeof errorData.data === "object") {
        const fieldErrors = Object.values(errorData.data as Record<string, string>).join(", ");
        if (fieldErrors) message = fieldErrors;
      }

      setServerError(message);
    }
  };

  const isLoading = resetMutation.isPending || isSubmitting;
  const isSuccess = resetMutation.isSuccess;

  return (
    <div className="relative w-full max-w-[460px]">
      {/* Precision ambient background glow */}
      <div className="absolute -top-12 -left-12 w-48 h-48 bg-[#0045cc]/5 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute -bottom-10 -right-10 w-44 h-44 bg-[#dce9ff]/60 rounded-full blur-2xl pointer-events-none"></div>

      {/* Main Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 sm:p-10 flex flex-col gap-6">
        <div className="flex flex-col items-center text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-[#eff4ff] text-[#0045cc] mb-3">
            <KeyRound className="w-6 h-6" />
          </div>
          <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight">
            Reset password
          </h1>
          <p className="text-sm text-[#434656] mt-1.5 max-w-[340px]">
            Create a new strong password for your TechHub marketplace account.
          </p>
        </div>

        {serverError && (
          <div className="flex items-start gap-2.5 p-3.5 bg-red-50 border border-red-200 text-red-600 rounded-xl text-xs leading-relaxed animate-in fade-in duration-200">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          {/* OTP Code Input or Verified Badge */}
          {hasStoredToken ? (
            <div className="flex items-center justify-between p-3.5 bg-[#eff4ff]/70 border border-[#0d5bff]/15 rounded-xl text-xs text-[#0b1c30]">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span className="font-semibold">Mã xác thực:</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold tracking-widest text-[#0045cc] bg-white px-2.5 py-1 rounded-md shadow-xs">
                  ••••••
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setHasStoredToken(false);
                    setValue("token", "");
                  }}
                  className="text-[11px] font-semibold text-[#0045cc] hover:underline cursor-pointer"
                >
                  Thay đổi
                </button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="otp-code"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                6-Digit Verification Code (OTP)
              </label>
              <input
                id="otp-code"
                type="text"
                maxLength={6}
                {...register("token")}
                placeholder="123456"
                className={`w-full h-11 px-4 tracking-widest font-mono text-center bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.token
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              {errors.token && (
                <p className="text-xs text-rose-500 font-medium text-center">{errors.token.message}</p>
              )}
            </div>
          )}

          {/* New Password */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="new-password"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              New password
            </label>
            <div className="relative flex items-center">
              <input
                id="new-password"
                type={showPassword ? "text" : "password"}
                {...register("newPassword")}
                placeholder="Enter new password"
                className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.newPassword
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label="Toggle password visibility"
                className="absolute right-3.5 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
              >
                {showPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
            {errors.newPassword && (
              <p className="text-xs text-rose-500 font-medium">{errors.newPassword.message}</p>
            )}
          </div>

          {/* Confirm Password */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <label
                htmlFor="confirm-password"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Confirm new password
              </label>
              {confirmPasswordVal.length > 0 && (
                <span
                  className={`text-[11px] font-semibold ${
                    isMatch ? "text-emerald-600" : "text-rose-500"
                  }`}
                >
                  {isMatch ? "Matches" : "Doesn't match"}
                </span>
              )}
            </div>
            <div className="relative flex items-center">
              <input
                id="confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                {...register("confirmPassword")}
                placeholder="Confirm new password"
                className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.confirmPassword
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label="Toggle password confirmation visibility"
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
              <p className="text-xs text-rose-500 font-medium">{errors.confirmPassword.message}</p>
            )}
          </div>

          {/* Requirements Checklist */}
          <div className="bg-[#eff4ff]/60 rounded-xl p-3.5 flex flex-col gap-2 border border-slate-100">
            <p className="text-[11px] font-bold text-[#737688] uppercase tracking-wider">
              Password Requirements
            </p>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div
                className={`flex items-center gap-1.5 ${
                  hasLength ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <Check className="w-3.5 h-3.5" />
                <span>At least 8 chars</span>
              </div>
              <div
                className={`flex items-center gap-1.5 ${
                  hasUpper ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <Check className="w-3.5 h-3.5" />
                <span>One uppercase</span>
              </div>
              <div
                className={`flex items-center gap-1.5 ${
                  hasNumber ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <Check className="w-3.5 h-3.5" />
                <span>One number</span>
              </div>
              <div
                className={`flex items-center gap-1.5 ${
                  hasSpecial ? "text-emerald-600 font-medium" : "text-[#737688]"
                }`}
              >
                <Check className="w-3.5 h-3.5" />
                <span>One symbol</span>
              </div>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isLoading || isSuccess}
            className="w-full h-12 mt-1 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Updating password...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Password Reset Complete!</span>
              </>
            ) : (
              <>
                <span>Reset password</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center pt-2">
          <Link
            href={ROUTES.LOGIN}
            className="text-xs font-semibold text-[#0045cc] hover:underline"
          >
            Back to login
          </Link>
        </div>
      </div>
    </div>
  );
}