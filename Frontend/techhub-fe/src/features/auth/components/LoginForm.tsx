"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, ArrowRight, Loader2, CheckCircle2, AlertCircle } from "lucide-react";
import { useLoginMutation } from "@/features/auth/api/useAuthMutations";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { setPendingAuth } from "@/lib/api/token";
import { API_BASE_URL, ROUTES } from "@/lib/constants";
import { isSafeRelativeUrl } from "@/lib/security";
import { loginSchema, type LoginFormData } from "@/features/auth/schemas/authSchemas";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const rawReturnUrl = searchParams.get("returnUrl");
  // Prevent Open Redirect (CWE-601): Strictly validate relative path
  const returnUrl = isSafeRelativeUrl(rawReturnUrl)
    ? (rawReturnUrl as string)
    : ROUTES.HOME;

  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const isResetSuccess = searchParams.get("reset") === "true";
  const isChangedSuccess = searchParams.get("changed") === "true";

  const { login } = useAuth();
  const loginMutation = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      rememberMe: false,
    },
    mode: "onTouched",
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);

    try {
      const response = await loginMutation.mutateAsync({
        email: data.email.trim(),
        password: data.password,
      });

      if (response && response.success && response.data) {
        await login(response.data, returnUrl);
      }
    } catch (err) {
      const axiosError = err as AxiosError<ApiResponse<unknown>>;
      const errorData = axiosError.response?.data;
      const metadata = errorData?.metadata as { errorCode?: string } | undefined;
      const errorCode = metadata?.errorCode;
      const rawMessage = errorData?.message?.toLowerCase() || "";

      const isUnverified =
        errorCode === "ACCOUNT_NOT_VERIFIED" ||
        rawMessage.includes("not verified") ||
        rawMessage.includes("verify your email");

      const isLocked =
        errorCode === "ACCOUNT_LOCKED" ||
        rawMessage.includes("locked") ||
        rawMessage.includes("15 minutes");

      const isSuspended =
        errorCode === "ACCOUNT_SUSPENDED" ||
        errorCode === "ACCOUNT_BANNED" ||
        rawMessage.includes("suspended") ||
        rawMessage.includes("banned");

      const isInvalidCredentials =
        errorCode === "INVALID_CREDENTIALS" ||
        rawMessage.includes("invalid email or password") ||
        rawMessage.includes("bad credentials");

      if (isUnverified) {
        setPendingAuth(data.email.trim(), "verify-email");
        router.push(ROUTES.VERIFY_OTP);
      } else if (isLocked) {
        setServerError(
          "Tài khoản của bạn đã bị khóa tạm thời 15 phút do nhập sai mật khẩu nhiều lần."
        );
      } else if (isSuspended) {
        router.push(ROUTES.SUSPENDED);
      } else if (isInvalidCredentials) {
        setServerError("Email hoặc mật khẩu không chính xác.");
      } else {
        setServerError(
          errorData?.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin."
        );
      }
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
  };

  const isLoading = loginMutation.isPending || isSubmitting;
  const isSuccess = loginMutation.isSuccess;

  return (
    <div className="relative w-full max-w-[440px]">
      {/* Precision ambient background glow */}
      <div className="absolute w-[500px] h-[500px] rounded-full bg-[#0d5bff]/10 blur-3xl pointer-events-none -top-24 -left-20"></div>
      <div className="absolute w-[440px] h-[440px] rounded-full bg-[#dce9ff]/40 blur-3xl pointer-events-none -bottom-20 -right-16"></div>

      {/* Main Login Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 sm:p-10 flex flex-col gap-6">
        <div className="flex flex-col items-center text-center gap-1.5">
          <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight">
            Log in to your account
          </h1>
          <p className="text-sm text-[#434656]">
            Please enter your email and password
          </p>
        </div>

        {(isResetSuccess || isChangedSuccess) && (
          <div className="flex items-center gap-2.5 p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs leading-relaxed animate-in fade-in duration-200">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
            <span>
              {isChangedSuccess
                ? "Mật khẩu của bạn đã được thay đổi thành công. Vui lòng đăng nhập lại."
                : "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập bằng mật khẩu mới."}
            </span>
          </div>
        )}

        {serverError && (
          <div className="flex items-start gap-2.5 p-3.5 bg-red-50 border border-red-200 text-red-600 rounded-xl text-xs leading-relaxed animate-in fade-in duration-200">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          {/* Email */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="email"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Email address
            </label>
            <input
              id="email"
              type="email"
              {...register("email")}
              placeholder="name@example.com"
              className={`w-full h-12 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                errors.email
                  ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                  : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
              }`}
            />
            {errors.email && (
              <p className="text-xs text-rose-500 font-medium">{errors.email.message}</p>
            )}
          </div>

          {/* Password */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="password"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Password
            </label>
            <div className="relative flex items-center">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                {...register("password")}
                placeholder="••••••••••••"
                className={`w-full h-12 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                  errors.password
                    ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                    : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label="Toggle password visibility"
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
              <p className="text-xs text-rose-500 font-medium">{errors.password.message}</p>
            )}
          </div>

          {/* Remember me & Forgot Password */}
          <div className="flex items-center justify-between pt-1">
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                {...register("rememberMe")}
                className="w-4 h-4 rounded text-[#0d5bff] focus:ring-0 accent-[#0d5bff] cursor-pointer"
              />
              <span className="text-xs text-[#434656]">Remember me</span>
            </label>
            <Link
              href={ROUTES.FORGOT_PASSWORD}
              className="text-xs font-semibold text-[#0045cc] hover:underline"
            >
              Forgot password?
            </Link>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isLoading || isSuccess}
            className="w-full h-12 mt-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-80 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Authenticating...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Authenticated</span>
              </>
            ) : (
              <>
                <span>Log in</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="flex items-center gap-3">
          <div className="flex-1 h-px bg-[#e5eeff]"></div>
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#737688]">
            Or log in with
          </span>
          <div className="flex-1 h-px bg-[#e5eeff]"></div>
        </div>

        <button
          type="button"
          onClick={handleGoogleLogin}
          className="w-full h-12 bg-[#eff4ff]/70 hover:bg-[#e5eeff] text-[#0b1c30] text-sm font-semibold rounded-xl flex items-center justify-center gap-3 transition-all active:scale-[0.99] cursor-pointer"
        >
          <svg aria-hidden="true" className="w-5 h-5 shrink-0" viewBox="0 0 24 24">
            <path
              d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
              fill="#4285F4"
            />
            <path
              d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
              fill="#34A853"
            />
            <path
              d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.17 0 9.97 0 12s.45 3.83 1.25 5.42l4.03-3.15z"
              fill="#FBBC05"
            />
            <path
              d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
              fill="#EA4335"
            />
          </svg>
          <span>Google</span>
        </button>

        <div className="text-center pt-1">
          <p className="text-sm text-[#434656]">
            Don&apos;t have an account?{" "}
            <Link
              href={ROUTES.REGISTER}
              className="text-sm font-semibold text-[#0045cc] hover:underline ml-1"
            >
              Sign up now
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}