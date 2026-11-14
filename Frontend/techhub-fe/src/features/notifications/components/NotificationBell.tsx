"use client";

import React, { useState, useRef } from "react";
import { Bell } from "lucide-react";
import { useUnreadNotificationCount } from "../api/notificationQueries";
import { useNotificationContext } from "@/providers/NotificationProvider";
import { NotificationDropdown } from "./NotificationDropdown";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { PortalContext } from "../types";

interface NotificationBellProps {
  portalContext?: PortalContext;
  className?: string;
  iconClassName?: string;
}

export function NotificationBell({
  portalContext = "buyer",
  className = "",
  iconClassName = "w-5 h-5",
}: NotificationBellProps) {
  const { isAuthenticated } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // 1. Fetch unread count & formatted badge
  const { badgeText, hasUnread } = useUnreadNotificationCount({
    enabled: isAuthenticated,
  });

  // 2. Stream state from global NotificationContext
  const { hasNewPing } = useNotificationContext();

  if (!isAuthenticated) return null;

  return (
    <div className="relative inline-block">
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className={`relative p-2 rounded-xl text-slate-700 hover:text-primary hover:bg-slate-100 transition-all cursor-pointer focus:outline-hidden ${className} ${
          hasNewPing ? "animate-bounce" : ""
        }`}
        aria-label="Thông báo"
        title="Thông báo"
      >
        <Bell className={iconClassName} />

        {/* Badge counter */}
        {hasUnread && (
          <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[10px] font-extrabold flex items-center justify-center shadow-xs border-2 border-white pointer-events-none">
            {badgeText}
            {hasNewPing && (
              <span className="absolute inset-0 rounded-full bg-red-400 animate-ping opacity-75" />
            )}
          </span>
        )}
      </button>

      {/* Popover Dropdown */}
      <NotificationDropdown
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        portalContext={portalContext}
        triggerRef={triggerRef}
      />
    </div>
  );
}
