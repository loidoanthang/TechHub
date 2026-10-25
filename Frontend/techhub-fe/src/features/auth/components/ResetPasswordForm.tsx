"use client";

import { useState } from "react";
import Link from "next/link";
import { Eye, EyeOff, ArrowRight, Check, Loader2, CheckCircle2 } from "lucide-react";

export function ResetPasswordForm() {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const hasLength = newPassword.length >= 8;
  const hasUpper = /[A-Z]/.test(newPassword);
  const hasLower = /[a-z]/.test(newPassword);
  const hasNumber = /[0-9]/.test(newPassword);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPassword);
  const isMatch = newPassword.length > 0 && newPassword === confirmPassword;
  const isValid = hasLength && hasUpper && hasLower && hasNumber && isMatch;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setIsSuccess(true);
      setTimeout(() => {
        window.location.href = "/login";
      }, 1500);
    }, 1200);
  };

  return (
    <div className="relative w-full max-w-[460px]">
      {/* Precision ambient background glow */}
      <div className="absolute -top-12 -left-12 w-48 h-48 bg-[#0045cc]/5 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute -bottom-10 -right-10 w-44 h-44 bg-[#dce9ff]/60 rounded-full blur-2xl pointer-events-none"></div>

      {/* Main Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 sm:p-10 flex flex-col gap-6">
        <div className="flex flex-col items-center text-center">
          <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight">
            Reset password
          </h1>
          <p className="text-sm text-[#434656] mt-1.5 max-w-[340px]">
            Create a new strong password for your TechHub marketplace account.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Enter new password"
                required
                className="w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
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
              {confirmPassword.length > 0 && (
                <span
                  className={`text-[11px] font-semibold ${
                    isMatch ? "text-emerald-600" : "text-rose-500"
                  }`}
                >
                  {isMatch ? "Passwords match" : "Does not match"}
                </span>
              )}
            </div>
            <div className="relative flex items-center">
              <input
                id="confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter new password"
                required
                className="w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label="Toggle confirm password visibility"
                className="absolute right-3.5 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
              >
                {showConfirmPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* Password Requirements */}
          <div className="bg-[#eff4ff]/70 rounded-xl p-4 flex flex-col gap-2">
            <span className="text-[11px] uppercase tracking-wider text-[#434656] font-bold">
              Password Must Contain:
            </span>
            <ul className="flex flex-col gap-1.5 text-xs text-[#434656]">
              <li
                className={`flex items-center gap-2 ${
                  hasLength ? "text-emerald-600 font-medium" : ""
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasLength ? "bg-emerald-100 text-emerald-600" : "bg-[#e5eeff] text-[#737688]"
                  }`}
                >
                  <Check className="w-3 h-3" />
                </div>
                <span>At least 8 characters</span>
              </li>
              <li
                className={`flex items-center gap-2 ${
                  hasUpper && hasLower ? "text-emerald-600 font-medium" : ""
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasUpper && hasLower ? "bg-emerald-100 text-emerald-600" : "bg-[#e5eeff] text-[#737688]"
                  }`}
                >
                  <Check className="w-3 h-3" />
                </div>
                <span>Uppercase & lowercase letters</span>
              </li>
              <li
                className={`flex items-center gap-2 ${
                  hasNumber ? "text-emerald-600 font-medium" : ""
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasNumber ? "bg-emerald-100 text-emerald-600" : "bg-[#e5eeff] text-[#737688]"
                  }`}
                >
                  <Check className="w-3 h-3" />
                </div>
                <span>At least one number (0-9)</span>
              </li>
              <li
                className={`flex items-center gap-2 ${
                  hasSpecial ? "text-emerald-600 font-medium" : ""
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${
                    hasSpecial ? "bg-emerald-100 text-emerald-600" : "bg-[#e5eeff] text-[#737688]"
                  }`}
                >
                  <Check className="w-3 h-3" />
                </div>
                <span>Special character (e.g. !@#$%^&*)</span>
              </li>
            </ul>
          </div>

          <button
            type="submit"
            disabled={isLoading || isSuccess || !isValid}
            className="w-full h-12 mt-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Resetting password...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Password Reset Successfully!</span>
              </>
            ) : (
              <>
                <span>Save New Password</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center pt-1 border-t border-[#eff4ff]">
          <Link
            href="/login"
            className="text-xs font-semibold text-[#434656] hover:text-[#0045cc] transition-colors"
          >
            Back to login
          </Link>
        </div>
      </div>
    </div>
  );
}
