"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, ArrowLeft, MailCheck, Loader2, AlertCircle } from "lucide-react";
import { useForgotPasswordMutation } from "@/features/auth/api/useAuthMutations";
import { setPendingAuth } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";
import { forgotPasswordSchema, type ForgotPasswordFormData } from "@/features/auth/schemas/authSchemas";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function ForgotPasswordForm() {
  const router = useRouter();
  const [isSent, setIsSent] = useState(false);
  const [sentEmail, setSentEmail] = useState("");
  const [serverError, setServerError] = useState<string | null>(null);

  const forgotMutation = useForgotPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
    mode: "onTouched",
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setServerError(null);

    try {
      const response = await forgotMutation.mutateAsync({ email: data.email.trim() });
      if (response && response.success) {
        setPendingAuth(data.email.trim(), "reset-password");
        setSentEmail(data.email.trim());
        setIsSent(true);
        setTimeout(() => {
          router.push(ROUTES.VERIFY_OTP);
        }, 1200);
      }
    } catch (err) {
      const axiosError = err as AxiosError<ApiResponse<unknown>>;
      setServerError(
        axiosError.response?.data?.message ||
          "Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng kiểm tra lại email."
      );
    }
  };

  const isLoading = forgotMutation.isPending || isSubmitting;

  return (
    <div className="relative w-full max-w-[440px]">
      {/* Precision ambient background glow */}
      <div className="absolute w-[500px] h-[500px] rounded-full bg-[#0045cc]/10 blur-3xl pointer-events-none -top-24 opacity-60"></div>

      {/* Main Forgot Password Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 sm:p-10 flex flex-col items-center">
        {!isSent ? (
          <>
            <div className="text-center w-full mb-8">
              <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight mb-2">
                Forgot password
              </h1>
              <p className="text-sm text-[#434656] leading-relaxed">
                Enter your registered email address and we&apos;ll send you an OTP verification code.
              </p>
            </div>

            {serverError && (
              <div className="w-full flex items-start gap-2.5 p-3.5 mb-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-xs leading-relaxed animate-in fade-in duration-200">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{serverError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="w-full flex flex-col gap-4">
              <div className="flex flex-col gap-1.5 text-left">
                <label
                  htmlFor="recovery-email"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Email address
                </label>
                <input
                  id="recovery-email"
                  type="email"
                  {...register("email")}
                  placeholder="alex.m@techhub.com"
                  className={`w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none transition-all placeholder:text-[#737688]/60 ${
                    errors.email
                      ? "border border-rose-400 bg-rose-50/30 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                      : "focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25"
                  }`}
                />
                {errors.email && (
                  <p className="text-xs text-rose-500 font-medium">{errors.email.message}</p>
                )}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-12 mt-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    <span>Sending code...</span>
                  </>
                ) : (
                  <>
                    <span>Send Code</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          </>
        ) : (
          <div className="w-full flex flex-col items-center text-center py-2">
            <div className="w-14 h-14 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center mb-4">
              <MailCheck className="w-7 h-7" />
            </div>
            <h2 className="font-heading text-xl font-bold text-[#0b1c30] mb-1">
              Code Dispatched
            </h2>
            <p className="text-sm text-[#434656] mb-6">
              Check your inbox at{" "}
              <span className="font-semibold text-[#0b1c30]">{sentEmail}</span> for a
              6-digit one-time authorization code.
            </p>
            <div className="flex flex-col gap-3 w-full">
              <Link
                href={ROUTES.VERIFY_OTP}
                className="w-full h-11 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold flex items-center justify-center transition-colors"
              >
                Enter Verification Code
              </Link>
              <button
                type="button"
                onClick={() => setIsSent(false)}
                className="text-xs text-[#737688] hover:text-[#0b1c30] transition-colors cursor-pointer"
              >
                Try with different email
              </button>
            </div>
          </div>
        )}

        <div className="w-full mt-8 pt-4 border-t border-[#eff4ff] flex items-center justify-center">
          <Link
            href={ROUTES.LOGIN}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#434656] hover:text-[#0b1c30] transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to login</span>
          </Link>
        </div>
      </div>
    </div>
  );
}