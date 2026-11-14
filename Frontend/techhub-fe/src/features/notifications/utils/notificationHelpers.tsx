import type React from "react";
import {
  Truck,
  Star,
  TicketPercent,
  ShieldCheck,
  Store,
  Bell,
} from "lucide-react";
import type { NotificationType } from "../types";

export interface NotificationIconStyle {
  icon: React.ElementType;
  bg: string;
  text: string;
  border: string;
}

export interface NotificationCategoryConfig {
  key: "ALL" | NotificationType;
  label: string;
  icon: React.ElementType;
}

export const NOTIFICATION_CATEGORIES: NotificationCategoryConfig[] = [
  { key: "ALL", label: "Tất cả", icon: Bell },
  { key: "ORDER", label: "Đơn hàng", icon: Truck },
  { key: "PROMOTION", label: "Khuyến mãi", icon: TicketPercent },
  { key: "REVIEW", label: "Đánh giá", icon: Star },
  { key: "SYSTEM", label: "Hệ thống", icon: ShieldCheck },
  { key: "SHOP", label: "Gian hàng", icon: Store },
];

export function formatNotificationTime(dateStr: string): string {
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) return "Vừa xong";
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay === 1) return "Hôm qua";
    if (diffDay < 7) return `${diffDay} ngày trước`;

    return date.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
}

export function getNotificationIcon(type: NotificationType): NotificationIconStyle {
  switch (type) {
    case "ORDER":
      return {
        icon: Truck,
        bg: "bg-blue-50",
        text: "text-blue-600",
        border: "border-blue-100",
      };
    case "REVIEW":
      return {
        icon: Star,
        bg: "bg-amber-50",
        text: "text-amber-600",
        border: "border-amber-100",
      };
    case "PROMOTION":
      return {
        icon: TicketPercent,
        bg: "bg-purple-50",
        text: "text-purple-600",
        border: "border-purple-100",
      };
    case "SHOP":
      return {
        icon: Store,
        bg: "bg-emerald-50",
        text: "text-emerald-600",
        border: "border-emerald-100",
      };
    case "SYSTEM":
    default:
      return {
        icon: ShieldCheck,
        bg: "bg-primary/10",
        text: "text-primary",
        border: "border-primary/20",
      };
  }
}

/**
 * Normalizes Jackson dual-field boolean serialization (`read` vs `isRead`)
 * ensuring `isRead: boolean` is consistently populated.
 */
export function normalizeNotification(
  raw: Record<string, unknown>
): import("../types").NotificationResponse {
  return {
    ...raw,
    isRead: Boolean(raw.isRead ?? raw.read),
  } as unknown as import("../types").NotificationResponse;
}
