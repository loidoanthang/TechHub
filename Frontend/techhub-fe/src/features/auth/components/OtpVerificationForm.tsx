"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Clock, ArrowRight, ArrowLeft, RefreshCw, Loader2, CheckCircle2 } from "lucide-react";

export function OtpVerificationForm() {
  const searchParams = useSearchParams();
  const emailParam = searchParams.get("email") || "alex.morgan@example.com";

  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const [timeLeft, setTimeLeft] = useState(58);
  const [canResend, setCanResend] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [mounted, setMounted] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (timeLeft <= 0) {
      setCanResend(true);
      return;
    }
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          setCanResend(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  const handleChange = (index: number, val: string) => {
    const numericVal = val.replace(/\D/g, "");
    if (!numericVal) {
      const newDigits = [...digits];
      newDigits[index] = "";
      setDigits(newDigits);
      return;
    }

    const lastChar = numericVal.slice(-1);
    const newDigits = [...digits];
    newDigits[index] = lastChar;
    setDigits(newDigits);

    // Auto focus next input
    if (index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace") {
      if (!digits[index] && index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
    } else if (e.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === "ArrowRight" && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
    if (!pasted) return;

    const newDigits = [...digits];
    for (let i = 0; i < pasted.length; i++) {
      newDigits[i] = pasted[i];
    }
    setDigits(newDigits);
    const focusTarget = Math.min(pasted.length, 5);
    inputRefs.current[focusTarget]?.focus();
  };

  const handleResend = () => {
    if (!canResend) return;
    setTimeLeft(60);
    setCanResend(false);
    setDigits(["", "", "", "", "", ""]);
    inputRefs.current[0]?.focus();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (digits.some((d) => !d)) return;

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setIsSuccess(true);
      setTimeout(() => {
        window.location.href = "/";
      }, 1200);
    }, 1200);
  };

  const maskedEmail = emailParam.replace(/(^.{2})(.*)(@.*$)/, "$1***$3");

  return (
    <div className="relative w-full max-w-[460px]">
      {/* Ambient decorative glow */}
      <div className="absolute -top-16 left-1/2 -translate-x-1/2 w-80 h-80 rounded-full bg-[#0045cc]/5 blur-3xl pointer-events-none"></div>

      {/* Main Card */}
      <div className="relative w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-6 sm:p-9 flex flex-col items-center">
        {/* Title */}
        <div className="text-center mb-8">
          <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight mb-2">
            Verify OTP
          </h1>
          <p className="text-sm text-[#434656] max-w-[280px] mx-auto">
            Please enter the 6-digit verification code sent to your email
          </p>
          <div className="mt-2 inline-flex items-center justify-center">
            <span className="font-mono text-xs font-semibold text-[#0b1c30] bg-[#eff4ff] px-2.5 py-1 rounded-md">
              {maskedEmail}
            </span>
          </div>
        </div>

        {/* 6 Digit Input Grid */}
        <form onSubmit={handleSubmit} className="w-full flex flex-col items-center">
          <div className="w-full grid grid-cols-6 gap-2 sm:gap-2.5 mb-6">
            {digits.map((digit, idx) => (
              <input
                key={idx}
                ref={(el) => {
                  inputRefs.current[idx] = el;
                }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleChange(idx, e.target.value)}
                onKeyDown={(e) => handleKeyDown(idx, e)}
                onPaste={handlePaste}
                className={`w-full aspect-[4/5] rounded-xl text-center font-heading text-xl sm:text-2xl font-bold text-[#0b1c30] outline-none transition-all shadow-sm ${
                  digit
                    ? "bg-white border-2 border-[#0045cc] ring-2 ring-[#0045cc]/15"
                    : "bg-[#eff4ff]/60 border border-transparent focus:bg-white focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
                }`}
              />
            ))}
          </div>

          {/* Countdown indicator */}
          <div className="flex items-center justify-center gap-1.5 mb-6 text-sm text-[#434656]">
            <Clock className="w-4 h-4 text-[#737688]" />
            <span>
              Resend code in{" "}
              <span className="font-mono font-semibold text-[#0045cc]">
                {mounted ? (timeLeft > 0 ? `${timeLeft}s` : "0s") : "58s"}
              </span>
            </span>
          </div>

          {/* Submit button */}
          <button
            type="submit"
            disabled={isLoading || isSuccess || digits.some((d) => !d)}
            className="w-full h-12 bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold rounded-xl shadow-md shadow-[#0045cc]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.99] disabled:opacity-70 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Verifying...</span>
              </>
            ) : isSuccess ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-300" />
                <span>Verified!</span>
              </>
            ) : (
              <>
                <span>Verify Code</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>

          {/* Resend button */}
          <div className="mt-5 text-center text-sm text-[#434656]">
            Didn&apos;t receive the code?{" "}
            <button
              type="button"
              onClick={handleResend}
              disabled={!canResend}
              className={`font-semibold inline-flex items-center gap-1 ml-1 ${
                !canResend
                  ? "text-[#737688] opacity-50 cursor-not-allowed"
                  : "text-[#0045cc] hover:underline cursor-pointer"
              }`}
            >
              <span>Resend</span>
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Back to login */}
          <div className="w-full mt-6 pt-4 border-t border-[#eff4ff] flex items-center justify-center">
            <Link
              href="/login"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#434656] hover:text-[#0b1c30] transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to login</span>
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
