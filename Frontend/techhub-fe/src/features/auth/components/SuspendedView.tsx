"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Lock, ShieldAlert, Headphones, ArrowLeft } from "lucide-react";
import { clearTokens } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";

export function SuspendedView() {
  useEffect(() => {
    clearTokens();
  }, []);

  const info = {
    accountEmail: "support@techhub.market",
    detectionFlag: "Security Policy Flag / Admin Lock",
    status: "Suspended",
    supportEmail: "support@techhub.market",
    referenceCode: "TH-SECURITY",
  };

  return (
    <div className="relative w-full max-w-[480px] flex flex-col items-center">
      {/* Ambient backdrop subtle blue & red glow */}
      <div className="absolute -top-10 left-1/2 -translate-x-1/2 w-72 h-72 bg-[#0045cc]/10 rounded-full blur-3xl pointer-events-none -z-10"></div>
      <div className="absolute -bottom-10 left-1/2 -translate-x-1/2 w-64 h-64 bg-[#ba1a1a]/5 rounded-full blur-3xl pointer-events-none -z-10"></div>

      {/* Main Card */}
      <div className="w-full bg-white rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] p-8 md:p-10 flex flex-col items-center text-center relative">
        {/* High-Impact Lock Alert Badge */}
        <div className="relative flex items-center justify-center mb-6">
          <div className="absolute w-24 h-24 rounded-full bg-[#ffdad4]/60 animate-ping opacity-30"></div>
          <div className="w-20 h-20 rounded-full bg-white shadow-md flex items-center justify-center relative z-10">
            <div className="w-14 h-14 rounded-full bg-[#ffdad4] flex items-center justify-center">
              <div className="w-10 h-10 rounded-full bg-[#ce2917] flex items-center justify-center shadow-inner text-white">
                <Lock className="w-5 h-5" />
              </div>
            </div>
          </div>
        </div>

        {/* Title */}
        <h1 className="font-heading text-2xl sm:text-[28px] font-bold text-[#0b1c30] tracking-tight mb-2">
          Account Suspended
        </h1>

        {/* Description */}
        <p className="text-sm text-[#434656] max-w-[380px] mx-auto mb-6 leading-relaxed">
          Your account has been temporarily locked or suspended due to security policy violations or administrative action. Please contact our support team for assistance.
        </p>

        {/* Technical Status Strip */}
        <div className="w-full bg-[#eff4ff]/70 rounded-xl p-4 mb-6 flex items-center justify-between text-left">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-8 h-8 rounded-lg bg-[#d3e4fe] flex items-center justify-center text-[#0045cc] shrink-0">
              <ShieldAlert className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <p className="text-[11px] font-bold uppercase tracking-wider text-[#737688]">
                Detection Flag
              </p>
              <p className="text-sm font-semibold text-[#0b1c30] truncate">
                {info.detectionFlag}
              </p>
            </div>
          </div>
          <div className="text-right shrink-0">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full bg-[#ffdad4] text-[#a90900] text-[11px] font-bold uppercase tracking-wide">
              {info.status}
            </span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="w-full flex flex-col gap-3">
          <a
            href={`mailto:${info.supportEmail}?subject=Account%20Suspension%20Appeal%20-%20Ref%20${info.referenceCode}`}
            className="w-full h-11 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold flex items-center justify-center gap-2 shadow-md shadow-[#0045cc]/20 transition-all active:scale-[0.98]"
          >
            <Headphones className="w-4 h-4" />
            <span>Contact Support</span>
          </a>

          <Link
            href={ROUTES.LOGIN}
            className="w-full h-10 rounded-xl bg-transparent hover:bg-[#eff4ff] text-[#434656] hover:text-[#0b1c30] text-sm font-semibold flex items-center justify-center gap-1.5 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Login</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
