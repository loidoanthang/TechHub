"use client";

import { useEffect, useState } from "react";
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from "lucide-react";
import type { ToastItem, ToastType } from "@/lib/toast";

export function ToastContainer() {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    const handleToastEvent = (e: Event) => {
      const customEvent = e as CustomEvent<ToastItem>;
      if (!customEvent.detail) return;

      const newToast = customEvent.detail;
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
      className="fixed bottom-5 right-5 z-[9999] flex flex-col gap-2.5 max-w-sm w-full pointer-events-none"
    >
      {toasts.map((toast) => {
        const isSuccess = toast.type === "success";
        const isError = toast.type === "error";
        const isWarning = toast.type === "warning";

        return (
          <div
            key={toast.id}
            role="status"
            className={`pointer-events-auto flex items-center justify-between gap-3 p-3.5 rounded-2xl shadow-xl border backdrop-blur-md transition-all duration-300 animate-in fade-in slide-in-from-bottom-3 ${
              isSuccess
                ? "bg-slate-900/95 text-white border-emerald-500/30"
                : isError
                ? "bg-slate-900/95 text-white border-rose-500/30"
                : isWarning
                ? "bg-slate-900/95 text-white border-amber-500/30"
                : "bg-slate-900/95 text-white border-blue-500/30"
            }`}
          >
            <div className="flex items-center gap-2.5 min-w-0">
              {isSuccess && <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />}
              {isError && <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />}
              {isWarning && <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />}
              {!isSuccess && !isError && !isWarning && (
                <Info className="w-4 h-4 text-blue-400 shrink-0" />
              )}
              <span className="text-xs font-medium text-slate-100 leading-snug break-words">
                {toast.message}
              </span>
            </div>

            <button
              type="button"
              onClick={() => removeToast(toast.id)}
              className="p-1 text-slate-400 hover:text-white rounded-lg transition-colors shrink-0 cursor-pointer"
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
