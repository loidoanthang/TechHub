"use client";

import React from "react";

export type SellerOrderTabKey =
  | "ALL"
  | "PENDING_CONFIRMATION"
  | "CONFIRMED"
  | "PREPARING"
  | "WAITING_FOR_PICKUP"
  | "SHIPPING"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "CANCELLATIONS"
  | "RETURNS";

interface SellerOrdersTabsProps {
  activeTab: SellerOrderTabKey;
  onTabChange: (tab: SellerOrderTabKey) => void;
  counts?: Partial<Record<SellerOrderTabKey, number>>;
}

export function SellerOrdersTabs({
  activeTab,
  onTabChange,
  counts = {},
}: SellerOrdersTabsProps) {
  const tabs: { key: SellerOrderTabKey; label: string; badgeVariant?: string }[] = [
    { key: "ALL", label: "Tất cả" },
    { key: "PENDING_CONFIRMATION", label: "Chờ xác nhận" },
    { key: "CONFIRMED", label: "Đã xác nhận" },
    { key: "PREPARING", label: "Đang đóng gói" },
    { key: "WAITING_FOR_PICKUP", label: "Chờ lấy hàng" },
    { key: "SHIPPING", label: "Đang giao" },
    { key: "DELIVERED", label: "Đã giao" },
    { key: "COMPLETED", label: "Hoàn tất" },
    { key: "CANCELLED", label: "Đã hủy" },
    { key: "CANCELLATIONS", label: "Yêu cầu hủy", badgeVariant: "amber" },
    { key: "RETURNS", label: "Yêu cầu trả hàng", badgeVariant: "red" },
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-x-auto scrollbar-thin">
      <div
        role="tablist"
        aria-label="Bộ lọc danh sách đơn hàng theo trạng thái"
        className="flex items-center min-w-max px-3 border-b border-slate-100"
      >
        {tabs.map((t) => {
          const isActive = activeTab === t.key;
          const count = counts[t.key];
          return (
            <button
              key={t.key}
              id={`tab-${t.key}`}
              role="tab"
              type="button"
              aria-selected={isActive}
              aria-controls="seller-orders-tab-content"
              onClick={() => onTabChange(t.key)}
              className={`py-3.5 px-3.5 relative text-xs font-bold transition-all flex items-center gap-2 shrink-0 min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset ${
                isActive
                  ? "text-primary font-extrabold"
                  : "text-on-surface-variant hover:text-on-surface hover:bg-primary/5"
              }`}
            >
              <span>{t.label}</span>
              {count !== undefined && count > 0 && (
                <span
                  className={`text-[10px] px-1.5 py-0.5 rounded-full font-extrabold leading-none ${
                    isActive
                      ? "bg-primary text-white"
                      : t.badgeVariant === "red"
                      ? "bg-red-100 text-red-900 border border-red-300"
                      : t.badgeVariant === "amber"
                      ? "bg-amber-100 text-amber-900 border border-amber-300"
                      : "bg-slate-100 text-slate-700"
                  }`}
                >
                  {count}
                </span>
              )}
              {isActive && (
                <span
                  aria-hidden="true"
                  className="absolute bottom-0 left-2 right-2 h-0.5 bg-primary rounded-full"
                />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
