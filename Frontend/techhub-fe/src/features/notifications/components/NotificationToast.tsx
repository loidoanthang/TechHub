"use client";

import React, { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { X, ArrowRight } from "lucide-react";
import { useMarkAsReadMutation } from "../api";
import { getNotificationIcon } from "../utils/notificationHelpers";
import { resolveNotificationRoute } from "../utils/resolveNotificationRoute";
import { useNotificationContext } from "@/providers/NotificationProvider";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { PortalContext } from "../types";

export function NotificationToast() {
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuth();
  const { activeToast, dismissToast } = useNotificationContext();
  const markAsReadMutation = useMarkAsReadMutation();

  const isSeller = Boolean(user?.roles?.includes("SELLER"));

  let portalContext: PortalContext = "buyer";
  if (pathname?.startsWith("/admin")) {
    portalContext = "admin";
  } else if (
    pathname?.startsWith("/seller") ||
    (isSeller &&
      (activeToast?.type === "REVIEW" || activeToast?.type === "SHOP"))
  ) {
    portalContext = "seller";
  }

  // Auto-dismiss after 5 seconds
  useEffect(() => {
    if (!activeToast) return;

    const timer = setTimeout(() => {
      dismissToast();
    }, 5000);

    return () => clearTimeout(timer);
  }, [activeToast, dismissToast]);

  if (!activeToast) return null;

  const { icon: IconComponent, bg, text, border } = getNotificationIcon(
    activeToast.type
  );

  const handleAction = () => {
    if (!activeToast.isRead) {
      markAsReadMutation.mutate(activeToast.id);
    }
    const targetRoute = resolveNotificationRoute(activeToast, portalContext);
    dismissToast();
    router.push(targetRoute);
  };

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed bottom-4 right-4 z-50 w-[calc(100vw-2rem)] sm:w-[380px] bg-white rounded-2xl shadow-xl border border-slate-200/80 p-4 flex items-start gap-3.5 animate-in slide-in-from-bottom-5 duration-300"
    >
      {/* Icon Badge */}
      <div
        className={`w-9 h-9 rounded-xl ${bg} ${text} ${border} border flex items-center justify-center shrink-0 mt-0.5 shadow-2xs`}
      >
        <IconComponent className="w-4 h-4" />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0 pr-1">
        <h4 className="text-xs font-bold text-slate-900 leading-snug line-clamp-1">
          {activeToast.title}
        </h4>
        <p className="text-xs text-slate-600 line-clamp-2 mt-1 leading-relaxed">
          {activeToast.content}
        </p>

        <div className="mt-2.5 flex items-center gap-3">
          <button
            type="button"
            onClick={handleAction}
            className="text-xs font-bold text-primary hover:text-primary-container hover:underline flex items-center gap-1 cursor-pointer"
          >
            <span>Xem ngay</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <button
        type="button"
        onClick={dismissToast}
        className="p-1 rounded-lg text-slate-500 hover:text-slate-700 hover:bg-slate-100 transition-colors shrink-0 cursor-pointer"
        aria-label="Đóng"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}
