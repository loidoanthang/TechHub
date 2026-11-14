"use client";

import React, { useState } from "react";
import {
  Bell,
  CheckCheck,
  BellOff,
  Loader2,
  Filter,
  ChevronDown,
} from "lucide-react";
import { NotificationItem } from "./NotificationItem";
import {
  useNotificationsQuery,
  useSystemCategoryNotificationsQuery,
  useMarkAsReadMutation,
  useMarkAllAsReadMutation,
  useDeleteNotificationMutation,
} from "../api";
import { Pagination } from "@/components/shared/Pagination";
import { toast } from "@/lib/toast";
import { NOTIFICATION_CATEGORIES } from "../utils/notificationHelpers";
import type { NotificationType, PortalContext } from "../types";

interface NotificationsPageViewProps {
  portalContext?: PortalContext;
  title?: string;
  description?: string;
}

type TabType = "ALL" | NotificationType;

export function NotificationsPageView({
  portalContext = "buyer",
  title = "Thông báo của tôi",
  description = "Theo dõi cập nhật đơn hàng, đánh giá, mã giảm giá và tin tức hệ thống",
}: NotificationsPageViewProps) {
  const [selectedTab, setSelectedTab] = useState<TabType>("ALL");
  const [readFilter, setReadFilter] = useState<"ALL" | "UNREAD" | "READ">("ALL");
  const [page, setPage] = useState(0);
  const pageSize = 20;

  // 5 Canonical tabs per Spec lines 158-162 ("Hệ thống" groups both SYSTEM and SHOP)
  const tabs = NOTIFICATION_CATEGORIES.filter((cat) => cat.key !== "SHOP");

  const isReadParam =
    readFilter === "ALL" ? undefined : readFilter === "READ";
  const typeParam = selectedTab === "ALL" ? undefined : selectedTab;
  const isSystemTab = selectedTab === "SYSTEM";

  // Standard query when not SYSTEM tab
  const { data: standardResponse, isLoading: isStandardLoading } =
    useNotificationsQuery(
      {
        type: typeParam,
        isRead: isReadParam,
        page,
        size: pageSize,
      },
      { enabled: !isSystemTab }
    );

  // When SYSTEM tab is selected, aggregate both SYSTEM and SHOP events (Spec lines 162 & 186)
  const systemCategory = useSystemCategoryNotificationsQuery({
    page,
    size: pageSize,
    isRead: isReadParam,
    enabled: isSystemTab,
  });

  const markAsReadMutation = useMarkAsReadMutation();
  const markAllAsReadMutation = useMarkAllAsReadMutation();
  const deleteMutation = useDeleteNotificationMutation();

  const isLoading = isSystemTab ? systemCategory.isLoading : isStandardLoading;
  const notifications = isSystemTab
    ? systemCategory.notifications
    : standardResponse?.data?.notifications?.content || [];
  const totalElements = isSystemTab
    ? systemCategory.totalElements
    : standardResponse?.data?.notifications?.totalElements || 0;
  const totalPages = isSystemTab
    ? systemCategory.totalPages
    : standardResponse?.data?.notifications?.totalPages || 0;
  const unreadCount = isSystemTab
    ? systemCategory.unreadCount
    : standardResponse?.data?.unreadCount || 0;

  const handleSelectTab = (tab: TabType) => {
    setSelectedTab(tab);
    setPage(0);
  };

  const handleSelectReadFilter = (filter: "ALL" | "UNREAD" | "READ") => {
    setReadFilter(filter);
    setPage(0);
  };

  const handleMarkAsRead = (id: string) => {
    markAsReadMutation.mutate(id);
  };

  const handleMarkAllAsRead = () => {
    markAllAsReadMutation.mutate(undefined, {
      onSuccess: () => {
        toast.success("Đã đánh dấu tất cả thông báo là đã đọc!");
      },
    });
  };

  const handleDelete = (id: string, wasUnread?: boolean) => {
    deleteMutation.mutate(
      { id, wasUnread },
      {
        onSuccess: () => {
          toast.success("Đã xóa thông báo khỏi danh sách.");
        },
      }
    );
  };

  return (
    <div className="w-full space-y-6 pb-12">
      {/* 1. Header with Title & Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold font-heading text-slate-900 tracking-tight flex items-center gap-2.5">
            <Bell className="w-6 h-6 text-primary" />
            <span>{title}</span>
            {unreadCount > 0 && (
              <span className="px-2.5 py-0.5 text-xs font-extrabold rounded-full bg-primary/10 text-primary">
                {unreadCount > 99 ? "99+" : unreadCount} chưa đọc
              </span>
            )}
          </h1>
          <p className="text-xs text-slate-500 mt-1">{description}</p>
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
          className="self-start sm:self-auto py-2 px-3.5 rounded-xl border border-slate-200 hover:border-primary/40 bg-white hover:bg-slate-50 text-xs font-bold text-primary disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center gap-2 shadow-2xs cursor-pointer active:scale-98"
        >
          <CheckCheck className="w-4 h-4" />
          <span>Đánh dấu tất cả đã đọc</span>
        </button>
      </div>

      {/* 2. Filters Card */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        {/* Categories Tab Strip */}
        <div className="flex items-center gap-1.5 p-3 sm:px-4 sm:py-3 border-b border-slate-100 overflow-x-auto scrollbar-none">
          {tabs.map((tab) => {
            const isActive = selectedTab === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => handleSelectTab(tab.key)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap flex items-center gap-1.5 ${
                  isActive
                    ? "bg-primary text-white shadow-xs"
                    : "text-slate-600 hover:text-slate-900 hover:bg-slate-100"
                }`}
              >
                {tab.icon && <tab.icon className="w-3.5 h-3.5" />}
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Read Status Dropdown Filter */}
        <div className="px-4 py-2.5 bg-slate-50/60 flex items-center justify-between text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <span className="font-semibold text-slate-700">Trạng thái:</span>
            <div className="relative inline-block">
              <select
                value={readFilter}
                onChange={(e) =>
                  handleSelectReadFilter(e.target.value as "ALL" | "UNREAD" | "READ")
                }
                aria-label="Lọc theo trạng thái đọc"
                className="appearance-none bg-white border border-slate-200 text-xs font-semibold text-slate-800 rounded-lg pl-3 pr-7 py-1 hover:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all cursor-pointer shadow-2xs"
              >
                <option value="ALL">Tất cả</option>
                <option value="UNREAD">Chưa đọc</option>
                <option value="READ">Đã đọc</option>
              </select>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>
          </div>

          <span className="text-[11px] font-medium hidden sm:inline">
            Tổng cộng: <strong className="text-slate-700">{totalElements}</strong> thông báo
          </span>
        </div>
      </div>

      {/* 3. Notifications List */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden divide-y divide-slate-100">
        {isLoading ? (
          <div className="p-16 flex flex-col items-center justify-center gap-3 text-slate-500">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <span className="text-xs font-semibold">Đang tải danh sách thông báo...</span>
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-16 text-center flex flex-col items-center justify-center gap-3">
            <div className="w-16 h-16 rounded-3xl bg-slate-100 text-slate-400 flex items-center justify-center">
              <BellOff className="w-8 h-8" />
            </div>
            <h3 className="text-sm font-bold text-slate-800">
              Không có thông báo nào phù hợp
            </h3>
            <p className="text-xs text-slate-500 max-w-sm">
              Bạn hiện chưa có thông báo nào trong danh mục hoặc bộ lọc đã chọn.
            </p>
          </div>
        ) : (
          notifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              onMarkAsRead={handleMarkAsRead}
              onDelete={handleDelete}
              portalContext={portalContext}
              showDeleteButton={true}
            />
          ))
        )}

        {/* 4. Pagination */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-100">
            <Pagination
              page={page}
              size={pageSize}
              totalElements={totalElements}
              totalPages={totalPages}
              onPageChange={setPage}
              variant="bottom-full"
            />
          </div>
        )}
      </div>
    </div>
  );
}
