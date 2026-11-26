"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, ArrowRight, Loader2, CheckCircle2, AlertCircle } from "lucide-react";
import { useRegisterMutation } from "@/features/auth/api/useAuthMutations";
import { setPendingAuth } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";
import { registerSchema, type RegisterFormData } from "@/features/auth/schemas/authSchemas";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function RegisterForm() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const registerMutation = useRegisterMutation();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      agreeTerms: false as unknown as undefined,
    },
    mode: "onTouched",
  });

  const passwordVal = watch("password") || "";
  const confirmPasswordVal = watch("confirmPassword") || "";

  // Visual strength indicator
  const getPasswordStrength = (pass: string) => {
    let score = 0;
    if (pass.length >= 8) score++;
    if (/[A-Z]/.test(pass)) score++;
    if (/[0-9]/.test(pass)) score++;
    if (/[^A-Za-z0-9]/.test(pass)) score++;
    return score;
  };

  const strength = getPasswordStrength(passwordVal);
  const passwordsMatch = passwordVal.length > 0 && passwordVal === confirmPasswordVal;

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);

    try {
      const response = await registerMutation.mutateAsync({
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        email: data.email.trim(),
        phone: data.phone?.trim() || undefined,
        password: data.password,
      });

      if (response && response.success) {
        setPendingAuth(data.email.trim(), "verify-email");
        setTimeout(() => {
          router.push(ROUTES.VERIFY_OTP);
        }, 800);
      }
    } catch (err) {
      const axiosError = err as AxiosError<ApiResponse<unknown>>;
      const errorData = axiosError.response?.data;
      const rawMessage = (errorData?.message || "").toLowerCase();

      if (rawMessage.includes("not verified") || rawMessage.includes("verify your account")) {
        setPendingAuth(data.email.trim(), "verify-email");
        router.push(ROUTES.VERIFY_OTP);
        return;
      }

      if (rawMessage.includes("already exist")) {
        setServerError("Email này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng email khác.");
        return;
      }

      setServerError(
        errorData?.message || "Đăng ký không thành công. Vui lòng kiểm tra lại thông tin."
      );
    }
  };

  const isLoading = registerMutation.isPending || isSubmitting;
  const isSuccess = registerMutation.isSuccess;

  return (
    <div className="relative w-full max-w-[480px]">
      {/* Ambient Glow */}
      <div className="absolute -top-20 -right-20 w-48 h-48 bg-[#0045cc]/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute -bottom-20 -left-20 w-48 h-48 bg-[#dce9ff]/60 rounded-full blur-3xl pointer-events-none"></div>

      {/* Main Register Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-6 sm:p-10 flex flex-col gap-6">
        <div className="flex flex-col items-center text-center gap-1.5">
          <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
            Tạo tài khoản mới
          </h1>
          <p className="text-sm text-[#434656] max-w-xs">
            Tham gia TechHub để khám phá thiết bị công nghệ và ưu đãi chính hãng
          </p>
        </div>

        {serverError && (
          <div className="flex items-start gap-2.5 p-3.5 bg-red-50 border border-red-200 text-red-600 rounded-xl text-xs leading-relaxed animate-in fade-in duration-200">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          {/* First & Last Name Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="first-name"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Họ
              </label>
              <input
                id="first-name"
                type="text"
                aria-invalid={Boolean(errors.firstName)}
                aria-describedby={errors.firstName ? "reg-first-name-error" : undefined}
                {...register("firstName")}
                placeholder="Nguyễn"
                className={`w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.firstName
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              {errors.firstName && (
                <p id="reg-first-name-error" className="text-[11px] text-rose-500 font-medium">{errors.firstName.message}</p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="last-name"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Tên
              </label>
              <input
                id="last-name"
                type="text"
                aria-invalid={Boolean(errors.lastName)}
                aria-describedby={errors.lastName ? "reg-last-name-error" : undefined}
                {...register("lastName")}
                placeholder="Văn A"
                className={`w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.lastName
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              {errors.lastName && (
                <p id="reg-last-name-error" className="text-[11px] text-rose-500 font-medium">{errors.lastName.message}</p>
              )}
            </div>
          </div>

          {/* Email */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="email"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Địa chỉ Email
            </label>
            <input
              id="email"
              type="email"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? "reg-email-error" : undefined}
              {...register("email")}
              placeholder="ten@example.com"
              className={`w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                errors.email
                  ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                  : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
              }`}
            />
            {errors.email && (
              <p id="reg-email-error" className="text-[11px] text-rose-500 font-medium">{errors.email.message}</p>
            )}
          </div>

          {/* Phone (Optional) */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="phone"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Số điện thoại (tùy chọn)
            </label>
            <input
              id="phone"
              type="tel"
              aria-invalid={Boolean(errors.phone)}
              aria-describedby={errors.phone ? "reg-phone-error" : undefined}
              {...register("phone")}
              placeholder="0987654321"
              className={`w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                errors.phone
                  ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                  : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
              }`}
            />
            {errors.phone && (
              <p id="reg-phone-error" className="text-[11px] text-rose-500 font-medium">{errors.phone.message}</p>
            )}
          </div>

          {/* Password */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="password"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Mật khẩu
            </label>
            <div className="relative flex items-center">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? "reg-password-error" : undefined}
                {...register("password")}
                placeholder="Ít nhất 8 ký tự"
                className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.password
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                className="absolute right-3 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
              >
                {showPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
            {errors.password && (
              <p id="reg-password-error" className="text-[11px] text-rose-500 font-medium">{errors.password.message}</p>
            )}
            {/* Strength meter indicator */}
            {passwordVal.length > 0 && (
              <div className="flex items-center gap-1.5 mt-1">
                <div
                  className={`h-1 flex-1 rounded-full ${
                    strength >= 1 ? "bg-amber-400" : "bg-neutral-200"
                  }`}
                ></div>
                <div
                  className={`h-1 flex-1 rounded-full ${
                    strength >= 2 ? "bg-amber-400" : "bg-neutral-200"
                  }`}
                ></div>
                <div
                  className={`h-1 flex-1 rounded-full ${
                    strength >= 3 ? "bg-emerald-500" : "bg-neutral-200"
                  }`}
                ></div>
                <div
                  className={`h-1 flex-1 rounded-full ${
                    strength >= 4 ? "bg-emerald-500" : "bg-neutral-200"
                  }`}
                ></div>
              </div>
            )}
          </div>

          {/* Confirm Password */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <label
                htmlFor="confirm-password"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Xác nhận mật khẩu
              </label>
              {confirmPasswordVal.length > 0 && (
                <span
                  className={`text-[11px] font-semibold ${
                    passwordsMatch ? "text-emerald-600" : "text-rose-500"
                  }`}
                >
                  {passwordsMatch ? "Khớp mật khẩu" : "Chưa khớp"}
                </span>
              )}
            </div>
            <div className="relative flex items-center">
              <input
                id="confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                aria-invalid={Boolean(errors.confirmPassword)}
                aria-describedby={errors.confirmPassword ? "reg-confirm-password-error" : undefined}
                {...register("confirmPassword")}
                placeholder="Nhập lại mật khẩu"
                className={`w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.confirmPassword
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label={showConfirmPassword ? "Ẩn xác nhận mật khẩu" : "Hiện xác nhận mật khẩu"}
                className="absolute right-3 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
              >
                {showConfirmPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
            {errors.confirmPassword && (
              <p id="reg-confirm-password-error" className="text-[11px] text-rose-500 font-medium">{errors.confirmPassword.message}</p>
            )}
          </div>

          {/* Terms checkbox */}
          <div className="flex flex-col gap-1 pt-1">
            <div className="flex items-start gap-2.5">
              <input
                id="terms-check"
                type="checkbox"
                aria-invalid={Boolean(errors.agreeTerms)}
                aria-describedby={errors.agreeTerms ? "reg-terms-error" : undefined}
                {...register("agreeTerms")}
                className="w-4 h-4 mt-0.5 rounded text-[#0d5bff] focus:ring-0 accent-[#0d5bff] cursor-pointer"
              />
              <label
                htmlFor="terms-check"
                className="text-xs text-[#434656] cursor-pointer leading-relaxed"
              >
                Tôi đồng ý với{" "}
                <Link href="#" className="text-[#0045cc] font-semibold hover:underline">
                  Điều khoản dịch vụ
                </Link>{" "}
                và{" "}
                <Link href="#" className="text-[#0045cc] font-semibold hover:underline">
                  Chính sách bảo mật
                </Link>
                .
              </label>
            </div>
            {errors.agreeTerms && (
              <p id="reg-terms-error" className="text-[11px] text-rose-500 font-medium ml-6">{errors.agreeTerms.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading || isSuccess}
            className="w-full h-12 mt-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Đang tạo tài khoản...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Tạo tài khoản thành công!</span>
              </>
            ) : (
              <>
                <span>Tạo tài khoản</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center pt-1">
          <p className="text-sm text-[#434656]">
            Đã có tài khoản?{" "}
            <Link
              href={ROUTES.LOGIN}
              className="text-sm font-semibold text-[#0045cc] hover:underline ml-1"
            >
              Đăng nhập
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}