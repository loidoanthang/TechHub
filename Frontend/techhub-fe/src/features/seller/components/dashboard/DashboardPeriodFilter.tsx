"use client";

import React from "react";
import { Calendar, RefreshCw } from "lucide-react";
import type { DashboardPeriod } from "@/types/sellerDashboard";

interface DashboardPeriodFilterProps {
  period: DashboardPeriod;
  onPeriodChange: (p: DashboardPeriod) => void;
  startDate: string;
  endDate: string;
  onStartDateChange: (d: string) => void;
  onEndDateChange: (d: string) => void;
  onRefresh: () => void;
  isFetching?: boolean;
  lastUpdated?: string;
}

export function DashboardPeriodFilter({
  period,
  onPeriodChange,
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  onRefresh,
  isFetching,
  lastUpdated,
}: DashboardPeriodFilterProps) {
  const periods: { id: DashboardPeriod; label: string }[] = [
    { id: "TODAY", label: "Hôm nay" },
    { id: "LAST_7_DAYS", label: "7 ngày qua" },
    { id: "THIS_MONTH", label: "Tháng này" },
    { id: "CUSTOM", label: "Tùy chỉnh" },
  ];

  const todayStr = new Date().toISOString().split("T")[0];

  return (
    <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#e2e8f0] shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      {/* Left: Quick period tabs */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1.5 p-1 bg-[#f1f5f9] rounded-xl border border-[#e2e8f0]">
          {periods.map((p) => {
            const isActive = period === p.id;
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => onPeriodChange(p.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  isActive
                    ? "bg-[#0045cc] text-white shadow-xs"
                    : "text-[#434656] hover:text-[#0b1c30] hover:bg-white/60"
                }`}
              >
                {p.label}
              </button>
            );
          })}
        </div>

        {/* Date pickers when CUSTOM is selected */}
        {period === "CUSTOM" && (
          <div className="flex items-center gap-2 animate-in fade-in slide-in-from-left-2">
            <div className="flex items-center gap-1.5 bg-[#f8f9fc] border border-[#e2e8f0] rounded-xl px-2.5 py-1.5">
              <Calendar className="w-3.5 h-3.5 text-[#737688]" />
              <input
                type="date"
                value={startDate}
                max={endDate || todayStr}
                onChange={(e) => onStartDateChange(e.target.value)}
                className="bg-transparent text-xs font-semibold text-[#0b1c30] focus:outline-none"
              />
            </div>
            <span className="text-xs text-[#737688] font-bold">đến</span>
            <div className="flex items-center gap-1.5 bg-[#f8f9fc] border border-[#e2e8f0] rounded-xl px-2.5 py-1.5">
              <Calendar className="w-3.5 h-3.5 text-[#737688]" />
              <input
                type="date"
                value={endDate}
                min={startDate}
                max={todayStr}
                onChange={(e) => onEndDateChange(e.target.value)}
                className="bg-transparent text-xs font-semibold text-[#0b1c30] focus:outline-none"
              />
            </div>
          </div>
        )}
      </div>

      {/* Right: Last updated timestamp & Refresh button */}
      <div className="flex items-center justify-between md:justify-end gap-3 text-xs text-[#737688]">
        {lastUpdated && (
          <span className="text-[11px]">
            Cập nhật lúc: <strong className="text-[#0b1c30]">{lastUpdated}</strong>
          </span>
        )}
        <button
          type="button"
          onClick={onRefresh}
          disabled={isFetching}
          title="Làm mới dữ liệu"
          className="p-1.5 rounded-xl border border-[#e2e8f0] text-[#434656] hover:text-[#0045cc] hover:bg-[#eff4ff] transition-colors disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isFetching ? "animate-spin text-[#0045cc]" : ""}`} />
        </button>
      </div>
    </div>
  );
}
