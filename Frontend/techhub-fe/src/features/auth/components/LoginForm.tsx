"use client";

import { useState } from "react";
import Link from "next/link";
import { Eye, EyeOff, ArrowRight, Loader2, CheckCircle2 } from "lucide-react";

export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false);
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    setTimeout(() => {
      setIsLoading(false);
      setIsSuccess(true);
      setTimeout(() => {
        setIsSuccess(false);
        window.location.href = "/";
      }, 1000);
    }, 1200);
  };

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

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="identifier"
              className="text-xs font-semibold text-[#0b1c30]"
            >
              Email or phone number
            </label>
            <input
              id="identifier"
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="name@example.com"
              required
              className="w-full h-12 px-4 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
            />
          </div>

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
                placeholder="••••••••••••"
                required
                className="w-full h-12 pl-4 pr-11 bg-[#eff4ff]/60 text-[#0b1c30] text-sm rounded-xl outline-none focus:bg-white focus:ring-2 focus:ring-[#0d5bff]/25 transition-all placeholder:text-[#737688]/60"
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
          </div>

          <div className="flex items-center justify-between pt-1">
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="w-4 h-4 rounded text-[#0d5bff] focus:ring-0 accent-[#0d5bff] cursor-pointer"
              />
              <span className="text-xs text-[#434656]">Remember me</span>
            </label>
            <Link
              href="/forgot-password"
              className="text-xs font-semibold text-[#0045cc] hover:underline"
            >
              Forgot password?
            </Link>
          </div>

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
          onClick={() => {}}
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
              href="/register"
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
