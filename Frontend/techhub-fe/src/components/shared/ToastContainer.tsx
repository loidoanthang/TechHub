"use client";

import { useEffect, useState } from "react";
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from "lucide-react";
import type { ToastItem, ToastType } from "@/lib/toast";

export function ToastContainer() {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    const handleToastEvent = (e: CustomEvent<ToastItem>) => {
      if (!e.detail) return;

      const newToast = e.detail;
      setToasts((prev) => [...prev, newToast]);

      // Auto dismiss after 4.5 seconds
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== newToast.id));
      }, 4500);
    };

    window.addEventListener("techhub:toast", handleToastEvent);
    return () => {
      window.removeEventListener("techhub:toast", handleToastEvent);
    };
  }, []);

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  if (toasts.length === 0) return null;

  return (
    <div
      aria-live="polite"
      className="fixed bottom-6 left-4 right-4 sm:left-auto sm:right-6 sm:bottom-6 z-[9999] flex flex-col gap-2.5 max-w-sm sm:w-full ml-auto pointer-events-none"
    >
      {toasts.map((toast) => {
        const isSuccess = toast.type === "success";
        const isError = toast.type === "error";
        const isWarning = toast.type === "warning";

        return (
          <div
            key={toast.id}
            role="status"
            className={`pointer-events-auto flex items-center justify-between gap-3 p-3.5 px-4 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] border transition-all duration-300 animate-in fade-in slide-in-from-bottom-3 bg-white text-[#0b1c30] ${
              isSuccess
                ? "border-emerald-200 shadow-emerald-500/5"
                : isError
                ? "border-rose-200 shadow-rose-500/5"
                : isWarning
                ? "border-amber-200 shadow-amber-500/5"
                : "border-[#dce9ff] shadow-[#0045cc]/5"
            }`}
          >
            <div className="flex items-center gap-2.5 min-w-0">
              {isSuccess && (
                <div className="w-6 h-6 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 stroke-[2.5]" />
                </div>
              )}
              {isError && (
                <div className="w-6 h-6 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
                  <AlertCircle className="w-4 h-4 text-rose-600 stroke-[2.5]" />
                </div>
              )}
              {isWarning && (
                <div className="w-6 h-6 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                  <AlertTriangle className="w-4 h-4 text-amber-600 stroke-[2.5]" />
                </div>
              )}
              {!isSuccess && !isError && !isWarning && (
                <div className="w-6 h-6 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center shrink-0">
                  <Info className="w-4 h-4 text-[#0045cc] stroke-[2.5]" />
                </div>
              )}
              <span className="text-xs font-semibold text-[#0b1c30] leading-snug break-words">
                {toast.message}
              </span>
            </div>

            <button
              type="button"
              onClick={() => removeToast(toast.id)}
              className="p-1 text-slate-400 hover:text-slate-700 rounded-lg transition-colors shrink-0 cursor-pointer"
              aria-label="Đóng thông báo"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        );
      })}
    </div>
  );
}
