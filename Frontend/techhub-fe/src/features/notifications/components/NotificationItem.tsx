"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import type { NotificationResponse, PortalContext } from "../types";
import { resolveNotificationRoute } from "../utils/resolveNotificationRoute";
import {
  formatNotificationTime,
  getNotificationIcon,
} from "../utils/notificationHelpers";

interface NotificationItemProps {
  notification: NotificationResponse;
  onMarkAsRead: (id: string) => void;
  onDelete?: (id: string, wasUnread?: boolean) => void;
  onItemClick?: () => void;
  portalContext?: PortalContext;
  showDeleteButton?: boolean;
}

export function NotificationItem({
  notification,
  onMarkAsRead,
  onDelete,
  onItemClick,
  portalContext = "buyer",
  showDeleteButton = false,
}: NotificationItemProps) {
  const router = useRouter();
  const isUnread = !notification.isRead;

  const { icon: IconComponent, bg, text, border } = getNotificationIcon(
    notification.type
  );

  const handleClick = () => {
    // 1. Mark as read if unread
    if (isUnread) {
      onMarkAsRead(notification.id);
    }

    // 2. Callback for dropdown close if needed
    onItemClick?.();

    // 3. Centralized route resolution based on portalContext
    const targetRoute = resolveNotificationRoute(notification, portalContext);
    router.push(targetRoute);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete?.(notification.id, isUnread);
  };

  return (
    <div
      onClick={handleClick}
      className={`group relative flex items-start gap-3.5 p-3.5 sm:p-4 transition-colors cursor-pointer border-b border-slate-100 last:border-b-0 ${
        isUnread
          ? "bg-primary/5 hover:bg-primary/10 border-l-[3px] border-l-primary"
          : "bg-white hover:bg-slate-50/80"
      }`}
    >
      {/* Icon Badge */}
      <div
        className={`w-9 h-9 sm:w-10 sm:h-10 rounded-xl ${bg} ${text} ${border} border flex items-center justify-center shrink-0 mt-0.5 shadow-xs transition-transform group-hover:scale-105`}
      >
        <IconComponent className="w-4 h-4 sm:w-5 sm:h-5" />
      </div>

      {/* Main Content */}
      <div className="flex-1 min-w-0 pr-1">
        <div className="flex items-center justify-between gap-2">
          <h4
            className={`text-xs sm:text-[13px] leading-snug line-clamp-1 ${
              isUnread
                ? "font-bold text-slate-900"
                : "font-semibold text-slate-800"
            }`}
          >
            {notification.title}
          </h4>

          {/* Time & Unread Dot */}
          <div className="flex items-center gap-1.5 shrink-0">
            <span className="text-[11px] text-slate-500 whitespace-nowrap">
              {formatNotificationTime(notification.createdAt)}
            </span>
            {isUnread && (
              <span
                className="w-2 h-2 rounded-full bg-primary shrink-0"
                title="Chưa đọc"
              />
            )}
          </div>
        </div>

        <p className="text-xs text-slate-600 line-clamp-2 mt-1 leading-relaxed">
          {notification.content}
        </p>
      </div>

      {/* Delete button (hover on desktop or toggleable) */}
      {showDeleteButton && onDelete && (
        <button
          type="button"
          onClick={handleDelete}
          title="Xóa thông báo"
          className="opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus:opacity-100 p-1.5 rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 transition-all shrink-0 cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
}
