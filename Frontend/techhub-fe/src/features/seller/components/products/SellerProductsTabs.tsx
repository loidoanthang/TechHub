"use client";

import React from "react";
import type { SellerProductTab } from "@/types/sellerProduct";

interface SellerProductsTabsProps {
  activeTab: SellerProductTab;
  onTabChange: (tab: SellerProductTab) => void;
  counts?: Partial<Record<SellerProductTab, number>>;
}

interface TabConfig {
  id: SellerProductTab;
  label: string;
  badgeVariant?: "red" | "amber" | "default";
}

const TABS: TabConfig[] = [
  { id: "ALL", label: "Tất cả" },
  { id: "PUBLISHED", label: "Đang mở bán" },
  { id: "PENDING_APPROVAL", label: "Chờ duyệt", badgeVariant: "amber" },
  { id: "DRAFT_REJECTED", label: "Bản nháp & Từ chối" },
  { id: "UNPUBLISHED", label: "Tạm ẩn" },
  { id: "SUSPENDED", label: "Bị khóa vi phạm", badgeVariant: "red" },
];

export function SellerProductsTabs({
  activeTab,
  onTabChange,
  counts = {},
}: SellerProductsTabsProps) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-x-auto scrollbar-thin">
      <div
        role="tablist"
        aria-label="Bộ lọc danh sách sản phẩm theo trạng thái"
        className="flex items-center min-w-max px-3 border-b border-slate-100"
      >
        {TABS.map((tab) => {
          const isActive = activeTab === tab.id;
          const count = counts[tab.id];

          return (
            <button
              key={tab.id}
              role="tab"
              type="button"
              aria-selected={isActive}
              onClick={() => onTabChange(tab.id)}
              className={`py-3.5 px-3.5 relative text-xs font-bold transition-all flex items-center gap-2 shrink-0 min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset ${
                isActive
                  ? "text-primary font-extrabold"
                  : "text-on-surface-variant hover:text-on-surface hover:bg-primary/5"
              }`}
            >
              <span>{tab.label}</span>
              {count !== undefined && count > 0 && (
                <span
                  className={`text-[10px] px-1.5 py-0.5 rounded-full font-extrabold leading-none ${
                    isActive
                      ? "bg-primary text-white"
                      : tab.badgeVariant === "red"
                      ? "bg-red-100 text-red-900 border border-red-300"
                      : tab.badgeVariant === "amber"
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
