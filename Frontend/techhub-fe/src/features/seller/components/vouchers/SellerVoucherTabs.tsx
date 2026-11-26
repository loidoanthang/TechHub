"use client";

import React from "react";
import { Search, Filter, Percent, Truck } from "lucide-react";
import { ScrollableTabsWrapper } from "@/components/shared/ScrollableTabsWrapper";
import { SelectDropdown } from "@/components/shared/SelectDropdown";
import type {
  VoucherStatus,
  DiscountType,
  VoucherAppliesTo,
} from "@/types/sellerVoucher";

interface SellerVoucherTabsProps {
  currentStatus?: VoucherStatus;
  onStatusChange: (status?: VoucherStatus) => void;
  keyword: string;
  onKeywordChange: (kw: string) => void;
  discountType?: DiscountType;
  onDiscountTypeChange: (dt?: DiscountType) => void;
  appliesTo?: VoucherAppliesTo;
  onAppliesToChange: (at?: VoucherAppliesTo) => void;
  statusCounts: {
    all: number;
    ongoing: number;
    upcoming: number;
    expired: number;
    disabled: number;
  };
}

export function SellerVoucherTabs({
  currentStatus,
  onStatusChange,
  keyword,
  onKeywordChange,
  discountType,
  onDiscountTypeChange,
  appliesTo,
  onAppliesToChange,
  statusCounts,
}: SellerVoucherTabsProps) {
  const tabs: { label: string; status?: VoucherStatus; count: number }[] = [
    { label: "Tất cả", status: undefined, count: statusCounts.all },
    { label: "Đang diễn ra", status: "ONGOING", count: statusCounts.ongoing },
    { label: "Sắp diễn ra", status: "UPCOMING", count: statusCounts.upcoming },
    { label: "Đã kết thúc", status: "EXPIRED", count: statusCounts.expired },
    { label: "Tạm dừng", status: "DISABLED", count: statusCounts.disabled },
  ];

  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl shadow-2xs overflow-hidden flex flex-col">
      {/* 1. Horizontal Status Tabs */}
      <ScrollableTabsWrapper>
        <div className="flex items-center overflow-x-auto no-scrollbar border-b border-slate-100 px-3 pt-1">
          {tabs.map((tab) => {
            const isActive = currentStatus === tab.status;
            return (
              <button
                key={tab.label}
                type="button"
                onClick={() => onStatusChange(tab.status)}
                className={`flex items-center gap-2 px-3.5 py-3 text-xs font-semibold whitespace-nowrap border-b-2 transition-all cursor-pointer ${
                  isActive
                    ? "border-[#0045cc] text-[#0045cc]"
                    : "border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300"
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                    isActive
                      ? "bg-blue-50 text-[#0045cc]"
                      : "bg-slate-100 text-slate-500"
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>
      </ScrollableTabsWrapper>

      {/* 2. Filter & Search Controls Bar */}
      <div className="p-3.5 flex flex-col md:flex-row items-center justify-between gap-3 bg-slate-50/40">
        {/* Search Input */}
        <div className="relative w-full md:w-80">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="Tìm theo mã hoặc tên voucher..."
            className="w-full h-9 pl-9 pr-3 rounded-xl border border-slate-200 bg-white text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-colors"
          />
        </div>

        {/* Dropdowns */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full md:w-auto">
          {/* Discount Type Dropdown */}
          <SelectDropdown
            value={discountType || ""}
            onChange={(val) =>
              onDiscountTypeChange(val ? (val as DiscountType) : undefined)
            }
            ariaLabel="Lọc theo hình thức giảm giá"
            className="w-full sm:w-44"
            buttonClassName="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-colors"
            options={[
              { value: "", label: "Tất cả hình thức" },
              { value: "PERCENTAGE", label: "Giảm theo %" },
              { value: "FIXED_AMOUNT", label: "Giảm tiền mặt (VNĐ)" },
            ]}
          />

          {/* Applies To Dropdown */}
          <SelectDropdown
            value={appliesTo || ""}
            onChange={(val) =>
              onAppliesToChange(val ? (val as VoucherAppliesTo) : undefined)
            }
            ariaLabel="Lọc theo phạm vi áp dụng"
            className="w-full sm:w-44"
            align="right"
            buttonClassName="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-colors"
            options={[
              { value: "", label: "Tất cả phạm vi" },
              { value: "PRODUCT", label: "Toàn bộ sản phẩm" },
              { value: "SHIPPING_FEE", label: "Phí vận chuyển" },
            ]}
          />
        </div>
      </div>
    </div>
  );
}
