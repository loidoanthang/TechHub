"use client";

import React from "react";
import { Calendar } from "lucide-react";
import type {
  SellerWalletTransactionType,
  DateFilterPreset,
} from "../../types/wallet";

export type { DateFilterPreset };

interface TransactionFilterTabsProps {
  selectedType?: SellerWalletTransactionType;
  onSelectType: (type?: SellerWalletTransactionType) => void;
  dateFilter: DateFilterPreset;
  onSelectDateFilter: (preset: DateFilterPreset) => void;
  customStartDate?: string;
  customEndDate?: string;
  onChangeCustomDates?: (start?: string, end?: string) => void;
}

const TYPE_TABS: { label: string; value?: SellerWalletTransactionType }[] = [
  { label: "Tất cả biến động", value: undefined },
  { label: "Quyết toán đơn (+)", value: "ORDER_SETTLEMENT" },
  { label: "Lệnh rút tiền (-)", value: "WITHDRAWAL" },
  { label: "Hoàn tiền rút (+)", value: "WITHDRAWAL_REFUND" },
];

const DATE_PRESETS: { label: string; value: DateFilterPreset }[] = [
  { label: "Tất cả", value: "ALL" },
  { label: "Hôm nay", value: "TODAY" },
  { label: "7 ngày qua", value: "7DAYS" },
  { label: "30 ngày qua", value: "30DAYS" },
  { label: "Tùy chỉnh", value: "CUSTOM" },
];

export function TransactionFilterTabs({
  selectedType,
  onSelectType,
  dateFilter,
  onSelectDateFilter,
  customStartDate,
  customEndDate,
  onChangeCustomDates,
}: TransactionFilterTabsProps) {
  return (
    <div className="space-y-3 pb-2 border-b border-slate-100">
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        {/* Type Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full lg:w-auto pb-1 lg:pb-0 scrollbar-none">
          {TYPE_TABS.map((tab) => {
            const isActive = selectedType === tab.value;
            return (
              <button
                key={tab.label}
                type="button"
                onClick={() => onSelectType(tab.value)}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all shrink-0 cursor-pointer ${
                  isActive
                    ? "bg-[#0045cc] text-white shadow-xs"
                    : "bg-slate-100/70 hover:bg-slate-200/60 text-slate-600"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Date Filter Presets */}
        <div className="flex items-center gap-2 self-stretch lg:self-auto justify-end">
          <div className="flex items-center gap-1 text-xs text-slate-500 mr-1">
            <Calendar className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Thời gian:</span>
          </div>
          <div className="flex items-center gap-1 bg-slate-100/80 p-1 rounded-xl">
            {DATE_PRESETS.map((p) => {
              const isSelected = dateFilter === p.value;
              return (
                <button
                  key={p.value}
                  type="button"
                  onClick={() => onSelectDateFilter(p.value)}
                  className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-colors cursor-pointer ${
                    isSelected
                      ? "bg-white text-slate-900 shadow-2xs font-bold"
                      : "text-slate-500 hover:text-slate-900"
                  }`}
                >
                  {p.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Custom Date Range Picker Inputs */}
      {dateFilter === "CUSTOM" && onChangeCustomDates && (
        <div className="flex flex-wrap items-center gap-2.5 pt-2 pl-1 animate-in fade-in duration-200 text-xs">
          <span className="text-slate-500 font-medium">Từ ngày:</span>
          <input
            type="date"
            value={customStartDate || ""}
            max={customEndDate}
            onChange={(e) =>
              onChangeCustomDates(e.target.value || undefined, customEndDate)
            }
            className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-800 text-xs font-mono focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc]"
          />
          <span className="text-slate-500 font-medium">đến:</span>
          <input
            type="date"
            value={customEndDate || ""}
            min={customStartDate}
            onChange={(e) =>
              onChangeCustomDates(customStartDate, e.target.value || undefined)
            }
            className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-800 text-xs font-mono focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc]"
          />
        </div>
      )}
    </div>
  );
}
