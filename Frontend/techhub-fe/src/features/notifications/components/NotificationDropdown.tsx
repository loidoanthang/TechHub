"use client";

import React, { useState, useRef, useEffect } from "react";
import Link from "next/link";
import {
  CheckCheck,
  BellOff,
  ArrowRight,
  Loader2,
  Truck,
  TicketPercent,
  ShieldCheck,
} from "lucide-react";
import { NotificationItem } from "./NotificationItem";
import {
  useNotificationsQuery,
  useSystemCategoryNotificationsQuery,
  useMarkAsReadMutation,
  useMarkAllAsReadMutation,
} from "../api";
import { ROUTES } from "@/lib/constants";
import { NOTIFICATION_CATEGORIES } from "../utils/notificationHelpers";
import type { NotificationType, PortalContext } from "../types";

interface NotificationDropdownProps {
  isOpen: boolean;
  onClose: () => void;
  portalContext?: PortalContext;
  triggerRef?: React.RefObject<HTMLElement | null>;
}

type TabKey = "ALL" | NotificationType;

const DROPDOWN_CATEGORY_KEYS = ["ALL", "ORDER", "PROMOTION", "SYSTEM"] as const;
const TABS = NOTIFICATION_CATEGORIES.filter((cat) =>
  (DROPDOWN_CATEGORY_KEYS as readonly string[]).includes(cat.key)
);

export function NotificationDropdown({
  isOpen,
  onClose,
  portalContext = "buyer",
  triggerRef,
}: NotificationDropdownProps) {
  const [activeTab, setActiveTab] = useState<TabKey>("ALL");
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 1. Fetch total unread count consistently regardless of current tab
  const { data: totalResponse } = useNotificationsQuery(
    { page: 0, size: 1 },
    { enabled: isOpen }
  );

  // 2. Fetch notifications
  const isSystemTab = activeTab === "SYSTEM";
  const standardType = activeTab === "ALL" ? undefined : activeTab;

  const { data: standardResponse, isLoading: isStandardLoading } = useNotificationsQuery(
    { page: 0, size: 15, type: standardType },
    { enabled: isOpen && !isSystemTab }
  );

  const systemCategory = useSystemCategoryNotificationsQuery({
    page: 0,
    size: 15,
    enabled: isOpen && isSystemTab,
  });

  const markAsReadMutation = useMarkAsReadMutation();
  const markAllAsReadMutation = useMarkAllAsReadMutation();

  const isLoading = isSystemTab ? systemCategory.isLoading : isStandardLoading;
  const unreadCount = totalResponse?.data?.unreadCount || 0;
  const notifications = isSystemTab
    ? systemCategory.notifications
    : standardResponse?.data?.notifications?.content || [];

  // 3. Click outside & Esc key listeners
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(target) &&
        (!triggerRef?.current || !triggerRef.current.contains(target))
      ) {
        onClose();
      }
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleMarkAsRead = (id: string) => {
    markAsReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    markAllAsReadMutation.mutate();
  };

  const allNotificationsUrl =
    portalContext === "seller"
      ? ROUTES.SELLER.NOTIFICATIONS
      : ROUTES.ACCOUNT.NOTIFICATIONS;

  return (
    <div
      ref={dropdownRef}
      className="fixed inset-x-3 top-16 sm:absolute sm:inset-auto sm:right-0 sm:top-full sm:mt-2 w-auto sm:w-[420px] max-w-[420px] bg-white rounded-2xl shadow-xl border border-slate-200/80 z-50 overflow-visible flex flex-col animate-in fade-in-0 zoom-in-95 duration-150"
      style={{ maxHeight: "calc(100vh - 100px)" }}
    >
      {/* Top arrow caret pointing to Bell on Desktop */}
      <div
        className="hidden sm:block absolute -top-1.5 right-3.5 w-3 h-3 bg-white border-t border-l border-slate-200/80 transform rotate-45 z-10"
        aria-hidden="true"
      />

      {/* 1. Header */}
      <div className="px-4 py-3 bg-white border-b border-slate-100 flex items-center justify-between shrink-0 rounded-t-2xl z-20">
        <div className="flex items-center gap-2">
          <h3 className="font-heading font-bold text-sm text-slate-900">
            Thông báo
          </h3>
          {unreadCount > 0 && (
            <span className="px-2 py-0.5 text-[10px] font-extrabold rounded-full bg-primary/10 text-primary">
              {unreadCount > 99 ? "99+" : unreadCount} mới
            </span>
          )}
        </div>

        <button
          type="button"
          onClick={handleMarkAllAsRead}
          disabled={unreadCount === 0 || markAllAsReadMutation.isPending}
          title={
            unreadCount === 0
              ? "Không có thông báo mới cần đọc"
              : "Đánh dấu tất cả thông báo là đã đọc"
          }
          aria-label="Đánh dấu tất cả thông báo là đã đọc"
          className="flex items-center gap-1.5 text-xs font-semibold text-primary hover:text-primary-container hover:underline disabled:opacity-40 disabled:hover:no-underline disabled:cursor-not-allowed transition-all cursor-pointer"
        >
          <CheckCheck className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Đánh dấu tất cả đã đọc</span>
          <span className="inline sm:hidden">Đọc tất cả</span>
        </button>
      </div>

      {/* 2. Filter Tabs */}
      <div className="flex items-center gap-1 px-3 py-2 bg-slate-50/70 border-b border-slate-100 overflow-x-auto scrollbar-none shrink-0 z-20">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`px-3 py-1 rounded-lg text-xs font-medium transition-all shrink-0 cursor-pointer flex items-center gap-1.5 ${
                isActive
                  ? "bg-primary text-white shadow-xs"
                  : "text-slate-600 hover:text-slate-900 hover:bg-slate-200/60"
              }`}
            >
              {tab.icon && (
                <tab.icon
                  className={`w-3 h-3 ${isActive ? "text-white" : "text-slate-400"}`}
                />
              )}
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* 3. Notification List with scroll */}
      <div className="flex-1 overflow-y-auto overscroll-contain divide-y divide-slate-100 max-h-[360px] sm:max-h-[420px] z-20">
        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-2">
            <Loader2 className="w-5 h-5 animate-spin text-primary" />
            <span className="text-xs">Đang tải thông báo...</span>
          </div>
        ) : notifications.length === 0 ? (
          <div className="py-12 px-4 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400 mb-2.5">
              <BellOff className="w-6 h-6" />
            </div>
            <p className="text-xs font-semibold text-slate-700">
              Không có thông báo nào
            </p>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Các thông báo mới sẽ xuất hiện tại đây
            </p>
          </div>
        ) : (
          notifications.map((item) => (
            <NotificationItem
              key={item.id}
              notification={item}
              onMarkAsRead={handleMarkAsRead}
              onItemClick={onClose}
              portalContext={portalContext}
            />
          ))
        )}
      </div>

      {/* 4. Footer (Storefront Buyer & Seller Centre only) */}
      {portalContext !== "admin" && (
        <div className="p-2.5 bg-slate-50/80 border-t border-slate-100 shrink-0 text-center rounded-b-2xl z-20">
          <Link
            href={allNotificationsUrl}
            onClick={onClose}
            className="inline-flex items-center justify-center gap-1 text-xs font-bold text-primary hover:text-primary-container hover:underline py-1 px-3 rounded-lg hover:bg-primary/5 transition-colors w-full"
          >
            <span>Xem tất cả thông báo</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      )}
    </div>
  );
}
