"use client";

import React, { useState, useRef, useEffect } from "react";
import { Calendar, RefreshCw, X } from "lucide-react";

export type DashboardPeriodType = "TODAY" | "LAST_7_DAYS" | "THIS_MONTH" | "CUSTOM";

export interface DashboardPeriodFilterBarProps {
  period: DashboardPeriodType;
  onPeriodChange: (p: DashboardPeriodType) => void;
  startDate?: string;
  endDate?: string;
  onDateRangeChange: (start: string, end: string) => void;
  onRefresh?: () => void;
  isFetching?: boolean;
  lastUpdated?: string;
  className?: string;
}

const PERIOD_OPTIONS: { id: DashboardPeriodType; label: string }[] = [
  { id: "TODAY", label: "Hôm nay" },
  { id: "LAST_7_DAYS", label: "7 ngày qua" },
  { id: "THIS_MONTH", label: "Tháng này" },
  { id: "CUSTOM", label: "Tùy chọn" },
];

export function DashboardPeriodFilterBar({
  period,
  onPeriodChange,
  startDate = "",
  endDate = "",
  onDateRangeChange,
  onRefresh,
  isFetching = false,
  lastUpdated,
  className = "",
}: DashboardPeriodFilterBarProps) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [tempStart, setTempStart] = useState(startDate);
  const [tempEnd, setTempEnd] = useState(endDate);

  const containerRef = useRef<HTMLDivElement>(null);
  const todayStr = new Date().toISOString().split("T")[0];

  const handleSelectPeriod = (p: DashboardPeriodType) => {
    if (p === "CUSTOM") {
      setTempStart(startDate || todayStr);
      setTempEnd(endDate || todayStr);
      setIsDropdownOpen((prev) => !prev);
    } else {
      setIsDropdownOpen(false);
      onPeriodChange(p);
    }
  };

  const handleApplyCustom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!tempStart || !tempEnd) return;
    onDateRangeChange(tempStart, tempEnd);
    onPeriodChange("CUSTOM");
    setIsDropdownOpen(false);
  };

  // Close when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    }
    if (isDropdownOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isDropdownOpen]);

  return (
    <div
      ref={containerRef}
      className={`relative flex items-center justify-between sm:justify-start gap-2 max-w-full ${className}`}
    >
      {/* Pills Container with horizontal scroll on mobile */}
      <div className="bg-white border border-slate-200 rounded-xl p-1 flex items-center gap-1 overflow-x-auto no-scrollbar shadow-2xs max-w-full shrink-0">
        {PERIOD_OPTIONS.map((opt) => {
          const isSelected = period === opt.id;
          return (
            <button
              key={opt.id}
              type="button"
              onClick={() => handleSelectPeriod(opt.id)}
              className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-bold transition-colors duration-150 whitespace-nowrap cursor-pointer flex items-center gap-1.5 shrink-0 ${
                isSelected
                  ? "bg-[#0045cc] text-white shadow-2xs"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {opt.id === "CUSTOM" && isSelected && startDate && endDate ? (
                <>
                  <Calendar className="w-3.5 h-3.5" />
                  <span>
                    {startDate} → {endDate}
                  </span>
                </>
              ) : (
                <span>{opt.label}</span>
              )}
            </button>
          );
        })}
      </div>

      {/* Refresh Button & Timestamp */}
      {onRefresh && (
        <div className="flex items-center gap-2 text-xs text-slate-500">
          {lastUpdated && (
            <span className="text-[11px] hidden sm:inline">
              Cập nhật lúc: <strong className="text-slate-800">{lastUpdated}</strong>
            </span>
          )}
          <button
            type="button"
            onClick={onRefresh}
            disabled={isFetching}
            aria-label="Làm mới số liệu"
            title="Làm mới dữ liệu"
            className="h-9 px-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shadow-2xs transition-colors"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${
                isFetching ? "animate-spin text-[#0045cc] motion-reduce:animate-none" : ""
              }`}
            />
            <span className="hidden sm:inline">Làm mới</span>
          </button>
        </div>
      )}

      {/* Dropdown Popover */}
      {isDropdownOpen && (
        <div className="absolute right-0 top-full mt-2 z-50 w-80 sm:w-96 bg-white rounded-2xl p-4 shadow-xl border border-slate-200/90 animate-in fade-in zoom-in-95 duration-150">
          <div className="flex items-center justify-between pb-2.5 border-b border-slate-100 mb-3">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-[#0045cc]" />
              <span className="text-xs font-bold text-[#0b1c30]">
                Chọn khoảng thời gian
              </span>
            </div>
            <button
              type="button"
              onClick={() => setIsDropdownOpen(false)}
              className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
              aria-label="Đóng"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          <form onSubmit={handleApplyCustom} className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-2">
              <div className="flex flex-col gap-1">
                <label
                  htmlFor="period-popover-start"
                  className="text-[11px] font-semibold text-slate-600"
                >
                  Từ ngày
                </label>
                <input
                  id="period-popover-start"
                  type="date"
                  required
                  max={tempEnd || todayStr}
                  value={tempStart}
                  onChange={(e) => setTempStart(e.target.value)}
                  className="h-8.5 px-2.5 rounded-lg border border-slate-200 bg-white text-xs font-medium text-slate-800 focus:outline-none focus:border-[#0045cc]"
                />
              </div>

              <div className="flex flex-col gap-1">
                <label
                  htmlFor="period-popover-end"
                  className="text-[11px] font-semibold text-slate-600"
                >
                  Đến ngày
                </label>
                <input
                  id="period-popover-end"
                  type="date"
                  required
                  min={tempStart}
                  max={todayStr}
                  value={tempEnd}
                  onChange={(e) => setTempEnd(e.target.value)}
                  className="h-8.5 px-2.5 rounded-lg border border-slate-200 bg-white text-xs font-medium text-slate-800 focus:outline-none focus:border-[#0045cc]"
                />
              </div>
            </div>

            {/* Quick Presets */}
            <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
              <button
                type="button"
                onClick={() => {
                  const d = new Date();
                  d.setDate(d.getDate() - 14);
                  setTempStart(d.toISOString().split("T")[0]);
                  setTempEnd(todayStr);
                }}
                className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold transition-colors"
              >
                14 ngày qua
              </button>
              <button
                type="button"
                onClick={() => {
                  const d = new Date();
                  d.setDate(d.getDate() - 30);
                  setTempStart(d.toISOString().split("T")[0]);
                  setTempEnd(todayStr);
                }}
                className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold transition-colors"
              >
                30 ngày qua
              </button>
              <button
                type="button"
                onClick={() => {
                  const d = new Date();
                  d.setDate(d.getDate() - 90);
                  setTempStart(d.toISOString().split("T")[0]);
                  setTempEnd(todayStr);
                }}
                className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold transition-colors"
              >
                3 tháng qua
              </button>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100 mt-1">
              <button
                type="button"
                onClick={() => setIsDropdownOpen(false)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-[11px] font-semibold text-slate-600 transition-colors"
              >
                Hủy
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-[#0045cc] hover:bg-[#0038a8] text-white text-[11px] font-bold transition-all shadow-xs cursor-pointer"
              >
                Áp dụng
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
