"use client";

import { useState } from "react";
import Link from "next/link";
import { Eye, EyeOff, ArrowRight, Loader2, CheckCircle2 } from "lucide-react";

export function RegisterForm() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  // Strength score
  const getPasswordStrength = (pass: string) => {
    let score = 0;
    if (pass.length >= 8) score++;
    if (/[A-Z]/.test(pass)) score++;
    if (/[0-9]/.test(pass)) score++;
    if (/[^A-Za-z0-9]/.test(pass)) score++;
    return score;
  };

  const strength = getPasswordStrength(password);
  const passwordsMatch = password.length > 0 && password === confirmPassword;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!passwordsMatch) return;
    setIsLoading(true);

    setTimeout(() => {
      setIsLoading(false);
      setIsSuccess(true);
      setTimeout(() => {
        window.location.href = `/verify-otp?email=${encodeURIComponent(email)}`;
      }, 1000);
    }, 1200);
  };

  return (
    <div className="relative w-full max-w-[480px]">
      {/* Ambient Glow */}
      <div className="absolute -top-20 -right-20 w-48 h-48 bg-[#0045cc]/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute -bottom-20 -left-20 w-48 h-48 bg-[#dce9ff]/60 rounded-full blur-3xl pointer-events-none"></div>

      {/* Main Register Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 sm:p-10 flex flex-col gap-6">
        <div className="flex flex-col items-center text-center gap-1.5">
          <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight">
            Create new account
          </h1>
          <p className="text-sm text-[#434656] max-w-xs">
            Join us to explore tech gear and exclusive enthusiast deals
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Full Name */}
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="full-name"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Full name
            </label>
            <input
              id="full-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Alex Morgan"
              required
              className="w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
            />
          </div>

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
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alex.morgan@example.com"
              required
              className="w-full h-11 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
            />
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
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                required
                minLength={8}
                className="w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
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
            {/* Strength meter indicator */}
            {password.length > 0 && (
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
                Confirm password
              </label>
              {confirmPassword.length > 0 && (
                <span
                  className={`text-[11px] font-semibold ${
                    passwordsMatch ? "text-emerald-600" : "text-rose-500"
                  }`}
                >
                  {passwordsMatch ? "Matches" : "Doesn't match"}
                </span>
              )}
            </div>
            <div className="relative flex items-center">
              <input
                id="confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Repeat your password"
                required
                className="w-full h-11 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label="Toggle password confirmation visibility"
                className="absolute right-3 text-[#737688] hover:text-[#0b1c30] p-1 rounded-lg transition-colors cursor-pointer"
              >
                {showConfirmPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* Terms checkbox */}
          <div className="flex items-start gap-2.5 pt-1">
            <input
              id="terms-check"
              type="checkbox"
              checked={agreeTerms}
              onChange={(e) => setAgreeTerms(e.target.checked)}
              required
              className="w-4 h-4 mt-0.5 rounded text-[#0d5bff] focus:ring-0 accent-[#0d5bff] cursor-pointer"
            />
            <label
              htmlFor="terms-check"
              className="text-xs text-[#434656] cursor-pointer leading-relaxed"
            >
              I agree to the{" "}
              <Link href="#" className="text-[#0045cc] font-semibold hover:underline">
                Terms of Service
              </Link>{" "}
              and acknowledge the{" "}
              <Link href="#" className="text-[#0045cc] font-semibold hover:underline">
                Privacy Statement
              </Link>
              .
            </label>
          </div>

          <button
            type="submit"
            disabled={isLoading || isSuccess || !agreeTerms || (confirmPassword.length > 0 && !passwordsMatch)}
            className="w-full h-12 mt-2 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Creating account...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Account Created!</span>
              </>
            ) : (
              <>
                <span>Create account</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="text-center pt-1">
          <p className="text-sm text-[#434656]">
            Already have an account?{" "}
            <Link
              href="/login"
              className="text-sm font-semibold text-[#0045cc] hover:underline ml-1"
            >
              Log in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
